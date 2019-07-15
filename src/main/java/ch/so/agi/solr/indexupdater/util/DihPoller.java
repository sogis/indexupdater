package ch.so.agi.solr.indexupdater.util;

import java.net.URI;
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

/*
 * Polls solr for the state of the currently
 * running dih import process.
 */
public class DihPoller {
	
	private static final Logger log = LoggerFactory.getLogger(DihPoller.class);
	
	private Job job;
	private RestTemplate client;

	public DihPoller(Job runningJob) {
		this.job = runningJob; 
		this.client = createConfiguredRestTemplate();
	}
	
	/*
	 * Polls solr for the state of the currently running job.
	 * Returns when the job is done, or when the time limit
	 * is exceeded.
	 */
	public void execute() {
		
		boolean loop = true;
		LocalDateTime loopEndTime = LocalDateTime.now().plusSeconds(job.getMaxWorkDurationSeconds());
				
		while(loop) {
			Util.sleep(1000);
					
			DihResponse dihResponse = queryJobState();
			
			if(dihResponse.getDocs_skipped() > 0) {
				abortJob();
			}
			if (DihResponse.STATUS_BUSY.equals(dihResponse.getStatus())) {
				log.info("{}: Indexing... Processed {} documents.", job.getJobIdentifier(), dihResponse.getDocs_processed());
			}
			else if(DihResponse.STATUS_IDLE.equals(dihResponse.getStatus())) {
				loop = false;
			}
			else if(LocalDateTime.now().isAfter(loopEndTime)) {
				abortJob();
			}				
		}
	}
	
	private void abortJob() {
		
		DihResponse dihResponse = null;
		
		for(int i=0; i<3; i++) {
			sendJobAbort();			
			Util.sleep(100);
			dihResponse = queryJobState();
			
			if(dihResponse.getStatus() == dihResponse.STATUS_IDLE)
				break;
			
			log.info("{}: Tried {} time(s) to abort job - not successful yet", job.getJobIdentifier(), i);
			
			Util.sleep(3000);
		}
		
		if( !(dihResponse.getStatus() == dihResponse.STATUS_IDLE) ) {
			
			String express = "******************************************************************************************";			
			String msg = MessageFormat.format(
					"{0}: Failed to abort running dih job for dataset {1}",
					job.getJobIdentifier(),
					job.getDataSetIdentifier()
					);
			
			log.error(express);
			log.error(msg);
			log.error(express);
			
			throw new DihJobHangingException(msg);
		}
	}
	
	private DihResponse queryJobState() {
		
		URI url = Util.buildUrl(
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
	
	private DihResponse sendJobAbort() {
		
		URI url = Util.buildUrl(
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
