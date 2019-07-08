package ch.so.agi.solr.indexupdater;


import org.springframework.http.HttpStatus;
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
    		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
    				"Parameter ds is missing. Specifying the dataset to queue is mandatory");
    	
    	
    	String jobId = IdentifierForCurrentTime.generate();
    	Job j = new Job(jobId, dataset, null);
    	QueueOfJobs.add(j);
    	
    	return jobId;
    }
}
