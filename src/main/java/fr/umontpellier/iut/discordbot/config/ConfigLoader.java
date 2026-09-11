package fr.umontpellier.iut.discordbot.config;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;

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
			if (this.config == null) {
				throw new IllegalStateException("Config file " + pathToConfig + " is empty");
			}
			validateConfig();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void validateConfig() {
		Map<String, String> channels = config.getChannels();
		if (channels != null) {
			for (String key : channels.keySet()) {
				if (!isKnownChannelKey(key)) {
					logger.warn("Unknown channel key '{}' in config; expected keys are {}", key, Arrays.toString(ConfigStructure.LogChannel.values()));
				}
			}
		}

		for (ConfigStructure.LogChannel channel : ConfigStructure.LogChannel.values()) {
			String channelId = config.getChannelId(channel);
			if (channelId == null) {
				logger.warn("Channel '{}' is not configured in the config file", channel);
			} else if (channelId.isBlank()) {
				logger.warn("Channel '{}' is configured but blank in the config file", channel);
			}
		}
	}

	private boolean isKnownChannelKey(String key) {
		for (ConfigStructure.LogChannel channel : ConfigStructure.LogChannel.values()) {
			if (channel.toString().equals(key) || channel.name().equals(key)) {
				return true;
			}
		}

		return false;
	}

	@NotNull
	public ConfigStructure get() {
		return config;
	}
}
