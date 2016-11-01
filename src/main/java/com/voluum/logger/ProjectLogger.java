package com.voluum.logger;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bogdan on 31.10.16.
 * Test framework
 */
public class ProjectLogger {

	private static ch.qos.logback.classic.Logger log;

	private ProjectLogger() {
	}

	public static Logger getLogger(final String className) {
		if (log == null) {
			log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(className);
			log.setLevel(Level.toLevel(System.getProperty("logging.level")));
		}
		return log;
	}
}
