// GridJob.java
//
// Created: November 2006
//
// Copyright 2006 Martin Senger
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

package org.bioinfogrid.services;

import org.soaplab.share.SoaplabException;
import org.soaplab.services.cmdline.CmdLineJob;
import org.soaplab.services.cmdline.IOParameter;
import org.soaplab.services.cmdline.Parameter;
import org.soaplab.services.cmdline.ParameterException;
import org.soaplab.services.Config;
import org.soaplab.services.Reporter;
import org.soaplab.services.JobState;
import org.soaplab.services.GenUtils;
import org.soaplab.services.IOData;
import org.soaplab.services.metadata.MetadataAccessor;
import org.soaplab.services.metadata.IOParamDef;
import org.soaplab.services.events.HeartbeatProgressEvent;

import org.glite.wmsui.apij.Api;
import org.glite.wmsui.apij.Url;
import org.glite.wmsui.apij.JobCollection;
import org.glite.wmsui.apij.Job;
import org.glite.wmsui.apij.JobId;
import org.glite.wmsui.apij.JobStatus;
import org.glite.wmsui.apij.Result;
import org.glite.jdl.JobAd;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A Soaplab's plugin to submit and monitor a job on a grid. <p>
 *
 * @author <A HREF="mailto:martin.senger@gmail.com">Martin Senger</A>
 * @version $Id: GridJob.java,v 1.17 2007/10/31 15:12:15 marsenger Exp $
 */

