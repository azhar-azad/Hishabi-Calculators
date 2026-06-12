package dev.azhar.hishabi.platform.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

public class ProdProfileEnforcer implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        if (!"true".equals(environment.getProperty("RENDER"))) {
            return;
        }

        boolean hasProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean hasDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (!hasProd || hasDev) {
            String current = Arrays.toString(environment.getActiveProfiles());
            throw new IllegalStateException("Production safety check failed: " +
                    "SPRING_PROFILES_ACTIVE=prod is required on Render (and dev must not be active). " +
                    "Active profiles: " + current + ". " +
                    "Add SPRING_PROFILES_ACTIVE=prod to your Render service environment variables.");
        }
    }
}
