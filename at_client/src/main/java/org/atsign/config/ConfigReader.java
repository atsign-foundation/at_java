package org.atsign.config;

import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads, reads and returns properties from the configuration file in the resources
 */
public class ConfigReader {
    private static Map<String, Object> config;
    
    public static String getProperty(String property, String subProperty) {
        if (config == null){
            loadConfig();
        }
        @SuppressWarnings("unchecked")
        Map<String, String> propertyMap = (Map<String, String>) config.get(property);
        return propertyMap.get(subProperty);
    }

    public static String getProperty(String property) {
        if (config == null){
            loadConfig();
        }
        return (String) config.get(property);
    }

    /**
     * Loads configuration properties from the yaml provided in the java/src/main/resources
     * Stores these key-value pairs in the config map
     */
    public static void loadConfig() {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("config.yaml");
        Yaml configYaml = new Yaml();
        config = configYaml.load(inputStream);
    }
}