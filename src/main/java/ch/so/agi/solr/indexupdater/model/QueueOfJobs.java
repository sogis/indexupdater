package ch.so.agi.solr.indexupdater.model;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import ch.so.agi.solr.indexupdater.util.QueueStatus;

/*
 * Queue containing the jobs waiting for indexing in solr.
 * 
 * By intent, this class provides no hasJobs() or similar method,
 * in order to be sure of thread safe usage of this class.
 */
public class QueueOfJobs {
	
	private static final int MAX_NUM_ENDED_JOBS = 30;
	
	private static Queue<Job> pendingJobs = new LinkedList<Job>();
	private static Job working_Placeholder = null; // This is only a sibling with same identity-information. Due to thread safety, the real working job is only accessed from the thread of the background worker
	private static Queue<Job> endedJobs = new LinkedList<Job>();

	
	public static synchronized void addAll(Collection<Job> jobs) {
		
		for(Job job:jobs) {
			if(job == null)
				throw new RuntimeException("Method argument [job] must not be null");
			
			pendingJobs.add(job);
		}
	}
	
	public static synchronized String queryJobState(String jobIdent) {
		
		Job found = null;
		for(Job pending : pendingJobs) {
			if(jobIdent.equals(pending.getJobIdentifier())){
				found = pending;
				break;
			}
		}
		
		if(found != null)
			return JobState.PENDING.toString();
		
		if(working_Placeholder != null && jobIdent.equals(working_Placeholder.getJobIdentifier()))
			return JobState.WORKING.toString();
		
		for(Job ended : endedJobs) {
			if(jobIdent.equals(ended.getJobIdentifier())){
				found = ended;
				break;
			}
		}
		
		if(found != null) {
			return found.getEndState().toString();
		}
		else {
			return null;
		}
	}
	
	/*
	 * Returns the longest waiting Job, of NULL if
	 * the queue is empty.
	 */
	public static synchronized Job swapEndedForPending(Job ended) {
		
		if(ended != null) {
			endedJobs.add(ended);
			
			if(endedJobs.size() > MAX_NUM_ENDED_JOBS)
				endedJobs.poll();
		}
			
		Job next = pendingJobs.poll();
		
		if(next != null)
			working_Placeholder = new Job(next.getJobIdentifier(), next.getDataSetIdentifier());
		else
			working_Placeholder = null;
		
		return next;
	}
	
	public static synchronized QueueStatus status() {
		
		String detailMessage = "";
		int numPending = 0;
		String workingIdent = null;
		int numFinishedOK = 0;
		int numFinishedError = 0;
		
		if(working_Placeholder != null) {
			workingIdent = working_Placeholder.getJobIdentifier();
		}
		
		numPending = pendingJobs.size();
		if(numPending > 0)
			detailMessage += MessageFormat.format(" Pending: {0}", pendingJobs);
				
		if(endedJobs.size() > 0)
			detailMessage += MessageFormat.format(" Latest ended Jobs: {0}", endedJobs);
		
		for (Job job : endedJobs) {
			if(job.getEndState() == JobState.ENDED_OK)
				numFinishedOK++;
			else
				numFinishedError++;
		}
		
		if (detailMessage.length() == 0)
			detailMessage = null;
		
		return new QueueStatus(
				numPending,
				workingIdent,
				numFinishedOK,
				numFinishedError,
				detailMessage
				);
	}
}
