package com.github.hryhoriishtanko.techstarsscraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

    @SpringBootApplication
    @EnableAsync // Enables Spring's asynchronous method execution capability
    public class TechstarsScraperApplication {

        public static void main(String[] args) {
            SpringApplication.run(TechstarsScraperApplication.class, args);
        }

    }

