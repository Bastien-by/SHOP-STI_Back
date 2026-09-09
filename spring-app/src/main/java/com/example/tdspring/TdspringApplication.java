package com.example.tdspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TdspringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TdspringApplication.class, args);
    }
}
