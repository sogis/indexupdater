package ch.so.agi.solr.indexupdater;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.QueueOfJobs;
import ch.so.agi.solr.indexupdater.util.IndexSliceUpdater;

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
    			logInfo("Job queue ist empty, going back to sleep");
    		
    		return;
    	}

    	logInfo(MessageFormat.format("{0}: Starting indexing. Queue state: {1}", newJob, QueueOfJobs.asString()));
    	
    	complementDefaults(newJob);
    	
    	IndexSliceUpdater updater = new IndexSliceUpdater(newJob);
    	updater.execute();
    }
    
    private static void complementDefaults(Job job) {
    	if(job.getDihPath() == null)
    		job.setDihPath("solr/gdi/dih");
    	
    	if(job.getMaxWorkDurationMinutes() == null)
    		job.setMaxWorkDurationMinutes(30);
    	
    	if(job.getPollIntervalSeconds() == null)
    		job.setPollIntervalSeconds(2);
    }
    
    private boolean needToEmitLiveSign() {
    	return lastAliveEmit.plusSeconds(MAX_SILENCE_SECONDS).isBefore(LocalDateTime.now());
    }
    
    private void logInfo(String msg) {
    	log.info(msg);
    	lastAliveEmit = LocalDateTime.now();
    }  
}