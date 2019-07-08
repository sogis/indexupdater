package ch.so.agi.solr.indexupdater.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hashids.Hashids;

/*
 * Uses org.hashids.Hashids to generate short friendly ids
 * for time instances. The possible number of time instances is defined by MIN_WAIT_MILLIS.
 * 
 * Dupliate ID's are not possible inside the current seed,
 * but could occur when considering a timespan longer than the livespan of a seed.
 */
public class IdentifierForCurrentTime {
	
	private static int MIN_WAIT_MILLIS = 500; //minimum wait period between generate() calls
	private static int SEED_MAX_DAYS = 7; // maximum time on the same seed
	
	private static IdentifierForCurrentTime singleton;
	private static final Logger log = LoggerFactory.getLogger(IdentifierForCurrentTime.class);
	
	private Hashids idgen;
	private LocalDateTime lastSeed;
	private LocalDateTime lastGenerate;
	
	private IdentifierForCurrentTime() {
		reseed();
		this.lastGenerate = LocalDateTime.now().minus(MIN_WAIT_MILLIS + 1, ChronoUnit.MILLIS);;
	}
	
	public static synchronized String generate() {
		if(IdentifierForCurrentTime.singleton == null)
			IdentifierForCurrentTime.singleton = new IdentifierForCurrentTime();
		
		return singleton.generateInternal();
	}
	
	private String generateInternal() {
		LocalDateTime now = LocalDateTime.now();
		
		if(lastSeed.plusDays(SEED_MAX_DAYS).isBefore(now)) 
			reseed();
		
		if(lastGenerate.plus(MIN_WAIT_MILLIS, ChronoUnit.MILLIS).isAfter(now)) {
			log.debug("sleeping for {} millis before generating the id", MIN_WAIT_MILLIS);
			Util.sleep(MIN_WAIT_MILLIS);
		}
		
		long millisElapsed = millisSinceSeedStart(); 
		String id = this.idgen.encode(millisElapsed / MIN_WAIT_MILLIS);
		
		lastGenerate = LocalDateTime.now();
		log.debug("Generated id {} for {} elapsed milliseconds since {}. (The begin of the current seed)", id, millisElapsed, lastSeed);
		
		return id;
	}
	
	private void reseed() {
		this.idgen = new Hashids(UUID.randomUUID().toString());
		this.lastSeed = LocalDateTime.now();
		
		log.debug("Created new Hashids instance with new seed.");
	}
	
	
	private long millisSinceSeedStart() {
		LocalDateTime now = LocalDateTime.now();
				
		return lastSeed.until(now, ChronoUnit.MILLIS);
	}
	
	static void _initializeForTesting(int seedMaxDays, int minWaitMillis) {
		IdentifierForCurrentTime.SEED_MAX_DAYS = seedMaxDays;
		IdentifierForCurrentTime.MIN_WAIT_MILLIS = minWaitMillis;
		
		IdentifierForCurrentTime.singleton = null;
	}
}
