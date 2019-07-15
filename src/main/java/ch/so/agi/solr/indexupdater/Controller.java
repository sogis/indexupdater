package ch.so.agi.solr.indexupdater;


import java.text.MessageFormat;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.QueueOfJobs;
import ch.so.agi.solr.indexupdater.util.IdentifierForCurrentTime;


@RestController
public class Controller {
	

    @RequestMapping("/queue")
    public String addJobToQueue(@RequestParam(value="ds") String dataset){
    	
    	if (dataset == null || dataset.length() == 0)
    		throw new ResponseStatusException(
    				HttpStatus.BAD_REQUEST, 
    				"Parameter ds is missing. Specifying the dataset to queue is mandatory"
    				);
    	
    	
    	String jobId = IdentifierForCurrentTime.generate();
    	Job j = new Job(jobId, dataset);
    	QueueOfJobs.add(j);
    	
    	return jobId;
    }  
    
    /*
     * Returns the state of all known jobs.
     * Known jobs can be in one of the following states:
     * [scheduled]: Job waits in queue to be processed
     * [working]: Job is currently executed
     * [finished]: Job finished working. With ending 
     * succesful or with error. 
     */
    @RequestMapping("/status")
    public String returnAppStatus(){
    	return QueueOfJobs.asString();
    } 
    
    /*
     * Returns the state of all known jobs.
     * Known jobs can be in one of the following states:
     * [scheduled]: Job waits in queue to be processed
     * [working]: Job is currently executed
     * [finished]: Job finished working. With ending 
     * succesful or with error. 
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
