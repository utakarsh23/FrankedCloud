package com.shresth.FrankenCloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.ControllerAdvice;

@SpringBootApplication
@ControllerAdvice
@EnableAsync
public class FrankenCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(FrankenCloudApplication.class, args);
	}

}
