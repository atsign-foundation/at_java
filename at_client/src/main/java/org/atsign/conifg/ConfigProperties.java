package org.atsign.conifg;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {
    private static boolean isLoaded = false;
    private static Properties properties = new Properties();

    private static void loadProperties() throws IOException, FileNotFoundException {
        if (isLoaded) {
            return;
        }
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("config.properties")) {
            properties.load(inputStream);
            isLoaded = true;
        }
    }

    public static String getApiKey() throws IOException, FileNotFoundException {
        if (System.getProperty("api_key") != null) {
            return System.getProperty("api_key");
        }
        
        loadProperties();
        return properties.getProperty("api_key");

    }
}
