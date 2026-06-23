package com.nexusxva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexusXvaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusXvaApplication.class, args);
    }
}
