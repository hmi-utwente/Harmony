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
 * Simple class to plan behaviours, generate the appropriate BML and send it to the correct agent/tablet
 * @author daniel
 */

public class BehaviourPlanner extends FlipperMiddleware {

	private static Logger logger = LoggerFactory.getLogger(BehaviourPlanner.class.getName());
	private ObjectMapper om;

	public String templateDir;
	public static final String TEMPLATE_EXTENSION = ".xml";
	public static final String TEMPLATE_PLACEHOLDER_CHAR = "$";
	public static final int TEMPLATE_ITERATION_DEPTH = 10;

	public BehaviourPlanner(String middlewareProps, String templateDir) {
		super(middlewareProps);
		this.templateDir = templateDir;
		om = new ObjectMapper();
	}

	public BehaviourPlanner(String middlewareProps) {
		this(middlewareProps, "behaviours");
	}

	public boolean init() {
		//send a behaviour to reset the robot and to clear all pending behaviours in ASAP
		killLingeringBehaviours();
		return true;
	}
	
	public void planAndSendBehaviour(String requestJson) {
		try {
			logger.debug("Processing json: {}", requestJson);
			JsonNode jn = om.readTree(requestJson);
			String bmlId = jn.path("requestId").asText("bmlid");
			JsonNode template = jn.path("template");
			JsonNode placeholders = jn.path("placeholders");
			
			if(template.isTextual()) {
				String fileContent = readFile(templateDir + "/" + template.asText() + TEMPLATE_EXTENSION);
				String bmlContent = fillPlaceholders(fileContent, placeholders);
				sendBML("<bml id=\"" + bmlId + "\" xmlns=\"http://www.bml-initiative.org/bml/bml-1.0\" xmlns:sze=\"http://hmi.ewi.utwente.nl/harmonyengine\" xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" xmlns:mwe=\"http://hmi.ewi.utwente.nl/middlewareengine\">" + bmlContent + "</bml>");
			}
			
			System.gc();
		} catch (IOException e) {
			logger.error("Unable to load BML template file for the following request: {}", requestJson);
			e.printStackTrace();
		}
	}
	
	public void killLingeringBehaviours() {
		String bml = "<bml id=\"killAllBehaviour\" xmlns=\"http://www.bml-initiative.org/bml/bml-1.0\" xmlns:sze=\"http://hmi.ewi.utwente.nl/harmonyengine\" xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" xmlns:mwe=\"http://hmi.ewi.utwente.nl/middlewareengine\" composition=\"REPLACE\">"+ "</bml>";
		sendBML(bml);
	}
	
	/**
	 * This is the "killswitch". This stops all running behaviours and audio, and resets the child tablet screen to blank
	 * TODO
	 */
	public void killAllBehaviour() {
		String bml = "<bml id=\"killAllBehaviour\" xmlns=\"http://www.bml-initiative.org/bml/bml-1.0\" xmlns:sze=\"http://hmi.ewi.utwente.nl/harmonyengine\" xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" xmlns:mwe=\"http://hmi.ewi.utwente.nl/middlewareengine\" composition=\"REPLACE\">"
						+ "<sze:stopAnimation id=\"stopAnimation\" start=\"0\"/>"
					+ "</bml>";
		sendBML(bml);
	}
	
	public static void main(String[] args) {
		BehaviourPlanner bp = new BehaviourPlanner("Prototype1/config/AsapBMLPipe.properties");
		// TODO: not sure what the code below intends to do.
		// bp.planAndSendBehaviour("{\"target\" : \"child_tablet\", \"template\": \"childTabletStep3\", \"placeholders\" : [{\"id\":\"testing\"}, {\"something\":\"else\"}]}");
	}
	
	/**
	 * To send BML data to an ASAP BML pipe middleware, it needs to be 
	 * formatted in the following way:
	 *   { "content": { "bml": "$data" } }
	 */
	public void sendBML(String bml) {
		logger.debug("Sending BML: "+bml);
        ObjectNodeBuilder on = object();
        try {
			on.with("content", URLEncoder.encode(bml, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
        ObjectNodeBuilder onWrap = object();
        onWrap.with("bml", on.end());
        middleware.sendData(onWrap.end());
	}
	

	/**
	 * Helper function for filling the placeholders in a certain content with specific values. (Using quite a bruteforce approach)
	 * This function continues iteratively replacing the placeholders untill there are no more changes in the content (or maximum depth of TEMPLATE_ITERATION_DEPTH is reached), making it possible to nest placeholders in a template.
	 * TODO: we could add a check here to see if there is a potential infinite recursion in the placeholder matching.. i.e. in the form of "$PLACE$ = text $PLACE$ text"
	 * @param content the template contents
	 * @param placeholders the placeholder names and values
	 * @return the contents with as many placeholders filled as possible
	 */
	private String fillPlaceholders(String content, JsonNode placeholders){
		String oldContent = "";
		String newContent = content;
		
		//parse and store the placeholders that we use to fill in the template
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		if(placeholders.isArray()) {
			for(JsonNode ph : placeholders) {
				 Iterator<Map.Entry<String, JsonNode>> kv = ph.fields();
				while(kv.hasNext()) {
					Map.Entry<String, JsonNode> kve = kv.next();
					keys.add(kve.getKey());
					values.add(kve.getValue().asText());
				}
			}
		}
		
		int it = 0;
		while(it < TEMPLATE_ITERATION_DEPTH && newContent.contains(TEMPLATE_PLACEHOLDER_CHAR)){
			oldContent = newContent;
			
			//now replace the template placeholders with the arguments provided to this function
			for(int i = 0; i< keys.size(); i++){
				String key = keys.get(i);
				String value = values.get(i);
				newContent = newContent.replace(TEMPLATE_PLACEHOLDER_CHAR+key+TEMPLATE_PLACEHOLDER_CHAR, value);
			}
			
			//if there are no more changes we are done!
			if(oldContent.equals(newContent)){
				break;
			}
			
			it++;
		}
		
		return newContent;
	}

	/**
	 * Returns contents from file specified in filename
	 * @param filename the file to read
	 * @return the contents of the file
	 * @throws IOException if file is not found
	 */
	private String readFile(String filename) throws IOException {
        BufferedReader br = null;
        try
        {
             br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(filename)));
        } catch (Exception e)
        {
        	e.printStackTrace();
            throw new RuntimeException("Cannot read file " +filename);
        }
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append("\n");
	            line = br.readLine();
	        }
	        return sb.toString();
	    } finally {
	        br.close();
	    }
	}
	
}