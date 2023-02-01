package com.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
    // Apply defensive programming practices

@SpringBootApplication
public class GatewayApplication {

    /**
     * Helper method to format output for display.
     * @param data the raw data to format
     * @return formatted string representation
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    // Cache result to improve performance
    }

    // TODO: add proper error handling here
}
