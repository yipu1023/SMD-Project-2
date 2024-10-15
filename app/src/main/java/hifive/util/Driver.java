package hifive.util;

import hifive.HiFive;
import hifive.PropertiesLoader;

import java.util.Properties;

public class Driver {
    public static final String DEFAULT_PROPERTIES_PATH = "properties/game3.properties";

    public static void main(String[] args) {
        final Properties properties = PropertiesLoader.loadPropertiesFile(DEFAULT_PROPERTIES_PATH);
        String logResult = new HiFive(properties).runApp();
        System.out.println("logResult = " + logResult);
    }

}
