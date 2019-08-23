package ch.so.agi.solr.indexupdater.jobexec;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.model.JobState;
import ch.so.agi.solr.indexupdater.util.Settings;
import ch.so.agi.solr.indexupdater.util.Util;

/*
 * Handles the complete update of one entity / facet of a
 * Solr document index.
 * Updates are made by first deleting all documents of the
 * given facet and subsequently loading the documents from
 * the source db using the dataimporthandler.
 */
public class IndexSliceUpdater {		
    private static final Logger log = LoggerFactory.getLogger(IndexSliceUpdater.class);
    
    private static Settings settings = Settings.instance();
    
	private Job job;
	
	private int lastDocCount;
	private ObjectMapper mapper;
	
	
	public IndexSliceUpdater(Job jobInfo) {
		this.job = jobInfo;
		this.mapper = new ObjectMapper();	
				
		this.lastDocCount = queryDocCount();
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
				
		URI url = Util.buildSolrUrl(settings.getSolrPathQuery(), qParams);
		
		log.debug("{}: Querying doc count with url {}", job.getJobIdentifier(), url);
		
		HttpRequest req = HttpRequest.newBuilder(url).GET().build();	
		HttpResponse<String> response = Util.sendBare(req, job.getJobIdentifier());

		Util.assert200(response, url, job.getJobIdentifier());
		
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
	
	public void execute() {
		deleteAllDocsInFacet();
		assertAfterDeleteCount();
		
		DataImportSuperviser importer = new DataImportSuperviser(job);
		importer.execute();
		
		JobState endState = importer.determineEndState();		
		job.setEndState(endState);
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
		

		URI url = Util.buildSolrUrl(
				settings.getSolrPathUpdate(), 
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
		
		HttpResponse<String> resp = Util.sendBare(req, job.getJobIdentifier());
		Util.assert200(resp, url, job.getJobIdentifier());
		
		Util.sleep(commitPeriodMillis);
		
		log.info("{}: Sent delete request. body: {}. url: {}.", 
				job.getJobIdentifier(),
				root,
				url);
	}
}
