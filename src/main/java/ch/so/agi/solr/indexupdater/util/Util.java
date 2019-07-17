package ch.so.agi.solr.indexupdater.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

public class Util {
	
	private static final Logger log = LoggerFactory.getLogger(Util.class);
	
	private static HttpClient client = configureNewClient();
	
	private static HttpClient configureNewClient() {
		
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();		
		return client;
	}
	
	public static void sleep(int millis) {
		int numTries = 3;
		
		for(int i=0; i<numTries; i++) {
			try {
				Thread.sleep(millis);
			}
			catch(InterruptedException ie) {
				if( !(i<numTries) ) {
					String msg = MessageFormat.format("Sleeping thread was interrupted {0} times. Giving up", numTries);
					throw new RuntimeException(msg, ie);
				}
			}
		}
	}
	
	public static URI buildUrl(BaseAddress adr, String path, Object id) {		
		return buildUrl(adr, path, id, null);
	}	
	
	public static URI buildUrl(BaseAddress adr, String path, String[] queryParams) {		
		return buildUrl(adr, path, null, queryParams);
	}	
	
	public static URI buildSolrUrl(String path, String[] queryParams) {
		return buildUrl(Settings.instance().getSolrBaseAddress(), path, null, queryParams);
	}	

	private static URI buildUrl(BaseAddress adr, String path, Object id, String[] queryParams) {
		
		if(queryParams != null && queryParams.length % 2 != 0) //must always be even number
			throw new RuntimeException("Array queryParams must have even number of cells");
		
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme(adr.getProtocol());
		builder.host(adr.getHost());
		builder.port(adr.getPort());
		builder.path(path);
		
		if(id != null) {
			builder.pathSegment(id.toString());
		}
		
		if(queryParams != null) {
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			for(int i=0; i<queryParams.length; i+=2) {
				map.add(queryParams[i], queryParams[i+1]);
			}
			builder.queryParams(map);
		}
		
		return builder.build().toUri();
	}
	
	public static HttpResponse<String> sendBare(HttpRequest req, String jobIdent){
		HttpResponse<String> resp;
		
		try {
			resp = client.send(req, BodyHandlers.ofString());
		}
		catch(Exception e) {
			String msg = MessageFormat.format(
					"{0}: Exception occured when sending http request. {1}", 
					jobIdent, 
					e.getMessage()
					);
			
			log.error(msg);
			throw new RuntimeException(msg, e);
		}
		
		return resp;
	}

}
