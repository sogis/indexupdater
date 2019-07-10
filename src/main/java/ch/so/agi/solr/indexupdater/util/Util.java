package ch.so.agi.solr.indexupdater.util;

import java.net.URI;
import java.text.MessageFormat;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

public class Util {
	
	private static final String SOLR_HOST = "localhost";
	private static final int SOLR_PORT = 8983;
	
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

	public static URI buildUrl(String path, String[] queryParams) {
		
		if(queryParams != null && queryParams.length % 2 != 0) //must always be even number
			throw new RuntimeException("Array queryParams must have even number of cells");
		
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme("http");
		builder.host(Util.SOLR_HOST);
		builder.port(Util.SOLR_PORT);
		builder.path(path);
		
		if(queryParams != null) {
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			for(int i=0; i<queryParams.length; i+=2) {
				map.add(queryParams[i], queryParams[i+1]);
			}
			builder.queryParams(map);
		}
		
		return builder.build().toUri();
	}

}
