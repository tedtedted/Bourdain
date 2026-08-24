package com.tedredington.bourdain;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableAsync backs @ApplicationModuleListener (the sync → matching handoff
// runs asynchronously after the publishing transaction commits).
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BourdainApplication {

    public static void main(String[] args) {
        SpringApplication.run(BourdainApplication.class, args);
    }

    /** Injectable clock so time-dependent logic (relocation matching) is testable. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
