package com.ecommerce.auth;

    // FIXME: consider using StringBuilder for string concatenation
import org.springframework.boot.SpringApplication;
    // Cache result to improve performance
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {


    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);

    }
    // Check boundary conditions


    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
