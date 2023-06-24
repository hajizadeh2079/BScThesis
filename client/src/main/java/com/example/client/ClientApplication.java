package com.example.client;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}

@RestController
class MyController {
    private final Timer responseTimer;

    public MyController(MeterRegistry meterRegistry) {
        this.responseTimer = meterRegistry.timer("response.time");
    }

    @Timed(value = "request.rate", description = "Request Rate")
    @GetMapping("/hello")
    public String hello() {
        Timer.Sample sample = Timer.start();
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        sample.stop(responseTimer);
        return "Hello, World!";
    }
}
