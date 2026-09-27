package com.my.craft.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
            "com.my.craft.main",
            "com.my.craft.security",
            "com.my.craft.filestorage",
            "com.my.craft.auditlog",
            "com.my.craft.opensearch"
        })
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
