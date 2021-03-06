package flipper;

import static nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.utwente.hmi.middleware.Middleware;
import nl.utwente.hmi.middleware.MiddlewareListener;
import nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.ObjectNodeBuilder;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;


public class FlipperMiddleware implements MiddlewareListener {

	protected BlockingQueue<JsonNode> queue = null;
	private static Logger logger = LoggerFactory.getLogger(FlipperMiddleware.class.getName());
	Middleware middleware;

	public FlipperMiddleware(String middlewareProps) {
		this.queue = new LinkedBlockingQueue<JsonNode>();
		Properties ps = new Properties();
        InputStream mwProps = FlipperMiddleware.class.getClassLoader().getResourceAsStream(middlewareProps);
        
		try {
			ps.load(mwProps);
		} catch (IOException ex) {
            logger.warn("Could not load flipper middleware props file {}", mwProps);
            ex.printStackTrace();
        }
		
		GenericMiddlewareLoader.setGlobalPropertiesFile("Prototype1/config/defaultmiddleware.properties");
		
        GenericMiddlewareLoader gml = new GenericMiddlewareLoader(ps.getProperty("middleware"), ps);
        middleware = gml.load();
        middleware.addListener(this);
	}
	
	
	// { "content": "$data" }
	public void send(String data) {
		//ObjectNodeBuilder on = object();

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(data);

			middleware.sendData(jsonNode);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public boolean hasMessage() {
		return isConnected() && !queue.isEmpty();
	}
	
	/**
	 * only call this when hasMessage() returned true,
	 * as it is otherwise blocking until it receives a message.
	 */
	public String getMessage() {
		try {
			JsonNode msg = queue.take();
			logger.debug("Processing message from middleware: {}", msg);
			return msg.toString();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "{}";
	}

	@Override
	public void receiveData(JsonNode jn) {
		queue.clear();
		queue.add(jn);
	}
	
	public boolean isConnected() {
		return middleware != null;
	}
	
	public static void Log(String s) {
        logger.debug("\n===\n{}\n===", s);
	}

}