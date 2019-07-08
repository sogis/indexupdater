package ch.so.agi.solr.indexupdater.model;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Queue;

/*
 * Queue containing the jobs waiting for indexing in solr.
 * 
 * By intent, this class provides no hasJobs() or similar method,
 * in order to be sure of thread safe usage of this class.
 */
public class QueueOfJobs {
	
	private static Queue<Job> jobs = new LinkedList<Job>();
	
	
	public static synchronized void add(Job job) {
		if(job == null)
			throw new RuntimeException("Method argument [job] must not be null");
			
		jobs.add(job);
	}
	
	/*
	 * Returns the longest waiting Job, of NULL if
	 * the queue is empty.
	 */
	public static synchronized Job remove() {
		return jobs.poll();
	}
	
	public static synchronized String asString() {
		return MessageFormat.format("[{0}]", jobs);
	}
}
