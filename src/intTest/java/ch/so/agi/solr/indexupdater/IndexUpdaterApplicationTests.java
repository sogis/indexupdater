package ch.so.agi.solr.indexupdater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexUpdaterApplicationTests {
	
	private static final String PROTOCOL = "http";
	private static final String HOST = "localhost";
	private static final int PORT = 8080;
	
	private static final String PATH_QUEUE = "queue";
	private static final String PATH_STATUS = "status";

	@Test
	public void UpdateSlice_WithNoPreviousDocs_OK() {
		/*
		 * Laden einer separaten entity, welche noch keine Dokumente im Index hat.
		 * 1. Laden mit einem neuen job anstossen
		 * 2. Pollen, bis State des Jobs "ended succesful" ist
	 */		
	}
	
	
	
	@Test
	public void UpdateSlice_WithPreviousDocs_OK() {
		/*
		 * Laden einer separaten entity, welche noch keine Dokumente im Index hat.
		 * 1. Laden mit zwei neuen jobs anstossen
		 * 2. Pollen, bis State des zweiten jobs "ended_ok" ist
	 */		
	}
	
	@Test
	public void UpdateSlice_CleanAbort_WhenExceedingMaxWorkDuration() {
		/*
		 * Laden einer separaten entity, welche noch keine Dokumente im Index hat.
		 * 1. Laden mit einem neuen jobs anstossen, welcher lange dautert, poll und maxduration kurz setzen
		 * 2. Pollen, bis State des jobs ended_aborted ist
	 */		
	}
	
	@Test
	public void UpdateSlice_Exception_WhenUsingWrongDatasetName() {
		/*
		 * Laden einer separaten entity, welche noch keine Dokumente im Index hat.
		 * 1. Laden mit einem neuen job anstossen
		 * 2. Pollen, bis State des Jobs "exception" ist
		 * */	
	}
	
	@Test
	public void UpdateSlice_Exception_WhenHavingSkippedDocuments() {
		
	}
	
	@Test()
	public void JobChain_SuccesAfterError_OK() {
		/*
		 * Laden einer separaten entity, welche noch keine Dokumente im Index hat.
		 * 2. Pollen, bis State des 2. Jobs "success" ist, mit state des ersten jobs "exception"
		 * */	
	}
	
	
	
	
	
	
	
	
	
	/*


 * 
 * - Call with wrong datasetname
 * --> needs keeping the past x jobs in memory to get if the state was successful
 * 
 * - job chain: success success
 * - job chain: error success
 * 
 * - count mismatch between view result and index sum
 * --> needs view returning null for mandatory field
	 */

}
