package ch.so.agi.solr.indexupdater.model;

import java.text.MessageFormat;

public class Job {
	private String jobIdentifier;
	private String dataSetIdentifier;
	
	private Integer pollIntervalSeconds;
	private String dihPath;
	private Integer maxWorkDurationMinutes;
	
	public Job(String jobIdentifier, String dataSetIdentifier) {
		this(jobIdentifier, dataSetIdentifier, null, null);
	}
	
	public Job(String jobIdentifier, String dataSetIdentifier, Integer pollIntervalSeconds, String dihPath) {
		
		if(jobIdentifier == null || jobIdentifier.length() == 0)
			throw new IllegalArgumentException("Attribute jobIdentifier must not be null in constructor");
		
		if(dataSetIdentifier == null || dataSetIdentifier.length() == 0)
			throw new IllegalArgumentException("Attribute dataSetIdentifier must not be null in constructor");
		
		this.jobIdentifier = jobIdentifier;
		this.dataSetIdentifier = dataSetIdentifier;
		this.pollIntervalSeconds = pollIntervalSeconds;
		this.dihPath = dihPath;
	}
	
	public Integer getMaxWorkDurationMinutes() {
		return maxWorkDurationMinutes;
	}

	public void setMaxWorkDurationMinutes(Integer maxWorkDurationMinutes) {
		this.maxWorkDurationMinutes = maxWorkDurationMinutes;
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

	@Override
	public String toString() {
		String repr = MessageFormat.format("Job [id:{0}, ds:{1}]", jobIdentifier, dataSetIdentifier);		
		return repr;
	}
	
	

}
