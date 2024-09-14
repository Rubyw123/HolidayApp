package com.az.holiday;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HolidayApplication {

	public static void main(String[] args) {
        SpringApplication.run(HolidayApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(WeatherApp weatherApp){
		return args -> {
			weatherApp.run();
		};
	}



}
