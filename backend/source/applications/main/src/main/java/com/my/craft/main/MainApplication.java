package com.my.craft.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages pulls in shared config from imported modules/* libraries
// (e.g. com.my.craft.security, com.my.craft.filestorage), which live outside this app's own base
// package.
@SpringBootApplication(scanBasePackages = {"com.my.craft.main", "com.my.craft.security", "com.my.craft.filestorage"})
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
