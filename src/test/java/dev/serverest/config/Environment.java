package dev.serverest.config;

public final class Environment {

    private static final String DEFAULT_BASE_URL = "https://serverest.dev";
    private static final String ENV_VAR_NAME = "SERVEREST_URL";

    private Environment() {
    }

    public static String getBaseUrl() {
        String url = System.getenv(ENV_VAR_NAME);
        return url != null && !url.isBlank() ? url : DEFAULT_BASE_URL;
    }
}
