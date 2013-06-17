// ExploreGrid.java
//
// Created: March 2007
//
// Copyright 2007 Martin Senger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.bioinfogrid.admin;

import org.bioinfogrid.services.GridJob;
import org.bioinfogrid.services.GridJobUtils;

import org.soaplab.clients.CmdLineHelper;
import org.soaplab.clients.InputUtils;
import org.soaplab.share.SoaplabException;
import org.soaplab.share.SoaplabMap;
import org.soaplab.services.AnalysisService;
import org.soaplab.services.JobFactory;
import org.soaplab.services.Reporter;
import org.soaplab.services.GenUtils;
import org.soaplab.services.Config;
import org.soaplab.services.metadata.MetadataAccessor;
import org.soaplab.services.cmdline.CmdLineJob;

import org.tulsoft.tools.BaseCmdLine;

import org.glite.wmsui.apij.Job;
import org.glite.wmsui.apij.JobId;
import org.glite.wmsui.apij.JobStatus;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.util.Map;
import java.util.Properties;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;

/**
 * A command-line tool doing various test for the bioinfogrid plugin
 * project. Start it with <tt>-help</tt> to see available options. <p>
 *
 * @author <A HREF="mailto:martin.senger@gmail.com">Martin Senger</A>
 * @version $Id: ExploreGrid.java,v 1.7 2007/10/31 15:12:15 marsenger Exp $
 */
public class ExploreGrid
    extends CmdLineHelper {

    AnalysisService service;
    static String inputJDL;
    static String outputJDL;

    /*************************************************************************
     *
     *************************************************************************/
    public ExploreGrid (String serviceName)
	throws SoaplabException {
	service = new AnalysisService (serviceName);
	MetadataAccessor metadataAccessor = service.getMetadataAccessor();
	metadataAccessor.getAnalysisDef().module = MyJobFactory.class.getName();
    }

    // here we get input data...
    public Map<String,Object>[] getInputSpec()
	throws SoaplabException {
	return SoaplabMap.toMaps (service.getInputSpec());
    }

    // here we create a job instance and ask it to create JDL
    // (possibly from my template 'myJDL'); return the created JDL
    public String processJDL (Map<String,Object> inputs, String myJDL)
	throws SoaplabException {
	this.inputJDL = myJDL;
	service.createAndRun (SoaplabMap.fromMap (inputs));
	return outputJDL;
    }

    // it must be static... (because of how ICreator works)
    public static class MyJobFactory
	implements JobFactory {

	public org.soaplab.services.Job newInstance (String jobId,
						     MetadataAccessor metadataAccessor,
						     Reporter reporter,
						     Map<String,Object> sharedAttributes,
						     boolean jobRecreated)
	    throws SoaplabException {
	    return new MyJob (jobId, metadataAccessor, reporter,
			      sharedAttributes, jobRecreated);
	}
    }

    public static class MyJob
	extends GridJob {

	public MyJob (String jobId,
		      MetadataAccessor metadataAccessor,
		      Reporter reporter,
		      Map<String,Object> sharedAttributes,
		      boolean jobRecreated)
	    throws SoaplabException {
	    super (jobId, metadataAccessor, reporter,
		   sharedAttributes, jobRecreated);
	}

	//
	// a real work is done here
	//
	public void run()
	    throws SoaplabException {

	    // input JDL file (or template)
	    if (inputJDL == null) {
		URL url = null;
		try {
		    url = GenUtils.locateFile (Config.getString (PROP_JDL_FILE,
								 DEFAULT_JDL_TEMPLATE,
								 getServiceName(), null));
		    inputJDL = 
			IOUtils.toString (new InputStreamReader (url.openStream()));

		} catch (FileNotFoundException e) {
		    throw new SoaplabException
			("Cannot find property with a JDL file for the service " +
			 getServiceName());
		} catch (IOException e) {
		    throw new SoaplabException
			("Reading from '" + url + "' failed: " +
			 e.getMessage());
		}
	    }

	    // process JDL file
	    outputJDL = processJDLTemplate (inputJDL);
	}
    }

    static class MyReporter extends Reporter {
	public String getDetails() {
	    return new String (reportProperties());
	}
    }

    /*************************************************************************
     *
     *  An entry point
     *
     *************************************************************************/
    public static void main (String[] args) {

	try {
	    BaseCmdLine cmd = getCmdLine (args, ExploreGrid.class);

	    // what can be on the command-line
 	    String jobid = cmd.getParam ("-job");
	    boolean veryVerbose = cmd.hasOption ("-vv");

	    boolean showSpec = cmd.hasOption ("-i");
	    boolean showMoreSpec = cmd.hasOption ("-ii");

	    String service = cmd.getParam ("-name");
	    boolean createJDL = cmd.hasOption ("-jdl");
	    String templateJDL = cmd.getParam ("-tjdl");
	    String ofileJDL = cmd.getParam ("-ojdl");
	    if (templateJDL != null || ofileJDL != null)
		createJDL = true;

	    // some checks
	    if (service == null &&
		(showSpec || showMoreSpec || createJDL)) {
		System.err.println ("With some options, a service name must be given. Use '-name'.");
		System.exit (1);
	    }

	    // enabling grid
	    if (jobid != null) {
		GridJobUtils.addGridConfiguration (service, null);
		Properties envProps = GridJobUtils.getGridEnvironment (service, null);
		if (GridJobUtils.checkGridEnvironment())
		    GridJobUtils.setGridEnvironment (envProps);
	    }

	    // show job status
	    if (jobid != null) {
 		Job job = new Job (new JobId (jobid));
		if (veryVerbose) {
		    System.out.println (job.getStatus().toString (2));
		} else {
		    MyReporter reporter = new MyReporter();
		    GridJobUtils.reportDetails ((JobStatus)job.getStatus().getResult(), reporter);
		    System.out.println ("Status of job " + jobid + ":");
		    System.out.print (reporter.getDetails());
		}
	    }
		

	    // explore a service
	    if (service != null) {
		System.setProperty ("service.name", service);

		ExploreGrid worker = new ExploreGrid (service);

		Map<String,Object>[] inputSpec = worker.getInputSpec();

		// collect data inputs from the command line
		Map<String,Object> inputs =	InputUtils.collectInputs (cmd, inputSpec);

		// show metadata
		if (showSpec || showMoreSpec) {
		    InputUtils.formatInputMetadata (inputSpec,
						    System.out,
						    showMoreSpec,
						    verbose);
		}

		if (createJDL) {
		    String jdl = worker.processJDL (inputs, templateJDL);
		    if (ofileJDL != null ) {
			File jdlFile = new File (ofileJDL);
			FileUtils.writeStringToFile (jdlFile, jdl,
						     System.getProperty ("file.encoding"));
			System.out.println ("JDL stored in: " + jdlFile.getAbsolutePath());
		    } else {
			System.out.println (jdl);
		    }
		}
	    }

	    System.exit (0);

	} catch (Exception e) {
	    System.err.println ("===ERROR===");
	    e.printStackTrace();
	    System.err.println ("===ERROR===");
	    System.exit (1);
	}
    }

}
