package ch.so.agi.solr.indexupdater.util;

/*
 * Thrown when all abort calls to solr from this monitoring
 * service did not work.
 */
public class DihJobHangingException extends RuntimeException {
	public DihJobHangingException(String msg) {
		super(msg);
	}
}
