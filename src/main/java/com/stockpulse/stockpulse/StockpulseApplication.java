package com.stockpulse.stockpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockpulseApplication {
	public static void main(String[] args) {
		SpringApplication.run(StockpulseApplication.class, args);
	}
}