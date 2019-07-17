package ch.so.agi.solr.indexupdater.util;

/*
 * DTO for the properties of (http)
 * base addresses.
 */
public class BaseAddress {

	private String protocol;
	private String host;
	private int port;
	
	public BaseAddress(String host, int port) {
		this("http", host, port);
	}
	
	public BaseAddress(String protocol, String host, int solrPort) {
		this.protocol = protocol;
		this.host = host;
		this.port = solrPort;
	}
	
	public String getProtocol() {
		return protocol;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
