package com.voluum;

import com.voluum.logger.ProjectLogger;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Created by bogdan on 01.11.16.
 * Test framework
 */
public final class VoluumProperties {

	private static final Logger LOG = ProjectLogger.getLogger(VoluumProperties.class.getSimpleName());
	private static final String COMMON_PROPERTY_FILE = "src/test/resources/properties/common.properties";
	private static final String SECURITY_PROPERTY_FILE = "src/test/resources/properties/security.properties";
	private static VoluumProperties instance = new VoluumProperties();
	private Properties properties;

	private VoluumProperties() {
		this.properties = new Properties();
		loadPropertiesFromFiles(COMMON_PROPERTY_FILE, SECURITY_PROPERTY_FILE);
	}

	public static VoluumProperties getInstance() {
		return instance;
	}

	private void loadPropertiesFromFiles(final String... propertyFileNames) {
		for (String propertyFile : propertyFileNames) {
			propertiesFromFile(propertyFile);
		}
		afterPropertiesSet();
	}

	private void propertiesFromFile(final String fileName) {
		try (FileInputStream fileInputStream = new FileInputStream(fileName)) {
			properties.load(fileInputStream);
			LOG.debug("System properties were successfully loaded, file: {}", fileName);
		} catch (IOException e) {
			LOG.error("Error occurs during loading properties from file {}", fileName, e);
			Assertions.fail("Error occurs during loading properties!");
		}
	}

	private void afterPropertiesSet() {
		for (Map.Entry props : properties.entrySet()) {
			String key = String.valueOf(props.getKey());
			String value = String.valueOf(props.getValue());
			if (StringUtils.isBlank(System.getProperty(key))) {
				System.setProperty(key, value);
				LOG.debug("System property {} was successfully added: {}", key, value);
			}
		}
	}
}
