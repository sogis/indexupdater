package ch.so.agi.solr.indexupdater.util;

import java.text.MessageFormat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties()
public class Settings {
	
	private static Settings instance;
	
	private boolean configChecked = false;
	
	private String solrProtocol; //http or https
	private String solrHost;
	private int solrPort;
	private String solrPathQuery;
	private String solrPathUpdate;
	
	private int logSilenceMaxDurationSeconds;
	
	private int dihPollIntervalSeconds;
	private int dihImportMaxDurationSeconds;
	private String dihDefaultPath;
	
	public static Settings instance() {		
		return Settings.instance;
	}
	
	public Settings() {
		Settings.instance = this; //Hack to let Spring create the instance, and use it also in static context's.
	}
	
	public void assertConfigComplete() {
		
		
		if(this.configChecked)
			return;

		String[] stringSettings = new String[] {
				solrProtocol,
				solrHost,
				dihDefaultPath,
				solrPathQuery,
				solrPathUpdate
				};
		
		for(String setting : stringSettings) {
			if(setting == null || setting.length() == 0) {
				throw new RuntimeException(MessageFormat.format(
						"Indexupdater configuration is not complete. Attributes of the settings-object are {0}",
						this)
						);
			}
		}
		
		int[] intSettings = new int[] {
				solrPort,
				dihPollIntervalSeconds,
				dihImportMaxDurationSeconds,
				logSilenceMaxDurationSeconds
		};
		
		for(int i : intSettings) {
			if(i < 1) {
				throw new RuntimeException(MessageFormat.format(
						"Indexupdater configuration is not complete. Attributes of the settings-object are {0}",
						this)
						);
			}
		}
		
		this.configChecked = true;
		
	}
	
	@Override
	public String toString() {
		
		String hostAdr = MessageFormat.format(
				"Solr host address: protocol: {0}, host: {1}, port {2}, query path: {3}, update path {4}",
				solrProtocol,
				solrHost,
				solrPort,
				solrPathQuery,
				solrPathUpdate
				);
		
		String defaults = MessageFormat.format(
				"Default settings (DIH path: {0}, Poll interval [s]: {1}, Max. import duration [s]: {2}"
				+ ", Max silence duration on log [s]: {3})", 
				dihDefaultPath,
				dihPollIntervalSeconds,
				dihImportMaxDurationSeconds,
				logSilenceMaxDurationSeconds
				);
		
		String repr = MessageFormat.format("{0}. {1}", hostAdr, defaults);
		
		return repr;
	}	

	public BaseAddress getSolrBaseAddress() {
		assertConfigComplete();
		
		return new BaseAddress(solrProtocol, solrHost, solrPort);
	}

	public void setSolrProtocol(String solrProtocol) {		
		this.solrProtocol = solrProtocol;
	}

	public void setSolrHost(String solrHost) {
		this.solrHost = solrHost;
	}

	public void setSolrPort(int solrPort) {
		this.solrPort = solrPort;
	}

	public String getSolrPathQuery() {
		assertConfigComplete();
		
		return solrPathQuery;
	}

	public void setSolrPathQuery(String solrPathQuery) {	
		this.solrPathQuery = solrPathQuery;
	}

	public String getSolrPathUpdate() {
		assertConfigComplete();
		
		return solrPathUpdate;
	}

	public void setSolrPathUpdate(String solrPathUpdate) {
		this.solrPathUpdate = solrPathUpdate;
	}

	public int getLogSilenceMaxDurationSeconds() {
		assertConfigComplete();
		
		return logSilenceMaxDurationSeconds;
	}

	public void setLogSilenceMaxDurationSeconds(int logSilenceMaxDurationSeconds) {
		this.logSilenceMaxDurationSeconds = logSilenceMaxDurationSeconds;
	}

	public int getDihPollIntervalSeconds() {
		assertConfigComplete();
		
		return dihPollIntervalSeconds;
	}

	public void setDihPollIntervalSeconds(int dihPollIntervalSeconds) {
		this.dihPollIntervalSeconds = dihPollIntervalSeconds;
	}

	public int getDihImportMaxDurationSeconds() {
		assertConfigComplete();
		
		return dihImportMaxDurationSeconds;
	}

	public void setDihImportMaxDurationSeconds(int dihImportMaxDurationSeconds) {
		this.dihImportMaxDurationSeconds = dihImportMaxDurationSeconds;
	}

	public String getDihDefaultPath() {
		assertConfigComplete();
		
		return dihDefaultPath;
	}

	public void setDihDefaultPath(String dihPath) {
		this.dihDefaultPath = dihPath;
	}
}
