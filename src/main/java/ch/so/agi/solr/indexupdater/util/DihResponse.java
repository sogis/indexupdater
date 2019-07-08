package ch.so.agi.solr.indexupdater.util;

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
	private static final String STATUS_IDLE = "idle";
	private static final String STATUS_BUSY = "busy";

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
		this.docs_processed = Integer.parseInt(
				statusMessages.get("Total Documents Processed"));
		
		this.docs_skipped = Integer.parseInt(
				statusMessages.get("Total Documents Skipped"));
	}
	
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

}