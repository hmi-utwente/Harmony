package flipper;

import static nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
 * This class will issue a full restart of our system (including the flipper module!)
 * @author daniel
 */

public class SystemRestarter extends FlipperMiddleware {

	private static Logger logger = LoggerFactory.getLogger(SystemRestarter.class.getName());
	private ObjectMapper om;


	public SystemRestarter(String middlewareProps) {
		super(middlewareProps);
		om = new ObjectMapper();
	}

	public boolean init() {
		return true;
	}
	
	public void doSystemRestart() {
		logger.debug("Doing a system restart");
        send("restart");
	}
	
}