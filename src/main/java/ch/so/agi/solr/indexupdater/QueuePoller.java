package ch.so.agi.solr.indexupdater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.JobEndState;
import ch.so.agi.solr.indexupdater.model.QueueOfJobs;
import ch.so.agi.solr.indexupdater.util.IndexSliceUpdater;
import ch.so.agi.solr.indexupdater.util.Settings;

@Component
public class QueuePoller {

    private static final Logger log = LoggerFactory.getLogger(QueuePoller.class);
    
    private LocalDateTime lastAliveEmit = LocalDateTime.now();
    
    private Job working = null;
    
    Settings settings = Settings.instance();
    
    
    @Scheduled(fixedDelay = 1500)
    public void runOnePoll() {
    	
    	Job newJob = QueueOfJobs.swapEndedForPending(working);
    	this.working = newJob;
    	
    	if(working == null) {
    		if(needToEmitLiveSign())
    			logInfo("Job queue is currently empty, going back to sleep");
    		
    		return;
    	}

    	logInfo(MessageFormat.format("{0}: Starting indexing. Queue state: {1}", working, QueueOfJobs.asString()));
    	
    	complementDefaults(working);
    	
    	try {
        	IndexSliceUpdater updater = new IndexSliceUpdater(working);
        	updater.execute();
        	
        	working.setEndState(JobEndState.OK);
    	}
    	catch(Exception e) {
    		working.setEndState(JobEndState.EXCEPTION);
    		throw e;
    	}
    }
    
    private void complementDefaults(Job job) {
    	
    	boolean setDihPath = false, setWorkDuration = false, setPollInterval = false;
 
    	if(job.getDihPath() == null) {
    		job.setDihPath(settings.getDihPath());
    		setDihPath = true;
    	}
    	
    	if(job.getMaxWorkDurationSeconds() == null) {
    		job.setMaxWorkDurationSeconds(settings.getDihImportMaxDurationSeconds());
    		setWorkDuration = true;
    	}
    	
    	if(job.getPollIntervalSeconds() == null) {
    		job.setPollIntervalSeconds(settings.getDihPollIntervalSeconds());
    		setPollInterval = true;
    	}
    	
    	if(setDihPath || setWorkDuration || setPollInterval) {
    		   		
    		ArrayList<String> parts = new ArrayList<>();
    		
    		if(setDihPath)
    			parts.add(MessageFormat.format("Dih path: {0}", job.getDihPath()));
    		
    		if(setWorkDuration)
    			parts.add(MessageFormat.format("Max. work duration [s]: {0}", job.getMaxWorkDurationSeconds()));
    		
    		if(setDihPath)
    			parts.add(MessageFormat.format("Poll interval [s]: {0}", job.getPollIntervalSeconds()));
    		
    		log.info("{}: Set default(s) for job: {}", job.getJobIdentifier(), parts);
    	}
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