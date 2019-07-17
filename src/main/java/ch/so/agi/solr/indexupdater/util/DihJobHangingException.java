package ch.so.agi.solr.indexupdater.util;

/*
 * Thrown when all abort calls to solr from this monitoring
 * service did not work.
 */
public class DihJobHangingException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public DihJobHangingException(String msg) {
		super(msg);
	}
}
