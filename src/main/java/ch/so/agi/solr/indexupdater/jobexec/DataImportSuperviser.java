package ch.so.agi.solr.indexupdater.jobexec;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.JobState;
import ch.so.agi.solr.indexupdater.util.Util;

/*
 * Starts, monitors and ends (timeout) the data import
 * through the Solr DataImportHandler
 */
public class DataImportSuperviser {
	
	private static final Logger log = LoggerFactory.getLogger(DataImportSuperviser.class);
	
	private Job job;
	private RestTemplate client;
	
	private DihResponse lastResponse;
	private boolean dihTimedOut = false;

	public DataImportSuperviser(Job runningJob) {
		this.job = runningJob; 
		this.client = createConfiguredRestTemplate();
	}
	
	/*
	 * Returns when the job is done, or when the time limit
	 * is exceeded.
	 */
	public void execute() {
		
		startImport();
		
		LocalDateTime loopEndTime = LocalDateTime.now().plusSeconds(job.getMaxWorkDurationSeconds());
		
		boolean isWorkingClean = true;
		while(isWorkingClean) {
			Util.sleep(job.getPollIntervalSeconds() * 1000);
			
			if(LocalDateTime.now().isAfter(loopEndTime))
				this.dihTimedOut = true;
			
			queryDihState();
			
			isWorkingClean = dihWorkingClean();
		}	
		
		if(dihTimedOut)
			tryAbortImport();
	}
	
	private void startImport() {
		
		String[] qParams = new String[] {
				"command", "full-import",
				"entity", job.getDsIdentAsEntityName(),
				"clean", "false"
		};
		
		URI url = Util.buildSolrUrl(job.getDihPath(), qParams);
		
		HttpRequest req = HttpRequest.newBuilder(url).build();
		HttpResponse<String> res = Util.sendBare(req, job.getJobIdentifier());
		
		Util.assert200(res, url, job.getJobIdentifier());
		
		log.info("{}: Sent insert request with url: {}.", 
				job.getJobIdentifier(),
				url);
	}
	
	private boolean dihWorkingClean() {
		
		boolean res = true;
		
		if (dihTimedOut) {
			res = false;
		}
		else if(lastResponse.isDihIdle()) {
			res = false;
		}
		else if(lastResponse.getDocs_skipped() > 0) {
			res = false;
		}
	
		return res;
	}
	
	private void queryDihState() {
		
		this.lastResponse = queryJobState();

		if (!lastResponse.isDihIdle()) {
			log.info("{}: Indexing... Processed {} documents.", job.getJobIdentifier(), lastResponse.getDocs_processed());
		}
	}
	
	private void tryAbortImport() {				
		DihResponse dihResponse = null;
		
		for(int i=0; i<3; i++) {
			sendImportAbort();			
			Util.sleep(100);
			dihResponse = queryJobState();
			
			if(DihResponse.STATUS_IDLE.equals(dihResponse.getStatus()))
				break;
			
			log.info("{}: Tried {} time(s) to abort job - not successful yet", job.getJobIdentifier(), i+1);
			
			Util.sleep(3000);
		}
		
		if( !(DihResponse.STATUS_IDLE.equals(dihResponse.getStatus())) ) {
			
			String exclamation = "******************************************************************************************";			
			String msg = MessageFormat.format(
					"{0}: Failed to abort running dih job for dataset {1}",
					job.getJobIdentifier(),
					job.getDataSetIdentifier()
					);
			
			log.error(exclamation);
			log.error(msg);
			log.error(exclamation);
			
			throw new DihJobHangingException(msg);
		}
	}
	

	private DihResponse sendImportAbort() {
		
		URI url = Util.buildSolrUrl(
				job.getDihPath(), 
				new String[] {"command", "abort"});
		
		DihResponse res = client.getForObject(url, DihResponse.class);
		
		if(res == null) {
			String msg = MessageFormat.format(
					"{0}: Sending abort returned null response. Url: {1}", 
					job.getJobIdentifier(),
					url
					);
			
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		return res;
	}
	
	private DihResponse queryJobState() {
		
		URI url = Util.buildSolrUrl(
				job.getDihPath(), 
				new String[] {"command", "status"});
		
		DihResponse res = client.getForObject(url, DihResponse.class);
		
		if(res == null) {
			String msg = MessageFormat.format(
					"{0}: Query of import status returned null. Url: {1}", 
					job.getJobIdentifier(),
					url
					);
			
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		return res;
	}
	
	public JobState determineEndState() {
		
		JobState res = JobState.ENDED_OK;
		
		int skipped = lastResponse.getDocs_skipped();
		int processed = lastResponse.getDocs_processed();
		
		if(!lastResponse.isDihIdle())
			throw new IllegalStateException("Can not determine job end state as dih is still busy");
		
		if(dihTimedOut || skipped > 0 || processed < 1)
			res = JobState.ENDED_EXCEPTION;
		
		logEndState(res);
		
		return res;
	}
	
	private void logEndState(JobState res) {
		
		if(res == JobState.ENDED_OK){		
			log.info("{}: Ended succesfully: Timeout? {}, Docs processed: {}, Docs skipped: {}", 
					job.getJobIdentifier(),
					dihTimedOut,
					lastResponse.getDocs_processed(),
					lastResponse.getDocs_skipped()
					);
		}
		else {
			log.error("{}: Ended with exception: Timeout? {}, Docs processed: {}, Docs skipped: {}", 
					job.getJobIdentifier(),
					dihTimedOut,
					lastResponse.getDocs_processed(),
					lastResponse.getDocs_skipped()
					);
		}		
	}
	
	/*
	 * Solr returns responses with text/plain;charset=utf-8, ...
	 * This is unprecise, and must be registered with the RestTemplate.
	 */
	private static RestTemplate createConfiguredRestTemplate() {
		
		RestTemplate templ = new RestTemplate();
		
		List<MediaType> supportedTypeList = new ArrayList<MediaType>();
		supportedTypeList.add(MediaType.APPLICATION_JSON);
		supportedTypeList.add(MediaType.TEXT_PLAIN);
		
		List<HttpMessageConverter<?>> list = templ.getMessageConverters();
		
		int foundCount = 0;
		for (HttpMessageConverter<?> converter : list) {
			
			if(converter instanceof MappingJackson2HttpMessageConverter) {
				foundCount++;
				
				MappingJackson2HttpMessageConverter jConf = (MappingJackson2HttpMessageConverter)converter;
				jConf.setSupportedMediaTypes(supportedTypeList);
			}
		}
		
		if(foundCount == 0) {
			throw new RuntimeException(
					"Did not find MappingJackson2HttpMessageConverter in RestTemplate to configure additional response content types");
		}
		
		return templ;
	}
}
