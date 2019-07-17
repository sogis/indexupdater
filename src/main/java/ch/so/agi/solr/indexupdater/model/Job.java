package ch.so.agi.solr.indexupdater.model;

import java.text.MessageFormat;

public class Job {
	private String jobIdentifier;
	private String dataSetIdentifier;
	
	private Integer pollIntervalSeconds;
	private String dihPath;
	private Integer maxWorkDurationSeconds;
	
	private JobState endState;
	
	public Job(String jobIdentifier, String dataSetIdentifier) {
		this(jobIdentifier, dataSetIdentifier, null, null, null);
	}
	
	public Job(String jobIdentifier, String dataSetIdentifier, String dihPath, Integer pollIntervalSeconds, Integer maxWorkDurationSeconds) {
		
		if(jobIdentifier == null || jobIdentifier.length() == 0)
			throw new IllegalArgumentException("Attribute jobIdentifier must not be null in constructor");
		
		if(dataSetIdentifier == null || dataSetIdentifier.length() == 0)
			throw new IllegalArgumentException("Attribute dataSetIdentifier must not be null in constructor");
		
		this.jobIdentifier = jobIdentifier;
		this.dataSetIdentifier = dataSetIdentifier;
		
		this.dihPath = dihPath;
		this.pollIntervalSeconds = pollIntervalSeconds;
		this.maxWorkDurationSeconds = maxWorkDurationSeconds;
	}
	
	public Integer getMaxWorkDurationSeconds() {
		return maxWorkDurationSeconds;
	}

	public void setMaxWorkDurationSeconds(Integer maxWorkDurationSeconds) {
		this.maxWorkDurationSeconds = maxWorkDurationSeconds;
	}

	public Integer getPollIntervalSeconds() {
		return pollIntervalSeconds;
	}

	public void setPollIntervalSeconds(Integer setPollIntervalSeconds) {
		this.pollIntervalSeconds = setPollIntervalSeconds;
	}

	public String getJobIdentifier() {
		return jobIdentifier;
	}

	public String getDataSetIdentifier() {
		return dataSetIdentifier;
	}	
	
	public String getDsIdentAsEntityName() {
		if(dataSetIdentifier == null)
			return null;
		
		String entityName = dataSetIdentifier.replace(".", "_");		
		return entityName;
	}	

	public String getDihPath() {
		return dihPath;
	}

	public void setDihPath(String dihPath) {
		this.dihPath = dihPath;
	}
	
	public JobState getEndState() {
		return endState;
	}

	public void setEndState(JobState endState) {
		this.endState = endState;
	}

	@Override
	public String toString() {
		String repr = null;
		
		if(endState == null)
			repr = MessageFormat.format("Job (id:{0}, ds:{1})", jobIdentifier, dataSetIdentifier);
		else
			repr = MessageFormat.format("Job (id:{0}, ds:{1}, ended:{2})", jobIdentifier, dataSetIdentifier, endState);
		
		return repr;
	}
}
