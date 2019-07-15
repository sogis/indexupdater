package ch.so.agi.solr.indexupdater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.QueueOfJobs;
import ch.so.agi.solr.indexupdater.util.IndexSliceUpdater;
import ch.so.agi.solr.indexupdater.util.Settings;

@Component
public class QueuePoller {

    private static final Logger log = LoggerFactory.getLogger(QueuePoller.class);
    
    private LocalDateTime lastAliveEmit = LocalDateTime.now();
    
    @Autowired
    Settings settings;
    
    
    @Scheduled(fixedDelay = 1500)
    public void runOnePoll() {
    	
    	Job newJob = QueueOfJobs.remove();
    	
    	if(newJob == null) {
    		if(needToEmitLiveSign())
    			logInfo("Job queue is currently empty, going back to sleep");
    		
    		return;
    	}

    	logInfo(MessageFormat.format("{0}: Starting indexing. Queue state: {1}", newJob, QueueOfJobs.asString()));
    	
    	complementDefaults(newJob);
    	
    	IndexSliceUpdater updater = new IndexSliceUpdater(newJob);
    	updater.execute();
    }
    
    private void complementDefaults(Job job) {
    	if(job.getDihPath() == null)
    		job.setDihPath(settings.getDihPath()); //"solr/gdi/dih"
    	
    	if(job.getMaxWorkDurationSeconds() == null)
    		job.setMaxWorkDurationSeconds(settings.getDihImportMaxDurationSeconds());
    	
    	if(job.getPollIntervalSeconds() == null)
    		job.setPollIntervalSeconds(settings.getDihPollIntervalSeconds());
    }
    
    private boolean needToEmitLiveSign() {
    	int maxSilence = settings.getLogSilenceMaxDurationSeconds();
    	return lastAliveEmit.plusSeconds(maxSilence).isBefore(LocalDateTime.now());
    }
    
    private void logInfo(String msg) {
    	log.info(msg);
    	lastAliveEmit = LocalDateTime.now();
    }  
}