package ch.so.agi.solr.indexupdater.util;

import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.HashSet;

import org.junit.Test;

public class IdentifierForCurrentTimeTests {

	
	/*
	 * - Ob 1000 Aufrufe verteilt auf 7 Tage keine Kollisionen gibt
	 * - Ob thread sleep funktioniert
	 * - Ob der seed übergang sauber funktioniert
	 */
	
	/*
	 * Generate two identifier strings without needing the second
	 * generate() method to sleep.
	 */
	@Test
	public void generateWithoutSleep_OK() throws InterruptedException {
		IdentifierForCurrentTime._initializeForTesting(1, 20);
		
		String id1 = IdentifierForCurrentTime.generate();
		Thread.sleep(50);
		String id2 = IdentifierForCurrentTime.generate();
		
		assertNotEquals(id1, id2);		
		assertNotNull(id1);
	}
	
	/*
	 * Generate two identifier strings with needing the second
	 * generate() method to sleep.
	 */
	@Test
	public void generateWitSleep_OK() {
		IdentifierForCurrentTime._initializeForTesting(1, 20);
		
		String id1 = IdentifierForCurrentTime.generate();
		String id2 = IdentifierForCurrentTime.generate();
		
		assertNotEquals(id1, id2);		
		assertNotNull(id1);
	}
	
	/*
	 * Generate two identifier strings with seeding every time
	 */
	@Test
	public void reseed_OK() throws InterruptedException {
		IdentifierForCurrentTime._initializeForTesting(1, 10);
		
		String id1 = IdentifierForCurrentTime.generate();
		Thread.sleep(1100);
		String id2 = IdentifierForCurrentTime.generate();
		
		assertTrue(IdentifierForCurrentTime._inLastSecondReseeded());
		
		assertNotEquals(id1, id2);		
		assertNotNull(id1);
		assertNotNull(id2);
	}
	
	/*
	 * Tests that the algorithm does not generate duplicates when working
	 * with the same seed.
	 */
	@Test
	public void noDuplicatesWithSameSeed() {
		
		HashSet<String> ids = new HashSet<>(1000);
		IdentifierForCurrentTime._initializeForTesting(1, 2);
	
		for (int i=0; i<500; i++) {
			String id = IdentifierForCurrentTime.generate();
			
			boolean couldAdd = ids.add(id);

			assertTrue(
					MessageFormat.format("Could not add id {0} as it already exists in the set ids. Set content {1}.", id, ids), 
					couldAdd);
		}
	}

}

