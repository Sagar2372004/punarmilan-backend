package com.punarmilan.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PunarmilanBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PunarmilanBackendApplication.class, args);

		System.err.println("Aplication Started ..........");

	}

}
