package ch.so.agi.solr.indexupdater;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.QueueOfJobs;

@Component
public class QueuePoller {

    private static final Logger log = LoggerFactory.getLogger(QueuePoller.class);
    private static final int MAX_SILENCE_SECONDS = 3;
    
    private LocalDateTime lastAliveEmit = LocalDateTime.now();
    
    
    @Scheduled(fixedDelay = 1500)
    public void runOnePoll() {
    	
    	Job newJob = QueueOfJobs.remove();
    	
    	if(newJob == null) {
    		if(needToEmitLiveSign())
    			logInfo("QueueOfJobs gave me nothing to work on, going back to sleep");
    		
    		return;
    	}

    	indexJob(newJob);    	
    }
    
    private boolean needToEmitLiveSign() {
    	return lastAliveEmit.plusSeconds(MAX_SILENCE_SECONDS).isBefore(LocalDateTime.now());
    }
    
    private void logInfo(String msg) {
    	log.info(msg);
    	lastAliveEmit = LocalDateTime.now();
    }
    
    private void indexJob(Job newJob) {
    	logInfo(MessageFormat.format("Starting indexing for {0}. Queue state: {1}", newJob, QueueOfJobs.asString()));
    	
    	try {
    		Thread.sleep(2000);
    	}
    	catch(InterruptedException ie) {}
    }
    
    
}