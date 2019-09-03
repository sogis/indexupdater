package ch.so.agi.solr.indexupdater.model;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/*
 * Queue containing the jobs waiting for indexing in solr.
 * 
 * By intent, this class provides no hasJobs() or similar method,
 * in order to be sure of thread safe usage of this class.
 */
public class QueueOfJobs {
	
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
			
			if(endedJobs.size() > 20)
				endedJobs.poll();
		}
			
		Job next = pendingJobs.poll();
		
		if(next != null)
			working_Placeholder = new Job(next.getJobIdentifier(), next.getDataSetIdentifier());
		else
			working_Placeholder = null;
		
		return next;
	}
	
	public static synchronized String asString() {
		
		String msg = null;
		if(working_Placeholder != null)
			msg = MessageFormat.format("Working on job {0}.", working_Placeholder.getJobIdentifier());
		else
			msg = "Not working on a job.";
		
		if(pendingJobs.size() > 0)
			msg += MessageFormat.format(" Pending: {0}", pendingJobs);
		
		if(endedJobs.size() > 0)
			msg += MessageFormat.format(" Latest ended Jobs: {0}", endedJobs);
		
		return msg;
	}
}
