package com.qrcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QrServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QrServiceApplication.class, args);
	}

}
