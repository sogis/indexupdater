package ch.so.agi.solr.indexupdater;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.so.agi.solr.indexupdater.model.JobState;
import ch.so.agi.solr.indexupdater.util.BaseAddress;
import ch.so.agi.solr.indexupdater.util.Util;


public class IndexUpdaterApplicationTests {
	
	private static final Logger log = LoggerFactory.getLogger(IndexUpdaterApplicationTests.class);
	
	private static final BaseAddress ADDRESS_IDX_UPDATER = new BaseAddress("localhost", 8080);
	
	private static final String PATH_QUEUE = "queue";
	private static final String PATH_STATUS = "status";
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Test
	public void UpdateSlice_OK_WhenUsingAllParams() {
		String[] qPara = new String[] {
				"ds", "ch.so.agi.fill_0_1k",
				"dih", "solr/gdi/dih",
				"poll", "1",
				"timeout", "180"
				};
		
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent =  Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);
		
		pollForEndState(arr[0], JobState.ENDED_OK);
	}

	@Test
    public void UpdateSlice_WithNoPreviousDocs_OK() {
		String[] qPara = new String[] {"ds", "ch.so.agi.fill_2k_3k"};
				
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent =  Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);
		
		pollForEndState(arr[0], JobState.ENDED_OK);		
	}
	
	@Test
	public void UpdateSlice_WithPreviousDocs_OK() {
		
		String[] qPara = new String[] {"ds", "ch.so.agi.fill_0_1k,ch.so.agi.fill_0_1k"};
		
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent =  Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);
		
		pollForEndState(arr[1], JobState.ENDED_OK);
	}
	
	@Test
	public void UpdateSlice_CleanAbort_WhenExceedingMaxWorkDuration() {
	
		String[] qPara = new String[] {
				"ds", "ch.so.agi.fill_10k_60k",
				"poll", "1",
				"timeout", "2"
				};
		
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent =  Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);
		
		pollForEndState(arr[0], JobState.ENDED_EXCEPTION);
	}
	
	@Test
	public void UpdateSlice_Exception_WhenUsingWrongDatasetName() {
		String[] qPara = new String[] {"ds", "gugus"};
		
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent =  Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);
		
		pollForEndState(arr[0], JobState.ENDED_EXCEPTION);
	}
	
	@Test
	public void UpdateSlice_Exception_WhenHavingSkippedDocuments() {
		String[] qPara = new String[] {"ds", "ch.so.agi.fill_faulty"};
		
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent =  Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);
		
		pollForEndState(arr[0], JobState.ENDED_EXCEPTION);
	}
	
	@Test
	public void JobChain_SuccesAfterError_OK() throws JsonParseException, JsonMappingException, IOException {
		String[] qPara = new String[] {"ds", "gugus,ch.so.agi.fill_0_1k"};
		
		URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_QUEUE, qPara);
		HttpRequest req = HttpRequest.newBuilder(url).build();
		
		String jobIdent = Util.sendBare(req, null).body();
		String[] arr = jsonToArray(jobIdent);		
		
		pollForEndState(arr[1], JobState.ENDED_OK);
	}
	
	private String[] jsonToArray(String jsonString) {
		
		String[] res = null;
		
		try{
			res = mapper.readValue(jsonString, String[].class);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return res;
	}
	
	private static void pollForEndState(String jobIdent, JobState state) {
		pollForEndState(jobIdent, state, 80);
	}	
	
	private static void pollForEndState(String jobIdent, JobState state, int timeOutSecs) {
		LocalDateTime timeOutTime = LocalDateTime.now().plusSeconds(timeOutSecs);
		
		boolean endStateReached = false;
		String lastResponse = null;
		while(LocalDateTime.now().isBefore(timeOutTime) && !endStateReached) {
			Util.sleep(1000);
			
			URI url = Util.buildUrl(ADDRESS_IDX_UPDATER, PATH_STATUS, jobIdent);
			HttpRequest req = HttpRequest.newBuilder(url).build();
			
			lastResponse = Util.sendBare(req, jobIdent).body();	
			
			log.info("{}: Polling... Response is: {}. Request was: {}", jobIdent, lastResponse, req);
			
			endStateReached = isEndState(lastResponse);	
		}
		
		if(!endStateReached) {//timeout
			throw new RuntimeException(MessageFormat.format(
					"Timeout exceeded while polling job {0} for state {1}", 
					jobIdent,
					state)
					);
		}
		else if( !state.name().equals(lastResponse) ) {
			throw new RuntimeException(MessageFormat.format(
					"{0}: Ended in wrong state {1}. Expected: {2}",
					jobIdent,
					lastResponse,
					state
					));
		}
	}
	
	private static boolean isEndState(String endStateName) {
		JobState[] endStates = new JobState[] {
				JobState.ENDED_EXCEPTION,
				JobState.ENDED_OK
				};
		
		boolean isEndState = false;
		for( JobState endState : endStates) {
			if(endState.name().equals(endStateName)) {
				isEndState = true;
				break;
			}
		}
		return isEndState;
	}
}
