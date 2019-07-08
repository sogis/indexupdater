package ch.so.agi.solr.indexupdater;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IndexUpdaterApplication {

	public static void main(String[] args) {
		SpringApplication.run(IndexUpdaterApplication.class, args);
	}

}
