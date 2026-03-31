package br.com.bertan.finsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinsyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinsyncApplication.class, args);
	}

}
