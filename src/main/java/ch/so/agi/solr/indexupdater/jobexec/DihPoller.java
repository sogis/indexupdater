package ch.so.agi.solr.indexupdater.jobexec;

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
import ch.so.agi.solr.indexupdater.model.JobState;

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
		
		LocalDateTime loopEndTime = LocalDateTime.now().plusSeconds(job.getMaxWorkDurationSeconds());
		
		PollState state = PollState.DIH_WORKING_CLEAN;
		
		while(state == PollState.DIH_WORKING_CLEAN) {
			Util.sleep(job.getPollIntervalSeconds() * 1000);
			
			if(LocalDateTime.now().isAfter(loopEndTime)) {
				state = PollState.TIMEOUT_EXCEEDED;
				break;
			}
			
			state = queryDihState();
		}
		
		if(state == PollState.DIH_WORKING_SKIPPED_DOCS || state == PollState.TIMEOUT_EXCEEDED) {
			state = tryAbortJob(state);
		}
		
		assertDihIdle(state);		
	}
	
	private void assertDihIdle(PollState state) {
		if(state != PollState.DIH_IDLE) {
			throw new IllegalStateException(MessageFormat.format(
					"{0}: DIH must be in state {1} when ending poll for job", 
					job.getJobIdentifier(),
					state
					));
		}
	}
	
	private PollState queryDihState() {
		PollState res = null;
		
		DihResponse dihResponse = queryJobState();
		
		if(dihResponse.getDocs_skipped() > 0) {
			res = PollState.DIH_WORKING_SKIPPED_DOCS;
		}
		else if (DihResponse.STATUS_BUSY.equals(dihResponse.getStatus())) {
			log.info("{}: Indexing... Processed {} documents.", job.getJobIdentifier(), dihResponse.getDocs_processed());
			res = PollState.DIH_WORKING_CLEAN;
		}
		else if(DihResponse.STATUS_IDLE.equals(dihResponse.getStatus())) {
			res = PollState.DIH_IDLE;
		}
		else {
			throw new IllegalStateException(MessageFormat.format(
					"The returned DIH response is not handled in Indexupdater. DihResponse: {0}", 
					dihResponse
					));
		}		
		
		return res;
	}
	
	private PollState tryAbortJob(PollState state) {		
		PollState res = null;		
		DihResponse dihResponse = null;
		
		for(int i=0; i<3; i++) {
			sendJobAbort();			
			Util.sleep(100);
			dihResponse = queryJobState();
			
			if(DihResponse.STATUS_IDLE.equals(dihResponse.getStatus()))
				break;
			
			log.info("{}: Tried {} time(s) to abort job - not successful yet", job.getJobIdentifier(), i+1);
			
			Util.sleep(3000);
		}
		
		if( !(DihResponse.STATUS_IDLE.equals(dihResponse.getStatus())) ) {
			
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
		else {
			log.info("{}: Succesfully aborted job. DIH is currently idle", job.getJobIdentifier());
			job.setEndState(JobState.ENDED_ABORTED);
			res = PollState.DIH_IDLE;
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
	
	private DihResponse sendJobAbort() {
		
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

enum PollState {
	DIH_WORKING_CLEAN, DIH_WORKING_SKIPPED_DOCS, TIMEOUT_EXCEEDED, DIH_IDLE 
}
	