public class GridJob
    extends CmdLineJob
    implements BioinfogridConstants {

    private static org.apache.commons.logging.Log log =
       org.apache.commons.logging.LogFactory.getLog (GridJob.class);

    // names of GridJob-specific properties
    protected static final String JOB_PROP_GRID_JOB_ID = "grid.job.id";

    // constants used for generating JDL files form templates
    protected static final int TOKEN_GRID_VO         = 1;
    protected static final int TOKEN_EXECUTABLE      = 2;
    protected static final int TOKEN_INPUT_SANDBOX   = 3;
    protected static final int TOKEN_OUTPUT_SANDBOX  = 4;
    protected static final int TOKEN_CMD_ARGS        = 5;
    protected static final int TOKEN_ENVS            = 6;

    protected static Map<String, Integer> knownTokens =
	new HashMap<String, Integer>();
    static {
	knownTokens.put ("GRID_VO",        new Integer (TOKEN_GRID_VO));
	knownTokens.put ("EXECUTABLE",     new Integer (TOKEN_EXECUTABLE));
	knownTokens.put ("INPUT_SANDBOX",  new Integer (TOKEN_INPUT_SANDBOX));
	knownTokens.put ("OUTPUT_SANDBOX", new Integer (TOKEN_OUTPUT_SANDBOX));
	knownTokens.put ("CMD_ARGS",       new Integer (TOKEN_CMD_ARGS));
	knownTokens.put ("ENVS",           new Integer (TOKEN_ENVS));
    }

    protected static final String TOKEN_SEPARATOR = "@";


    // names of the files where output standard streams will be stored
    // (these names must correspond with the names in the JDL template
    // - this class does not put them there, it expects them to be
    // there hardcoded)
    protected static final String STDOUT_SANDBOX_NAME = "STDOUT.FILE";
    protected static final String STDERR_SANDBOX_NAME = "STDERR.FILE";


    // a filename with this plugin properties
    protected static String configFilename;

    // a directory for all jobs files
    protected static File jobsDir;

    // control the init() method
    protected static boolean jobClassInitialized = false;

    // global variables for this job instance
    protected File jobDir;       // directory for this job
    protected File resultsDir;   // directory for this job's results
    protected String gridJobId;  // grid job ID (obtained from job submission)

    List<String> args;
    IOData[] ioData;


    /**************************************************************************
     * The main constructor.
     **************************************************************************/
    public GridJob (String jobId,
		    MetadataAccessor metadataAccessor,
		    Reporter reporter,
		    Map<String,Object> sharedAttributes,
		    boolean jobRecreated)
	throws SoaplabException {
	super (jobId, metadataAccessor, reporter,
	       sharedAttributes, jobRecreated);
    }

    /**************************************************************************
     * Do it only once, for all GridJob instances:
     *
     * Change IOParameters for our own instances.
     * Add configuration specific for this plugin.
     * Create super-directory for all jobs files.
     * Check how to set the grid environment.
     **************************************************************************/
    protected synchronized void init()
	throws SoaplabException {
	super.init();

	if (jobClassInitialized)
	    return;
	jobClassInitialized = true;

	// is the grid environment set?
	if (! GridJobUtils.checkGridEnvironment()) {
	    internalError
		("GRID environent is not set. Or the certificate expired. Or anything else GRID related.");
	}

	// fill (create and set) the grid environment
 	Properties envProps = getGridEnvironment();
	GridJobUtils.setGridEnvironment (envProps);

	// change IOParameters for our own instances
	Parameter[] parameters = (Parameter[])sharedAttributes.get (JOB_SHARED_PARAMETERS);
	if (parameters != null) {
	    for (int i = 0; i < parameters.length; i++) {
		if (parameters[i] instanceof IOParameter) {
		    parameters[i] =
			new GridIOParameter ((IOParamDef)parameters[i].getDefinition());
		}
	    }
	}

	// add our own configuration
	if (configFilename == null) {
	    try {
		configFilename =
		    GridJobUtils.addGridConfiguration (getServiceName(), this);
	    } catch (ConfigurationException e) {
		internalError (e.getMessage());
	    }
 	}

	// create directory for all jobs files
	if (jobsDir == null) {
	    String jobsDirName = Config.getString (PROP_GRID_JOBS_DIR,
						   null,
						   getServiceName(),
						   this);
	    if (jobsDirName == null) {
		jobsDir = new File (System.getProperty ("java.io.tmpdir"),
				    DEFAULT_JOBS_DIR);
	    } else {
		jobsDir = new File (jobsDirName);
	    }
	    if (! jobsDir.exists())
		jobsDir.mkdirs();
	    if (! jobsDir.exists())
		internalError ("Cannot create JOBS directory '" +
			       jobsDir.getAbsolutePath() + "'.");
	    if (! jobsDir.isDirectory())
		internalError ("JOBS directory '" +
			       jobsDir.getAbsolutePath() +
			       "' is not really a directory.");
	    log.info ("Using JOBS directory: " + jobsDir.getAbsolutePath());
	}
    }

    /**************************************************************************
     * "Our" own IOParameter. The only difference from the normal one
     * (provided by Soaplab2) is that it creates names of the
     * input/output files on the command-line parameters without
     * absolute path (as needed for use in JDL files).
     **************************************************************************/
    class GridIOParameter extends IOParameter {
	public GridIOParameter (IOParamDef paramDef) {
	    super (paramDef);
	}

	protected String addToIOData (boolean isDirect,
				      Object data,
				      org.soaplab.services.Job job,
				      String id)
	    throws ParameterException {
	    String ref = super.addToIOData (isDirect, data, job, id);
	    if (ref != null)
		return new File (ref).getName();
	    return ref;
	}

    }

    /**************************************************************************
     * It is almost identical to the run() method in the superclass -
     * except this one does not report that a job is completed (it is
     * the StateWatchingThread who does that).
     **************************************************************************/
    public void run()
	throws SoaplabException {
	if (runAllowed() && ! jobRecreated) {
	    checkInputs();
	    reporter.getState().set (JobState.RUNNING);
	    realRun();
	}
    }

    /**************************************************************************
     * Terminate this running job.
     **************************************************************************/
    public void terminate()
	throws SoaplabException {

	if (gridJobId == null) {

	    // it may happen when the job was re-created from the
	    // persistent storage... (so find its GRID's Id)
	    Properties jobProps = reporter.getJobProperties();
	    gridJobId = jobProps.getProperty (JOB_PROP_GRID_JOB_ID);
	    if (gridJobId == null) {

		// ...or, when this was called too early (in which
		// case follow the request and pronounce it terminated
		reporter.getState().set (JobState.TERMINATED_BY_REQUEST);
		return;
	    }
	}

	// otherwise, make a proper grid cancellation
	try {
	    Job job = new Job (new JobId (gridJobId));
	    Result cancelResult = job.cancel();
	    int code = cancelResult.getCode();

	    if (code < 10) {
		// success: means that a request to cancel was
		// successfully submitted - it does not mean that it
		// is already cancelled
		if (log.isDebugEnabled())
		    log.debug (StringUtils.stripEnd (cancelResult.toString (2), null));

		// TBD: I am not sure about this - but at least this
		// job will be freed and not wasting resources even if
		// the grid does not make the cancelation...
		reporter.getState().set (JobState.TERMINATED_BY_REQUEST);

	    } else {
		// cancel 'manually'
		reporter.getState().set (JobState.TERMINATED_BY_REQUEST);
	    }
	} catch (Exception e) {
	    // ignore errors - they may be because 'terminate' was
	    // called several times before the grid job cancellation
	    // took place - and that causes error (which we ignore
	    // here) - but keep it at least in the DEBUG log
	    log.debug ("in terminate(): " + e.toString());
	}
    }


    /**************************************************************************
     *
     **************************************************************************/
    protected void realRun()
	throws SoaplabException {
	initialize();

	// fill the job directory
	File jdlFile = createJDL();

	// submit our job to the grid; save its ID
	gridJobId = submit (jdlFile);

	// store GRID job id (for cases of job re-creation)
	Properties props = reporter.getJobProperties();
	props.put (JOB_PROP_GRID_JOB_ID, gridJobId);
	reporter.setJobProperties (props);

	// check and set status
	Thread stateWatching = getStateWatchingThread();
	stateWatching.setName (stateWatching.getName().replace ("Thread", "GridWatcher"));
	stateWatching.start();

    }

    /**************************************************************************
     * Return a thread that monitors a submitted job until it is
     * completed. The core is done in method 'processJobStatus' - look
     * there for details if you wish to overwrite the watching thread.
     **************************************************************************/
    protected Thread getStateWatchingThread() {
	return new StateWatchingThread();
    }

    /**
     * It cannot report back any errors found when waiting for a job
     * completion. Therefore, it puts them into the 'report' result as
     * errors. <p>
     *
     * It polls for the job status (how often, it depends on several
     * properties). It ends when:
     *    a) an error (retrieving status) occurs, or
     *    b) the reporter reports a job completion, or
     *    c) a timeout occurs (if timeout is set).
     */
    protected class StateWatchingThread
	extends Thread {

	int pollingInterval;
	int maxPollingInterval;

	// special timeout for job cancelling
	boolean inCancelling = false;
	long cancellingTimeout = 10 * 60 * 1000;   // 10 minutes

	protected StateWatchingThread() {
	    pollingInterval = Config.getInt (PROP_GRID_POLLING_INTERVAL,
					     DEFAULT_POLLING_INTERVAL,
					     getServiceName(), this);
	    if (pollingInterval <= 0)
		pollingInterval = DEFAULT_POLLING_INTERVAL;

	    maxPollingInterval = Config.getInt (PROP_GRID_MAX_POLLING_INTERVAL,
					     DEFAULT_MAX_POLLING_INTERVAL,
					     getServiceName(), this);
	    if (maxPollingInterval < pollingInterval)
		maxPollingInterval =  DEFAULT_MAX_POLLING_INTERVAL;
	}

	public void run() {
	    long started = System.currentTimeMillis();
	    int pollingCount = 10;
	    while (! reporter.getState().isCompleted()) {
		processJobStatus();

		if (inCancelling) {
		    if (System.currentTimeMillis() > cancellingTimeout) {
			// even cancelling timeouted
			reporter.getState().set (JobState.TERMINATED_BY_REQUEST, "-4");
			processJobStatus();
			endOfWatching();
			return;
		    }

		} else {
		    long remainingTime =
			getRemainingTimeout (System.currentTimeMillis() - started);
		    if (remainingTime < 0) {
			// timeout: we call 'terminate' but it can take some
			// time and we do not want to call 'terminate' again -
			// therefore we create a new, 'hidden', timeout ('cancellingTimeout'),
			// just to make sure that cancelling will not take
			// infinitive time
			reporter.error ("Soaplab lost patience and ordered TIMEOUT...");
			try {
			    terminate();
			} catch (SoaplabException e) {
			    // our 'terminate' does not produce any exception
			}
			cancellingTimeout += System.currentTimeMillis();
			inCancelling = true;
		    }
		}

		// sleep for a while
		if (pollingCount-- <= 0) {
		    pollingInterval *= 2;
		    if (pollingInterval > maxPollingInterval)
			pollingInterval = maxPollingInterval;
		    pollingCount = 10;
		}
		synchronized (reporter) {
		    try { reporter.wait (pollingInterval * 1000); }
		    catch (Exception e) { }
		}
	    }
	    endOfWatching();
	}

	// this could be actually a finalize() method - but I have
	// never used it, and I do not want to rely on garbage
	// collector to invoke it - but perhaps I am wrong and should
	// use finalize()
	protected void endOfWatching() {
	    try {
		// create informative results
		reporter.setDetailedStatusResult();
		reporter.setReportResult();

		// inform other threads waiting for the termination that
		// it has been done
		synchronized (reporter) {
		    reporter.notifyAll();
		}
	    } catch (Throwable e) {
		log.error ("Trouble in EndOfWatching: " + e.getMessage());
	    }
	}

    }

    /**************************************************************************
     * Return a timeout (in millis) that should be maximally spent
     * waiting for a job completion. The method gets parameter
     * 'alreadySpent' showing how much time (in millis) was already
     * spent on the job. At the beginning, during the first call) it
     * is usually zero, and it gradually inreases. <p>
     *
     * In a simple scenario (implemented here), the method takes a
     * timeout from a property {@link
     * BioinfogridConstants#PROP_GRID_JOB_TIMEOUT
     * PROP_GRID_JOB_TIMEOUT}, decreases it by 'alreadySpent' and
     * returns the result. <p>
     *
     * But there can be another implementation that actually checks
     * how much time is remaining for the current user-certificate
     * proxy, and adjust returned timeout accordingly. <p>
     *
     * Returning zero means no timeout (infinitive waiting). Returning
     * negative number means that timeout is exhauset (expired).
     **************************************************************************/
    protected long getRemainingTimeout (long alreadySpent) {

	int timeout = Config.getInt (PROP_GRID_JOB_TIMEOUT,
				     DEFAULT_JOB_TIMEOUT,
				     getServiceName(), this);
	if (timeout == 0)
	    return 0;
	if (timeout < 0)
	    timeout = DEFAULT_JOB_TIMEOUT;
	return ( (timeout * 1000) - alreadySpent);
    }

    /**************************************************************************
     * Initializing a job.
     **************************************************************************/
    protected synchronized void initialize()
	throws SoaplabException {

	// each job has its own directory
	jobDir = new File (jobsDir, id);
	if (! jobDir.mkdir())
	    internalError ("Cannot create JOB directory '" +
			   jobDir.getAbsolutePath() + "'.");

	// ...and this directory has a subdirectory for results
	resultsDir = new File (jobDir, STANDARD_GRID_RESULTS_DIR);
	if (! resultsDir.mkdir())
	    internalError ("Cannot create JOB RESULTS directory '" +
			   resultsDir.getAbsolutePath() + "'.");
	
    }

    /**************************************************************************
     * Create properties that will be turned into environment
     * variables later. They constitute a working grid environment. <p>
     *
     * Do not include properties that already exist as real
     * environment variables. Otherwise, take their values first from
     * the configuration, and - if still not found - use some default
     * values. <p>
     **************************************************************************/
    protected Properties getGridEnvironment() {
	return GridJobUtils.getGridEnvironment (getServiceName(), this);

    }

    /**************************************************************************
     * Return a base filename (no path attached) with the
     * user-certificate proxy. <p>
     *
     * It uses several configuration properties to create a different
     * file name for different virtual organizations and different
     * users.
     **************************************************************************/
    protected String getProxyFileName() {
	return GridJobUtils.getProxyFileName (getServiceName(), this);

    }

    /**************************************************************************
     * Create a JDL file in directory 'jobDir' (a global), and return
     * its full name (an absolute path).
     *
     * It does it by copying a file defined in a property
     * PROP_JDL_FILE and substituting there all tokens (so the file
     * can be either a regular JDL file - without any tokens - or its
     * template) using other properties and user real input data.
     *
     * Throw an exception if no JDL file/template can be found/read,
     * or processed. This should be considered an internal error (TBD:
     * or should we report here also invalid user input data?)
     **************************************************************************/
    protected File createJDL()
	throws SoaplabException {

	// find JDL file (or template)
	URL url = null;
	try {
	    url = GenUtils.locateFile (Config.getString (PROP_JDL_FILE,
							 DEFAULT_JDL_TEMPLATE,
							 getServiceName(), this));
	} catch (FileNotFoundException e) {
	    internalError ("Cannot find property with a JDL file for the service " +
			   getServiceName());
	}

	// process JDL file
	File result = new File (jobDir, STANDARD_JDL_FILENAME);
	try {
	    String jdlTemplate = 
		IOUtils.toString (new InputStreamReader (url.openStream()));
	    String jdl = processJDLTemplate (jdlTemplate);
	    FileUtils.writeStringToFile (result, jdl,
					 System.getProperty ("file.encoding"));
	    log.debug ("Created JDL file " + result.getAbsolutePath());
	    return result;
	} catch (IOException e) {
	    internalError ("Copying from '" + url + "' to '" +
			   result.getAbsolutePath() + "' failed: " +
			   e.getMessage());
	    return null;    // never comes here
	}
    }

    /**************************************************************************
     * Substitute recognized tokens in 'jdl' and return its new
     * version (or the same 'jdl' if no substitution done).
     *
     * Do we need any optimalization for parsing the JDL string?
     * Probably not, considering how long any Grid request takes
     * anyway.
     **************************************************************************/
    protected synchronized String processJDLTemplate (String jdl)
	throws SoaplabException {

	// return unchanged what is definitely not a template
	if (jdl.indexOf (TOKEN_SEPARATOR) < 0)
	    return jdl;

	// fill some globals 
	args = createArgs();
	ioData = createIO();

	StringBuffer buf = new StringBuffer();
	StringTokenizer st = new StringTokenizer (jdl, TOKEN_SEPARATOR, true);
	boolean inToken = false;
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    if (token.equals (TOKEN_SEPARATOR)) {
		inToken = ! inToken;
	    } else {
		buf.append (inToken ? substitute (token) : token);
	    } 
	}

	return buf.toString();
    }

    /**************************************************************************
     * The core of the token substitution (in JDL files).
     **************************************************************************/
    String substitute (String token)
	throws SoaplabException {

	Integer idx = knownTokens.get (token);

	// unknown token...
	if (idx == null) {
	    // ...try its name as a property name (case-insensitive)
	    String replacement = Config.getString (token,
						   null,
						   getServiceName(), this);
	    if (replacement == null)
		replacement = Config.getString (token.toLowerCase(),
						null,
						getServiceName(), this);
	    if (replacement == null) {

		// still not found: return unchanged (including token separators)
		return TOKEN_SEPARATOR + token + TOKEN_SEPARATOR;
	    } else {
		return replacement;
	    }
	}

	//
	// process the known token
	//
	switch (idx.intValue()) {

	case TOKEN_GRID_VO:
	    return Config.getString (PROP_GRID_VO, DEFAULT_GRID_VO, getServiceName(), this);

	case TOKEN_EXECUTABLE:
	    return new File (findExecutable (false)).getName();   // using just basename

	case TOKEN_INPUT_SANDBOX:
	    List<String> iList = new ArrayList<String>();
	    iList.add (quoted (findExecutable (true)));
	    for (IOData io: ioData) {
		IOParamDef def = io.getDefinition();
		if (def.isInput()) {
		    if (def.isRegularInput()) {
			iList.add (quoted (io.getReference()));
		    } else {
			log.warn ("Standard input is not supported. Ignored.");
		    }
		}
	    }
	    return StringUtils.join (iList.toArray (new String[] {}), ", ");

	case TOKEN_OUTPUT_SANDBOX:
	    List<String> oList = new ArrayList<String>();
	    oList.add (quoted (STDOUT_SANDBOX_NAME));
	    oList.add (quoted (STDERR_SANDBOX_NAME));
	    for (IOData io: ioData) {
		IOParamDef def = io.getDefinition();
		if (def.isRegularOutput()) {
		    oList.add (quoted (new File (io.getReference()).getName()));
		}
	    }
	    return StringUtils.join (oList.toArray (new String[] {}), ", ");

	case TOKEN_CMD_ARGS:
	    StringBuffer buf = new StringBuffer();
	    for (Iterator<String> it = args.iterator(); it.hasNext(); ) {
 		buf.append (escape (it.next()));
		buf.append (' ');
	    }
	    return new String (buf).trim();

	case TOKEN_ENVS:
	    // two sources of environmental variables (from user and from config)
	    Properties envProps = createEnvs();
	    Properties cfgProps =
		Config.getMatchingProperties (PROP_GRID_ENVAR, getServiceName(), this);
	    // ...merge the two sources (with priority on config - okay?)
	    for (Enumeration en = cfgProps.propertyNames(); en.hasMoreElements(); ) {
		String key = (String)en.nextElement();
		envProps.put (key, cfgProps.get (key));
	    }

	    List<String> pList = new ArrayList<String>();
	    for (Enumeration en = envProps.propertyNames(); en.hasMoreElements(); ) {
		String key = (String)en.nextElement();
  		pList.add (quoted (key + "=" + escape ((String)envProps.get (key))));
//  		pList.add (quoted (key + "=" + envProps.get (key)));
	    }
	    return StringUtils.join (pList.toArray (new String[] {}), ", ");

	default:
	    // we forgot to add here a new case/token I guess
	    return TOKEN_SEPARATOR + token + TOKEN_SEPARATOR;
	}

    }

    /**************************************************************************
     * Replace shell metacharacters, as requested by JDL specification.
     * <pre>
     *	 & ; ` ' \ " | * ? ~ &lt; &gt; ^ ( ) [ ] { } $ !
     * </pre>
     *
     * Actually, JDL spec asks for escaping by triple backslashes -
     * but it does not work on the GRID nodes. So I put there only two
     * backsleshes, and, surprise, surprise, it works. Bloody glite!
     **************************************************************************/
    private String escape (String str) {
	return str
	    .replace ('`', ' ')
// 	    .replaceAll ("([ &;'|*?!~<>()\\[\\]{}\\^\\$\\\"])", "\\\\\\\\\\\\$1");
	    .replaceAll ("([ &;'|*?!~<>()\\[\\]{}\\^\\$\\\"])", "\\\\\\\\$1");
    }

    /**************************************************************************
     * Return given string double quoted. No enquiries are made
     * whether the string contain quotes inside.
     **************************************************************************/
    private String quoted (String str) {
	return "\"" + str + "\"";
    }

    /**************************************************************************
     * Return a name of executable (still doing substitutions in JDL
     * template). The executable is found using these rules:
     *
     * 1) Take executable from a configuration file, or from the
     * metadata (in this order).
     *
     * 2) If it is absolute, return it. End of story.
     *
     * 3) Look if such file can be found in the PROP_SCRIPTS_DIR
     * (which is another property), or use method 'locateFile'. If
     * found, return its absolute path.
     *
     * 4) Otherwise return just what you have.
     *
     * @param verbose if true complain (as a warning) if the found
     * executable does not have absolute path (the parameter is here
     * only because this method is called twice during processing one
     * JDL file, and we need the possible warning only once)
     **************************************************************************/
    protected String findExecutable (boolean verbose) {
	String prgName =
	    Config.getString (PROP_EXECUTABLE, null, getServiceName(), this);
	if (prgName == null) {
	    try {
		prgName = metadataAccessor.getAnalysisDef().file;
	    } catch (Exception e) {
		log.error ("When looking for an executable in metadata: " + e.toString());
		prgName = "/bin/echo";   // at least something...
	    }
	}
	File file = new File (prgName);
	if (file.isAbsolute())
	    return prgName;
	String prgBase =
	    Config.getString (PROP_SCRIPTS_DIR, null, getServiceName(), this);
	if (prgBase != null) {
	    StringBuffer buf = new StringBuffer();
	    buf.append (prgBase);
	    if (! prgBase.endsWith (File.separator))
		buf.append (File.separator);
	    buf.append (prgName);
	    if (new File (buf.toString()).exists())
		return buf.toString();
	}
	try {
	    String url = GenUtils.locateFile (prgName).toString();
	    if (url.startsWith ("file:"))
		return url.substring (5);
	    else
		return url;   // what *that* would be?
	} catch (FileNotFoundException e) {
	    if (verbose)
		log.warn ("Executable '" + prgName + "' does not have absolute path.");
	    return prgName;
	}
    }


    /**************************************************************************
     * Submit a job to the Grid.
     **************************************************************************/
    protected String submit (File jdlFile)
	throws SoaplabException {

	// prepare brokers adresses
	String nsAddress = Config.getString (PROP_GRID_NSADDRESS,
					     DEFAULT_NSADDRESS,
					     getServiceName(), this);
	String[] lbAddresses = Config.getStrings (PROP_GRID_LBADDRESS,
						  DEFAULT_LBADDRESS,
						  getServiceName(), this);
	Url ns = null;
	Vector<Url> lbs = new Vector<Url>();
	try {
	    ns = new Url (nsAddress);
	    for (String lb: lbAddresses)
		lbs.add (new Url (lb));

	} catch (java.lang.NumberFormatException e) {
	    internalError ("Not a numeric port number in properties " +
			   PROP_GRID_NSADDRESS +
			   " or " + PROP_GRID_LBADDRESS);
	} catch (java.lang.IllegalArgumentException e) {
	    internalError (e.getMessage());
	}

	// dedicated computing element?
	String ce = Config.getString (PROP_GRID_CE,
				      null,   // no default
				      getServiceName(), this);

	//
	JobAd jobAd = new JobAd();
	Vector submitResult = null;
	try {
	    // job description (JDL)
	    jobAd.fromFile (jdlFile.getAbsolutePath());

	    // prepare a one-element job collection
	    JobCollection collect = new JobCollection();
	    collect.insertAd (jobAd);
	    collect.setLoggerLevel (Config.getInt (PROP_GRID_LOGGER_LEVEL,
						   3, getServiceName(), this));
// 	    if (log.isDebugEnabled()) {
// 		collect.logDefaultValues (true);  // TBD: what's that?
// 	    }

	    // finally, submitting
 	    submitResult = collect.submit (ns, lbs, ce);

	} catch (Exception e) {
	    internalError (e.getMessage());
	}

	// evaluate, store and return grid job ID
	return processSubmitResult (submitResult);
    }

    /**************************************************************************
     * Check result returned from submitting operation (note that
     * 'submitResult' does not contain the real 'data' result, but
     * only the result of submitting; it's kind of a grid job
     * handler).
     * 
     * On success, store grid job ID into a file (into 'jobDir'
     * directory) and also return it. Otherwise report error and raise
     * exception.
     **************************************************************************/
    protected String processSubmitResult (Vector submitResult)
	throws SoaplabException {
	if (submitResult == null || submitResult.size() == 0)
	    internalError ("Lost result during submission.");
	if (submitResult.size() > 1)
	    log.warn ("Unexpected: Too many results in submission.");
	Result result = (Result)submitResult.elementAt (0);

	int code = result.getCode();

	// success
	if (code < 10) {
	    if (log.isDebugEnabled())
		log.debug (StringUtils.stripEnd (result.toString (2), null));

	    // ...store grid job ID to a file (for manual enquiries)
	    String gJobId = result.getId();
	    File idFile = new File (jobDir, STANDARD_JOBID_FILENAME);
	    try {
		FileUtils.writeStringToFile (idFile,
					     gJobId,
					     System.getProperty ("file.encoding"));
	    } catch (IOException e) {
		internalError ("Cannot write to " +
			       idFile.getAbsolutePath() +
			       ": " + e.getMessage());
	    }

	    // ...and log it, as well
	    // (I expect the manual enquiries will be needed often :-( )
	    log.info ("GRID job submitted: " + gJobId);
	    return gJobId;
	}

	// failure
	internalError (result.toString (2) + " [Code: " + code + "]");
	return null;   // never comes here
    }

    /**************************************************************************
     * Find the status of a grid job identified by given 'gridJobId',
     * and pass it (perhaps adjusted) to the Soaplab's reporter. This
     * is quite important because other methods regularly check the
     * status in the reporter, and may change their behaviour
     * according to it. <p>
     *
     * If the status cannot be retrieved, report it but continue. <p>
     *
     * If the grid status indicates that the grid job was completed,
     * change the Soaplab's minor state description to "Retrieving
     * results" but keep the major state still RUNNING. Then invoke a
     * result retriever that will bring job results from grid
     * here. Only after that, change the major Soaplab's state to
     * COMPLETED (or TERMINATED_BY_ERROR). Be also carefull that
     * meantime the whole job might have been
     * TERMINATED_BY_REQUEST. <p>
     *
     * When a job is completed, report also status details, and exit
     * code. <p>
     *
     **************************************************************************/
    protected void processJobStatus() {

	Result result = null;
	try {
	    Job job = new Job (new JobId (gridJobId));
	    result = job.getStatus();
	} catch (Exception e) {
	    log.warn ("Cannot retrieve status for " + gridJobId + ": " + e.toString() +
		      ". I continue trying...");
	    return;
	}

	int code = result.getCode();
	if (code >= 10) {

	    // cannot get job status
	    reporter.getState().set (JobState.TERMINATED_BY_ERROR, "-99");

	    Object r = result.getResult();
	    if (r == null)
		log.error ("Lost job status for: " + gridJobId);
	    else if (r instanceof Exception)
		log.error ( ((Exception)r).getMessage() + ": " + gridJobId);
	    else
		log.error (r.toString() + ": " + gridJobId);
	    return;

	} else {

	    // job status retrieved successfully - pass it to Reporter
	    JobStatus status = (JobStatus)result.getResult();
	    String statusName = StringUtils.strip (status.name());
	    reporter.sendEvent (new HeartbeatProgressEvent (id, statusName));
	    switch (status.code()) {
	    case JobStatus.ABORTED:
		reportDetails (status);
		reporter.getState().setDescription (statusName);
		reporter.getState().set (JobState.TERMINATED_BY_ERROR, "-2");
		break;
	    case JobStatus.CANCELLED:
		reportDetails (status);
		reporter.getState().setDescription (statusName);
		reporter.getState().set (JobState.TERMINATED_BY_REQUEST, "-2");
		break;
	    case JobStatus.CLEARED:
	    case JobStatus.PURGED:
		break;   // should I do anything here?
	    case JobStatus.DONE:
		reportDetails (status);
		reporter.getState().setDescription (statusName);
		switch (status.getValInt (JobStatus.DONE_CODE)) {
		case 1:  // DONE: failed
		    reporter.getState().set (JobState.TERMINATED_BY_ERROR, "-1");
		    break;
		case 2:  // DONE: cancelled
		    reporter.getState().set (JobState.TERMINATED_BY_REQUEST, "-1");
		    break;
		default: // DONE: ok
		    retrieveResults();
		    reporter.getState().set (JobState.COMPLETED,
					     "" + status.getValInt (JobStatus.EXIT_CODE));
		}
		break;
	    default:
		reporter.getState().setDescription (statusName);
	    }
	    
	}
    }

    /**************************************************************************
     *
     **************************************************************************/
    public String getStatus() {

	// usual case: do the same as in the parent class
	if (! jobRecreated)
	    return reporter.getState().getAsString();

	try {

	    // try to retrieve a GRID job id for this job
	    if (gridJobId == null) {
		Properties jobProps = reporter.getJobProperties();
		gridJobId = jobProps.getProperty (JOB_PROP_GRID_JOB_ID);
		if (gridJobId == null) {
		    // what else can I do?
		    return reporter.getState().getAsString();
		}
	    }

	    // ...and get the real GRID job status
	    processJobStatus();
	    if (reporter.getState().isCompleted()) {
		// create informative results
		reporter.setDetailedStatusResult();
		reporter.setReportResult();
	    }

	} catch (Throwable e) {
	    log.error ("Trouble in getStatus() for a re-created job: " + e.getMessage());
	}
	return reporter.getState().getAsString();
    }

    /**************************************************************************
     * Get details from the given 'status', select some, potentially
     * adjust them to be more readable, and pass them to the Reporter.
     **************************************************************************/
    protected void reportDetails (JobStatus status) {
	GridJobUtils.reportDetails (status, reporter);
    }

    /**************************************************************************
     *
     **************************************************************************/
    protected void retrieveResults() {
	try {
	    Job job = new Job (new JobId (gridJobId));
	    Result outputResult = job.getOutput (resultsDir.getAbsolutePath());
	    if (outputResult == null)
		throw new SoaplabException ("Returned Result is null.");
	    int code = outputResult.getCode();

	    if (code < 10) {
		// success
		if (log.isDebugEnabled()) {
		    log.debug (StringUtils.stripEnd (outputResult.toString (2), null));
		}

		// copy results to persistent storage
		File result = null;
		boolean stdoutPending = true;
		boolean stderrPending = true;
		IOData[] ioData = createIO();
		for (IOData io: ioData) {
		    IOParamDef def = io.getDefinition();
		    if (def.isInput())
			continue;
		    result = null;
		    if (def.isRegularOutput()) {
			result = new File (resultsDir, new File (io.getReference()).getName());
		    } else {
			if (def.isStdout()) {
			    result = new File (resultsDir, STDOUT_SANDBOX_NAME);
			    stdoutPending = false;
			} else {
			    result = new File (resultsDir, STDERR_SANDBOX_NAME);
			    stderrPending = false;
			}
		    }
		    if (result != null && result.exists())
			reporter.setResult (def.id, result);
		}
		// if standard streams were not copied (meaning they
		// were not explicitely defined in the service
		// metadata), put them into the report
		if (stdoutPending) {
		    result = new File (resultsDir, STDOUT_SANDBOX_NAME);
		    if (result.exists() && result.length() > 0)
			reporter.report ("Standard output:", result);
		}
		if (stderrPending) {
		    result = new File (resultsDir, STDERR_SANDBOX_NAME);
		    if (result.exists() && result.length() > 0)
			reporter.report ("Standard error:", result);
		}

	    } else {
		log.error ("Cannot get results for " + gridJobId + ". Code: " + code);
	    }
	} catch (Exception e) {
	    reporter.error ("Error when retrieving results: " + e.toString());
	    SoaplabException.formatAndLog (e, log);
	}
    }

}
