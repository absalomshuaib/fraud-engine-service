package com.fraud.engine.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class FraudEngineServiceApplication {

	public static void main(String[] args) throws InterruptedException {
		log.info("Welcome to my fraud engine service");
		try {
			SpringApplication.run(FraudEngineServiceApplication.class, args);
		} catch (Exception e) {
		}
	}
}
