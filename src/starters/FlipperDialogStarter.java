package starters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import hmi.flipper2.Config;
import hmi.flipper2.FlipperException;
import hmi.flipper2.launcher.FlipperLauncher;
import hmi.flipper2.launcher.FlipperLauncherThread;
import nl.utwente.hmi.middleware.Middleware;
import nl.utwente.hmi.middleware.helpers.JSONHelper;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class FlipperDialogStarter extends FlipperLauncherThread {

	final static Logger LOGGER = LoggerFactory.getLogger(FlipperDialogStarter.class.getName());

    private static FlipperDialogStarter launcher;

	private Middleware middleware;

	private ObjectMapper om;

	public FlipperDialogStarter(Properties ps) {
		super(ps);
		this.om = new ObjectMapper();
		String mwPropFile = "Prototype1/config/defaultmiddleware.properties";
		GenericMiddlewareLoader.setGlobalPropertiesFile(mwPropFile);
		
	}
	
	@Override
	public void LogIS(int stepMarker) {
		//don't want to log the IS when running full system during experiment
		
		try {
			if(middleware != null) {
				middleware.sendData(om.readTree(tc.getIs("is")));
			}
		} catch (IOException | FlipperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.LogIS(stepMarker);
		
	}


	private void initMW() {
		Properties ps = new Properties();
		ps.put("subscriber", "/dummy_in");
		ps.put("publisher", "/is_dump");

		// For loading Apache artemis middleware
		// GenericMiddlewareLoader gml = new GenericMiddlewareLoader("nl.utwente.hmi.middleware.stomp.STOMPMiddlewareLoader", ps);

		// For loading ROS1 middleware
		GenericMiddlewareLoader gml = new GenericMiddlewareLoader("nl.utwente.hmi.middleware.ros.ROSMiddlewareLoader", ps);
        middleware = gml.load();
	}
	
	public static void main(String[] args) {
		String help = "Expecting commandline arguments in the form of \"-<argname> <arg>\".\nAccepting the following argnames: config";
		String flipperPropFile = "Prototype1/config/flipper.properties";
		

		if(args.length % 2 != 0) {
			LOGGER.info(help);
			System.exit(0);
		}

		for(int i = 0; i < args.length; i = i + 2){
			if(args[i].equals("-config")) {
				flipperPropFile = args[i+1];
			} else {
				LOGGER.warn("Unknown commandline argument: \""+args[i]+" "+args[i+1]+"\".\n"+help);
				System.exit(0);
			}
		}

		Properties ps = new Properties();

		InputStream flipperPropStream = FlipperDialogStarter.class.getClassLoader().getResourceAsStream(flipperPropFile);

		try {
			LOGGER.info("Loading flipper config file: {}", flipperPropFile);
			ps.load(flipperPropStream);
		} catch (IOException ex) {
			LOGGER.warn("Could not load flipper settings from "+flipperPropFile);
			ex.printStackTrace();
		}

		// If you want to check templates based on events (i.e. messages on middleware),
		// you can run  flipperLauncherThread.forceCheck(); from a callback to force an immediate check.
		LOGGER.info("Starting Flipper thread");
		launcher = new FlipperDialogStarter(ps);
		launcher.initMW();
		launcher.start();
	}

}