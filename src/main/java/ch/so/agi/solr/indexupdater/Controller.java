package ch.so.agi.solr.indexupdater;


import java.text.MessageFormat;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.QueueOfJobs;
import ch.so.agi.solr.indexupdater.util.IdentifierForCurrentTime;


@RestController
public class Controller {
	
	private ObjectMapper mapper = new ObjectMapper();
	

    @RequestMapping("/queue")
    public String addJobToQueue(
    		@RequestParam(value="ds") String[] datasets,
    		@RequestParam(value="dih", required=false) String dihPath,
    		@RequestParam(value="poll", required=false) Integer pollIntervalSeconds,
    		@RequestParam(value="timeout", required=false) Integer timeoutSeconds){    	
    	
    	ArrayList<Job> jobs = new ArrayList<>();
    	ArrayList<String> ids = new ArrayList<>();
    	
    	for (String ds : datasets) {
        	String jobId = IdentifierForCurrentTime.generate();
        	
        	Job j = new Job(
        			jobId, 
        			ds,
        			dihPath,
        			pollIntervalSeconds,
        			timeoutSeconds
        			);
        	
        	jobs.add(j);
        	ids.add(jobId);
    	}
    	
    	QueueOfJobs.addAll(jobs);
    	
    	
    	String jsonArray = null;
    	
    	try {
    		jsonArray = mapper.writeValueAsString(ids);
    	}
    	catch(JsonProcessingException je) {
    		throw new RuntimeException(je);
    	}    	
    	
    	return jsonArray;
    }  
    
    
    
    /*
     * Returns the state of all known jobs.
     * Known jobs can be in one of the following states:
     * [scheduled]: Job waits in queue to be processed
     * [working]: Job is currently executed
     * [finished]: Job finished working. With ending 
     * successful or with error. 
     */
    @RequestMapping("/status")
    public String returnAppStatus(){
    	return QueueOfJobs.asString();
    } 
    
    /*
     * Returns the state of the given job.
     * Known jobs can be in one of the following states:
     * [scheduled]: Job waits in queue to be processed
     * [working]: Job is currently executed
     * [finished]: Job finished working. With ending 
     * successful or with error. 
     */
    @RequestMapping("/status/{ident}")
    public String returnJobStatus(@PathVariable("ident") String ident){
    	String state = QueueOfJobs.queryJobState(ident);
    	
    	if(state == null) {
    		throw new ResponseStatusException(
    				HttpStatus.NOT_FOUND, 
    				MessageFormat.format("No job with id {0} found on server. Might have forgotten job as it has ended", ident)
    				);
    	}
    	
    	return state;
    } 
}
