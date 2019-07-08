package ch.so.agi.solr.indexupdater;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * http://localhost:8983/solr/gdi/dih?command=status
 */
public class IndexSliceUpdater {		
    private static final Logger log = LoggerFactory.getLogger(IndexSliceUpdater.class);
	
	private static final String SOLR_HOST = "localhost";
	private static final int SOLR_PORT = 8983;
	private static final String SOLR_PATH_QUERY = "gdi/select";
	private static final String SOLR_PATH_UPDATE = "gdi/update";
		
	private Job jobInfo;
	private RestTemplate restTemplate;
	
	private int lastDocCount;
	private ObjectMapper mapper;
	
	
	public IndexSliceUpdater(Job jobInfo) {
		this.jobInfo = jobInfo;
		this.restTemplate = new RestTemplate();
		this.mapper = new ObjectMapper();		
		
		this.lastDocCount = queryDocCount();
	}
	
	private int queryDocCount() {		
		int count = -1;
		
		String filter = MessageFormat.format("facet:{1}", jobInfo.getDataSetIdentifier());
		
		String[] qParams = new String[] {
				"omitHeader", "true",
				"q", filter,
				"rows", "1",
				"fl", "display",
				"facet", "true",
				"facet.field", "facet"
		};
		
		String url = buildUrl(SOLR_PATH_QUERY, qParams);
		
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		assert200(response, url);
		
		JsonNode root = parseToNodes(response.getBody());
		JsonNode countArray = root.path("facet_counts").path("facet_fields").path("facet");
		if(!countArray.isArray()) {
			throw new RuntimeException(
					MessageFormat.format(
							"Expected facet_counts.facet_fields.facet in {1} to be a json array", 
							response.getBody())
					);
		}
		
		boolean isCount = false;
		for (JsonNode objNode : countArray) {
			if(isCount) {
				count = objNode.asInt();
				break;
			}
			
			String value = objNode.asText();
			if(jobInfo.getDataSetIdentifier().equals(value))
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
	
	private void assert200(ResponseEntity<String> response, String url) {
		if(response == null) {
			String msg = MessageFormat.format("{1}: Got null response for request {2}", jobInfo.getJobIdentifier(), url);
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		if(response.getStatusCodeValue() != 200) {
			String msg = MessageFormat.format(
					"{1}: Got status code {2} for request {3}",
					jobInfo.getJobIdentifier(),
					response.getStatusCodeValue(), 
					url);
			
			log.error(msg);
			throw new RuntimeException(msg);
		}
	}
	
	private static String buildUrl(String path, String[] queryParams) {
		
		if(queryParams != null && queryParams.length % 2 != 0) //must always be even number
			throw new RuntimeException("Array queryParams must have even number of cells");
		
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme("http");
		builder.host(SOLR_HOST);
		builder.port(SOLR_PORT);
		builder.path(path);
		
		if(queryParams != null) {
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			for(int i=0; i<queryParams.length; i+=2) {
				map.add(queryParams[i], queryParams[i+1]);
			}
			builder.queryParams(map);
		}
		
		return builder.toUriString();
	}
	
	public void execute() {
		deleteAllDocsInFacet();
		assertAfterDeleteCount();
		startImport();
	}
	
	private void assertAfterDeleteCount() {
		int curCount = queryDocCount();
		
		if (this.lastDocCount == 0) {
			log.warn(
					"{}: Can not verify delete as pre-delete count for {} is 0",
					jobInfo.getJobIdentifier(),
					jobInfo.getDataSetIdentifier());
		}
		else if(curCount == this.lastDocCount) {
			String msg = MessageFormat.format(
					"{1}: Delete request failed. Still got {2} documents in index for dataset {3} (as before delete operation).)", 
					jobInfo.getJobIdentifier(),
					curCount,
					jobInfo.getDataSetIdentifier()
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
		
		String url = buildUrl(
				SOLR_PATH_UPDATE, 
				new String[] {"commitWithin", Integer.toString(commitPeriodMillis)}
				);
		
		
		/*
		 curl -X POST -H 'Content-Type: application/json' 'http://localhost:8983/solr/gdi/update?commitWithin=1000' --data-binary '
{
	"delete": {
		"query": "facet:fill_0_10k"
	}
}
'		
		 */
	}
	
	private void startImport() {
		//localhost:8983/solr/gdi/dih?command=full-import&entity=ch_so_agi_fill_0_10k&clean=false
	}
}
