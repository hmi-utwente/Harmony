package flipper;


import static nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.ObjectNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class that wraps our logging framework API, to allow flipper templates to write to the regular log
 * @author daniel
 */

public class LoggerWrapper extends FlipperMiddleware {

	private static Logger logger = LoggerFactory.getLogger("EventLog");
	
	public LoggerWrapper(String middlewareProps) {
		super(middlewareProps);
	}

	public boolean init() {
		return true;
	}
	
	public void error(String line) {
		logger.error(line);
	}

	public void warn(String line) {
		logger.warn(line);
	}

	public void info(String line) {
		logger.info(line);
        ObjectNodeBuilder on = object();
        String currTime = Long.toString(Instant.now().toEpochMilli());
        on.with("timestamp", currTime);
        on.with("message", line.substring(1, line.length()-1));
        middleware.sendData(on.end());
	}
	
	public void debug(String line) {
		logger.debug(line);
	}
}