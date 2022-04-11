package starters;

import asap.bml.ext.bmlt.BMLTInfo;
import asap.environment.AsapEnvironment;
import asap.environment.AsapVirtualHuman;
import hmi.audioenvironment.AudioEnvironment;
import hmi.environmentbase.ClockDrivenCopyEnvironment;
import hmi.environmentbase.Environment;
import hmi.jcomponentenvironment.JComponentEnvironment;
import hmi.util.Console;
import hmi.util.SystemClock;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;
import saiba.bml.BMLInfo;
import saiba.bml.core.FaceLexemeBehaviour;
import saiba.bml.core.HeadBehaviour;
import saiba.bml.core.PostureShiftBehaviour;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Simple demo for the AsapRealizer+environment
 * @author dennisr
 * Edited for Harmony by @author bob
 */
public class HarmonyRealizerStarter{

    public HarmonyRealizerStarter(String spec) throws IOException{
        Console.setEnabled(false);

        BMLTInfo.init();
        BMLInfo.addCustomFloatAttribute(FaceLexemeBehaviour.class, "http://asap-project.org/convanim", "repetition");
        BMLInfo.addCustomStringAttribute(HeadBehaviour.class, "http://asap-project.org/convanim", "spindirection");
        BMLInfo.addCustomFloatAttribute(PostureShiftBehaviour.class, "http://asap-project.org/convanim", "amount");

        final AsapEnvironment ee = new AsapEnvironment();
        AudioEnvironment aue = new AudioEnvironment("LJWGL_JOAL");
        ClockDrivenCopyEnvironment ce = new ClockDrivenCopyEnvironment(1000 / 50);

        aue.init();
        ce.init();

        ArrayList<Environment> environments = new ArrayList<Environment>();
        environments.add(ee);
        environments.add(aue);
        environments.add(ce);

		// if no physics, use the following:
		SystemClock clock = new SystemClock(1000 / 100, "clock"); // the ideal frequency on which to run update messages depends on the robot and on the type of messages that you exchange... "play this animatino" does not typically require high frequency, but when you start to more or less directly control motors, you probably need much higher frequency.
		ee.init(environments, clock);
		clock.start();
		clock.addClockListener(ee);

        System.out.println("loading spec "+spec);
        AsapVirtualHuman zeno = ee.loadVirtualHuman("", spec, "AsapRealizer -- HMI Harmony Demo");

    }


    public static void main(String[] args) throws IOException{
    	String help = "Expecting commandline arguments in the form of \"-<argname> <arg>\".\nAccepting the following argnames: agentspec, middlewareprops";
    	
        String spec = "Prototype1/loaders/example_agentspec.xml";
    	String propFile = "Prototype1/config/defaultmiddleware.properties";

        if(args.length % 2 != 0){
        	System.err.println(help);
        	System.exit(0);
        }
        
        for(int i = 0; i < args.length; i = i + 2){
        	if(args[i].equals("-agentspec")){
        		spec = args[i+1];
        	} else if(args[i].equals("-middlewareprops")){
        		propFile = args[i+1];
        	} else {
            	System.err.println("Unknown commandline argument: \""+args[i]+" "+args[i+1]+"\".\n"+help);
            	System.exit(0);
        	}
        }
    	
		GenericMiddlewareLoader.setGlobalPropertiesFile(propFile);
		
        HarmonyRealizerStarter demo = new HarmonyRealizerStarter(spec);
    }
}
