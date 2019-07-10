package ch.so.agi.solr.indexupdater.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.so.agi.solr.indexupdater.model.Job;

/*
 * Handles the complete update of one entity / facet of a
 * Solr document index.
 * Updates are made by first deleting all documents of the
 * given facet and subsequently loading the documents from
 * the source db using the dataimporthandler.
 * For part of the Solr API, the responses give no indication
 * of whether a request was understood by Solr or not.
 * To alleviate, this class makes use of the index version,
 * which must be different after each manipulation of the
 * index.
 * 
 * Integration Tests:
 * - Update of slice that had documents
 * - Update of slice that had no documents
 * --> Poll until job is gone
 * 
 * 
 * - aborting job
 * --> abort long running job after 1 minute --> needs refactoring to seconds for abort time
 * 
 * - Call with wrong datasetname
 * --> needs keeping the past x jobs in memory to get if the state was successful
 * 
 * - job chain: success success
 * - job chain: error success
 * 
 * - count mismatch between view result and index sum
 * --> needs view returning null for mandatory field
 * 
 * 
 */
public class IndexSliceUpdater {		
    private static final Logger log = LoggerFactory.getLogger(IndexSliceUpdater.class);
	
	private static final String SOLR_PATH_QUERY = "solr/gdi/select";
	private static final String SOLR_PATH_UPDATE = "solr/gdi/update";
		
	private Job job;
	private HttpClient client;
	
	private int lastDocCount;
	private ObjectMapper mapper;
	
	
	public IndexSliceUpdater(Job jobInfo) {
		this.job = jobInfo;
		this.mapper = new ObjectMapper();	
		
		this.client = configureNewClient();		
		this.lastDocCount = queryDocCount();
	}
	
	private static HttpClient configureNewClient() {
		
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
		
		return client;
	}	
	
	private int queryDocCount() {		
		int count = -1;
		
		String filter = MessageFormat.format("facet:{0}", job.getDataSetIdentifier());
		
		String[] qParams = new String[] {
				"omitHeader", "true",
				"q", filter,
				"rows", "1",
				"fl", "display",
				"facet", "true",
				"facet.field", "facet"
		};
		
		URI url = Util.buildUrl(SOLR_PATH_QUERY, qParams);
		
		log.debug("{}: Querying doc count with url {}", job.getJobIdentifier(), url);
		
		HttpRequest req = HttpRequest.newBuilder(url).GET().build();	
		HttpResponse<String> response = sendBare(req);

		assert200(response, url);
		
		JsonNode root = parseToNodes(response.body());
		JsonNode countArray = root.path("facet_counts").path("facet_fields").path("facet");
		if(!countArray.isArray()) {
			throw new RuntimeException(
					MessageFormat.format(
							"Expected facet_counts.facet_fields.facet in {0} to be a json array", 
							response)
					);
		}
		
		boolean isCount = false;
		for (JsonNode objNode : countArray) {
			if(isCount) {
				count = objNode.asInt();
				break;
			}
			
			String value = objNode.asText();
			if(job.getDataSetIdentifier().equals(value))
				isCount = true;				
		}
		
		return count;
	}
	
	private HttpResponse<String> sendBare(HttpRequest req){
		HttpResponse<String> resp;
		
		try {
			resp = client.send(req, BodyHandlers.ofString());
		}
		catch(Exception e) {
			String msg = MessageFormat.format(
					"{0}: Exception occured when sending http request. {1}", 
					job.getJobIdentifier(), 
					e.getMessage()
					);
			
			log.error(msg);
			throw new RuntimeException(msg, e);
		}
		
		return resp;
	}
	
	private JsonNode parseToNodes(String json) {		
		JsonNode res = null;
		
		try {
			res = mapper.readTree(json);			
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return res;
	}
	
	private void assert200(HttpResponse<String> response, URI url) {
		if(response == null) {
			String msg = MessageFormat.format("{0}: Got null response for request {1}", job.getJobIdentifier(), url);
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		if(response.statusCode() != 200) {
			String msg = MessageFormat.format(
					"{0}: Got status code {1} for request {2}",
					job.getJobIdentifier(),
					response.statusCode(), 
					url);
			
			log.error(msg);
			throw new RuntimeException(msg);
		}
	}
	
	public void execute() {
		deleteAllDocsInFacet();
		assertAfterDeleteCount();
		startImport();
		
		DihPoller poller = new DihPoller(job);
		poller.execute();

		assertAfterInsertCount();
	}
	
	private void assertAfterInsertCount() {
		int curCount = queryDocCount();
		
		if ( !(curCount > this.lastDocCount) ) {
			String msg = MessageFormat.format(
					"{0}: Insert request failed. Still got {1} documents in index for dataset {2} (as before delete operation).)", 
					job.getJobIdentifier(),
					curCount,
					job.getDataSetIdentifier()
					);
			
			log.error(msg);					
			throw new RuntimeException(msg);
		}
		else {
			this.lastDocCount = curCount;
		}		
	}
	
	private void assertAfterDeleteCount() {
		int curCount = queryDocCount();
		
		if (this.lastDocCount < 0) {
			log.warn(
					"{}: Can not verify delete. Solr had no documents of type {} in index before delete",
					job.getJobIdentifier(),
					job.getDataSetIdentifier());
		}
		else if(curCount == this.lastDocCount) {
			String msg = MessageFormat.format(
					"{0}: Delete request failed. Still got {1} documents in index for dataset {2} (as before delete operation).)", 
					job.getJobIdentifier(),
					curCount,
					job.getDataSetIdentifier()
					);
			
			log.error(msg);					
			throw new RuntimeException(msg);
		}
		else {			
			this.lastDocCount = curCount;
		}
	}
	
	private void deleteAllDocsInFacet() {
		int commitPeriodMillis = 1000;
		

		URI url = Util.buildUrl(
				SOLR_PATH_UPDATE, 
				new String[] {"commitWithin", Integer.toString(commitPeriodMillis)}
				);

		
		ObjectNode delete = mapper.createObjectNode();
		delete.put("query", MessageFormat.format("facet:{0}", job.getDataSetIdentifier()));
		
		ObjectNode root = mapper.createObjectNode();
		root.set("delete", delete);
		
		HttpRequest req = HttpRequest.newBuilder(url)
				.POST(BodyPublishers.ofString(root.toString()))
				.header("Content-Type", "application/json")
				.build();
		
		HttpResponse<String> resp = sendBare(req);
		assert200(resp, url);
		
		Util.sleep(commitPeriodMillis);
		
		log.info("{}: Sent delete request. body: {}. url: {}.", 
				job.getJobIdentifier(),
				root,
				url);
	}
	

	private void startImport() {
		
		String filter = MessageFormat.format("facet:{0}", job.getDataSetIdentifier());
		
		String[] qParams = new String[] {
				"command", "full-import",
				"entity", job.getDsIdentAsEntityName(),
				"clean", "false"
		};
		
		URI url = Util.buildUrl(job.getDihPath(), qParams);
		
		HttpRequest req = HttpRequest.newBuilder(url).build();
		HttpResponse<String> res = sendBare(req);
		
		assert200(res, url);
		
		log.info("{}: Sent insert request with url: {}.", 
				job.getJobIdentifier(),
				url);
	}
}