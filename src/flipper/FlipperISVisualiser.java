package flipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.utwente.hmi.middleware.Middleware;
import nl.utwente.hmi.middleware.MiddlewareListener;
import nl.utwente.hmi.middleware.helpers.JSONHelper;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;
import nl.utwente.hmi.middleware.worker.AbstractWorker;


public class FlipperISVisualiser extends AbstractWorker implements ActionListener, MiddlewareListener {
  private static Logger logger = LoggerFactory.getLogger(FlipperISVisualiser.class.getName());

	private Middleware middleware;

	private List<String> filters;
	
	private JFrame jf;
	private JTextField filterBox;
	private JTextArea visualiser;
	private JSONHelper jh;



	private int refreshTime;
	
	public FlipperISVisualiser(int refreshTime){
		super();
		this.refreshTime = refreshTime;
	}
	
	
	public static void main(String[] args){
		String help = "Expecting commandline arguments in the form of \"-<argname> <arg>\".\nAccepting the following argnames: middlewareprops refreshtime";

    	String mwPropFile = "Prototype1/config/defaultmiddleware.properties";
    	int refreshTime = 100;
    	
        if(args.length % 2 != 0){
        	System.err.println(help);
        	System.exit(0);
        }
        
        for(int i = 0; i < args.length; i = i + 2){
        	if(args[i].equals("-middlewareprops")){
        		mwPropFile = args[i+1];
        	} else if(args[i].equals("-refreshtime")){
        		refreshTime = Integer.parseInt(args[i+1]);
        	} else {
            	System.err.println("Unknown commandline argument: \""+args[i]+" "+args[i+1]+"\".\n"+help);
            	System.exit(0);
        	}
        }
    	
		GenericMiddlewareLoader.setGlobalPropertiesFile(mwPropFile);
		
		FlipperISVisualiser vis = new FlipperISVisualiser(refreshTime);
		vis.init();
		
	}


	private void makeGUI() {
		jf = new JFrame ("Visualise the information states");
        
		jf.setLayout(new BorderLayout());
		
		filterBox = new JTextField(20);
		filterBox.addActionListener(this);
		jf.add(filterBox, BorderLayout.NORTH);
		
		visualiser = new JTextArea(200, 20);
		visualiser.setFont(new Font("SansSerif",Font.PLAIN,12));
		jf.add(visualiser, BorderLayout.CENTER);
        
        jf.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent winEvt)
            {
                System.exit(0);
            }
        });

        jf.setSize(450, 950);

        jf.setVisible(true);
        
	}


	private void init() {
		jh = new JSONHelper();
		
		makeGUI();
		
		filters = new ArrayList<String>();
		
		Properties ps = new Properties();
		ps.put("iTopic", "/topic/isDump");
		ps.put("oTopic", "/topic/dummyOut");
		
		new Thread(this).start();
		
        GenericMiddlewareLoader gml = new GenericMiddlewareLoader("nl.utwente.hmi.middleware.stomp.STOMPMiddlewareLoader", ps);
        middleware = gml.load();

        middleware.addListener(this);
        
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if("".equals(filterBox.getText())){
			filters = new ArrayList<String>();
		} else {
			filters = Arrays.asList(filterBox.getText().split(","));
		}
	}


  	/**
	 * Callback method which is called by the Middleware when a new data package arrives
	 * @param d the recieved data
	 */
	public void receiveData(JsonNode jn)
    {
		addDataToQueue(jn);
	}
	
	@Override
	public void addDataToQueue(JsonNode jn) {
		queue.clear();
		queue.add(jn);
	}

	public String prettyPrintJsonString(JsonNode jsonNode) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        Object json = mapper.readValue(jsonNode.toString(), Object.class);
	        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
	    } catch (Exception e) {
	        return "Sorry, pretty print didn't work";
	    }
	}
	
	@Override
	public void processData(JsonNode jn) {

		logger.debug("incoming data{}", jn.toString());
    
		String output = "";
		
		//Search for all the defined filters and add to output string
		for(String filter : filters){
			JsonNode found = jh.searchKey(jn, filter);
			
			if(!found.isMissingNode()){
				output += prettyPrintJsonString(found)+"\n";
			}
		}
		
		//if no filters are defined, show everything instead
		if(filters.size() == 0){		
			output = prettyPrintJsonString(jn);
		}

		output = output.replaceAll(" ", "   ");
		output = output.replaceAll("  :  ", ":");
		
		if(output.equals("")){
			visualiser.setText("Nothing found for filters "+filters.toString());
		} else {
			
			visualiser.setText("");
			visualiser.setText(output);

			try {
				Thread.sleep(refreshTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	
	
}
