package ch.so.agi.solr.indexupdater.jobexec;

import java.util.Set;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Convenience class providing the relevant dataimporthandler
 * status response attributes in POJO-form.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DihResponse {
	
	private static Set<String>  STATUS_SET;
	public static final String STATUS_IDLE = "idle";
	public static final String STATUS_BUSY = "busy";

	private String status;
	private int docs_processed;	
	private int docs_skipped;
	
	static {
		STATUS_SET = new HashSet<String>();
		STATUS_SET.add(STATUS_IDLE);
		STATUS_SET.add(STATUS_BUSY);
	}
	
	@JsonProperty("statusMessages")
	private void unpackMessages(Map<String, String> statusMessages){
		
		String processed = statusMessages.get("Total Documents Processed");
		if(processed != null && processed.length() > 0)
			this.docs_processed = Integer.parseInt(processed);
		else
			this.docs_processed = 0;
		
		String skipped = statusMessages.get("Total Documents Skipped");
		if(skipped != null && skipped.length() > 0)
			this.docs_skipped = Integer.parseInt(skipped);
		else
			this.docs_skipped = 0;
	}
	
	/*
	 * False if dih is busy
	 */
	public boolean isDihIdle() {
		return STATUS_IDLE.equalsIgnoreCase(status);
	}	
	
	/*
	 * Only here to map json to java via jackson
	 */
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {		
		if (!STATUS_SET.contains(status))
			throw new RuntimeException(
					MessageFormat.format("Solr returned unknown and unhandled status {1}. Aborting...", status)
					);
		
		this.status = status;
	}

	public int getDocs_processed() {
		return docs_processed;
	}
	
	public int getDocs_skipped() {
		return docs_skipped;
	}

	@Override
    public String toString() { 
		String objectString = super.toString();

        String repr = MessageFormat.format(
        		"{0} [ status: {1}, processed: {2}. Objectinfo: {3}",
        		DihResponse.class.getName(),
        		status,
        		docs_processed,
        		objectString
        		);
        
        return repr;
    } 
}
