package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomizedValidationDemoApplicationTests {

	@Test
	void contextLoads() {
		System.out.println("20"!="" && "20".matches("^[1-9]\\d*$"));
		
	}

}
