package ch.so.agi.solr.indexupdater.model;

import java.text.MessageFormat;

public class Job {
	private String jobIdentifier;
	private String dataSetIdentifier;
	
	private Integer pollIntervalMillis;
	
	public Job(String jobIdentifier, String dataSetIdentifier, Integer pollIntervalMillis) {
		this.jobIdentifier = jobIdentifier;
		this.dataSetIdentifier = dataSetIdentifier;
		this.pollIntervalMillis = pollIntervalMillis;
	}
	
	public Integer getPollIntervalMillis() {
		return pollIntervalMillis;
	}

	public void setPollIntervalMillis(Integer pollIntervalMillis) {
		this.pollIntervalMillis = pollIntervalMillis;
	}

	public String getJobIdentifier() {
		return jobIdentifier;
	}

	public String getDataSetIdentifier() {
		return dataSetIdentifier;
	}

	@Override
	public String toString() {
		String repr = null;
		if(pollIntervalMillis == null)
			repr = MessageFormat.format("Job [id:{0}, ds:{1}]", jobIdentifier, dataSetIdentifier);
		else
			repr = MessageFormat.format("Job [id:{0}, ds:{1}, mil:{2}]", jobIdentifier, dataSetIdentifier, pollIntervalMillis);
		
		return repr;
	}
	
	

}
