package ch.so.agi.solr.indexupdater.util;

import java.text.MessageFormat;

/*
 * DTO Containing the current Status information of the
 * Queues of pending, working and done jobs
 */
public class QueueStatus {
	private int numPending;
	private String workingIdent;
	private int numFinishedOK;
	private int numFinishedError;
	private String detailMessage;
	
	public QueueStatus(int numPending, String workingIdent, int numFinishedOK, int numFinishedError, String detailMessage) {
		this.numPending = numPending;
		this.workingIdent = workingIdent;
		this.numFinishedOK = numFinishedOK;
		this.numFinishedError = numFinishedError;
		this.numPending = numPending;
		this.detailMessage = detailMessage;
	}
	
	public int getNumFinishedError() {
		return numFinishedError;
	}
	
	@Override
	public String toString() {
		
		String res = "";
		
		if(workingIdent != null) {
			res += MessageFormat.format("Working on job {0}.<br/>", workingIdent);
		}
		else {
			res += "Not working on a job.<br/>";
		}
		
		res += "<br/>";
		
		res += MessageFormat.format("{0} jobs are pending (waiting).<br/>", numPending);
		res += MessageFormat.format("{0} jobs finished with error. {1} jobs finished succesfully.<br/>", numFinishedError, numFinishedOK);
		
		if(detailMessage != null) {
			res += "<br/>Details:<br/>";
			res += detailMessage;
		}
		
		return res;
	}
}
