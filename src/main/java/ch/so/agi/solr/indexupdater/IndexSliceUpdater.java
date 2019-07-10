package ch.so.agi.solr.indexupdater;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.so.agi.solr.indexupdater.model.Job;
import ch.so.agi.solr.indexupdater.util.Util;

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
 * 
 * Koord:
 * - Tel / Email Höhenmodell
 * - Stand Solr
 * - Stand altlasten
 */
public class IndexSliceUpdater {		
    private static final Logger log = LoggerFactory.getLogger(IndexSliceUpdater.class);
	
	private static final String SOLR_HOST = "localhost";
	private static final int SOLR_PORT = 8983;
	private static final String SOLR_PATH_QUERY = "solr/gdi/select";
	private static final String SOLR_PATH_UPDATE = "solr/gdi/update";
		
	private Job job;
	private RestTemplate restTemplate;
	
	private int lastDocCount;
	private ObjectMapper mapper;
	
	
	public IndexSliceUpdater(Job jobInfo) {
		this.job = jobInfo;
		this.mapper = new ObjectMapper();	
		
		RestTemplate templ = new RestTemplate();
		registerSolrContentTypes(templ);
		
		this.restTemplate = templ;
	
		
		this.lastDocCount = queryDocCount();
	}
	
	/*
	 * Solr returns responses with text/plain;charset=utf-8, ...
	 * This is unprecise, and must be registered with the RestTemplate.
	 */
	private static void registerSolrContentTypes(RestTemplate templ) {
		
		List<MediaType> supportedTypeList = new ArrayList<MediaType>();
		supportedTypeList.add(MediaType.APPLICATION_JSON);
		supportedTypeList.add(MediaType.TEXT_PLAIN);
		
		List<HttpMessageConverter<?>> list = templ.getMessageConverters();
		
		int foundCount = 0;
		for (HttpMessageConverter<?> converter : list) {
			
			if(converter instanceof MappingJackson2HttpMessageConverter) {
				foundCount++;
				
				MappingJackson2HttpMessageConverter jConf = (MappingJackson2HttpMessageConverter)converter;
				jConf.setSupportedMediaTypes(supportedTypeList);
			}
		}
		
		if(foundCount == 0) {
			throw new RuntimeException(
					"Did not find MappingJackson2HttpMessageConverter in RestTemplate to configure additional response content types");
		}

		
		
		
		/*
		
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();        
		//Add the Jackson Message converter
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

		// Note: here we are making this converter to process any kind of response, 
		// not only application/*json, which is the default behaviour
		converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));        
		messageConverters.add(converter);  
		restTemplate.setMessageConverters(messageConverters); 
		*/
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
		
		String url = buildUrl(SOLR_PATH_QUERY, qParams);
		
		log.info("{}: Querying doc count with url {}", job.getJobIdentifier(), url);
		
		String response = restTemplate.getForObject(url, String.class);
		//assert200(response, url);
		
		JsonNode root = parseToNodes(response);
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
	
	private void assert200(ResponseEntity<String> response, String url) {
		if(response == null) {
			String msg = MessageFormat.format("{0}: Got null response for request {1}", job.getJobIdentifier(), url);
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		if(response.getStatusCodeValue() != 200) {
			String msg = MessageFormat.format(
					"{0}: Got status code {1} for request {2}",
					job.getJobIdentifier(),
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
		
		String url = buildUrl(
				SOLR_PATH_UPDATE, 
				new String[] {"commitWithin", Integer.toString(commitPeriodMillis)}
				);
		
		ObjectNode delete = mapper.createObjectNode();
		delete.put("query", MessageFormat.format("facet:{0}", job.getDataSetIdentifier()));
		
		ObjectNode root = mapper.createObjectNode();
		root.set("delete", delete);
		
		restTemplate.postForObject(url, root, ResponseEntity.class); 
		
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
		
		String url = buildUrl(job.getDihPath(), qParams);
		
		ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
		assert200(res, url);
		
		log.info("{}: Sent insert request with url: {}.", 
				job.getJobIdentifier(),
				url);
	}
}
