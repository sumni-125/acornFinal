package com.example.ocean;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
		"com.example.ocean.mapper",
		"com.example.ocean.repository"
})
public class OceanApplication {

	public static void main(String[] args) {
		SpringApplication.run(OceanApplication.class, args);
	}

}
