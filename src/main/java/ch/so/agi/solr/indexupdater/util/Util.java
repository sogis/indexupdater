package ch.so.agi.solr.indexupdater.util;

import java.text.MessageFormat;

public class Util {
	
	public static void sleep(int millis) {
		int numTries = 3;
		
		for(int i=0; i<numTries; i++) {
			try {
				Thread.sleep(millis);
			}
			catch(InterruptedException ie) {
				if( !(i<numTries) ) {
					String msg = MessageFormat.format("Sleeping thread was interrupted {1} times. Giving up", numTries);
					throw new RuntimeException(msg, ie);
				}
			}
		}
	}

}
