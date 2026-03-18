package fr.umontpellier.iut.discordbot.config;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

public class ConfigLoader {
    private final Logger logger;
    private final Gson gson;
    private ConfigStructure config = null;

    public ConfigLoader() {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.gson = new Gson();
        this.readConfigFile();
    }

    public void readConfigFile() {
        String envPathToConfig = System.getenv("CONFIG_PATH");
        String pathToConfig = envPathToConfig != null ? envPathToConfig : "config.json";
        logger.debug("Loading config file from " + pathToConfig);

        try {
            Reader reader = new FileReader(pathToConfig);
            this.config = gson.fromJson(reader, ConfigStructure.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public ConfigStructure get() {
        return config;
    }
}
