// GridJobUtils.java
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

import org.soaplab.services.Config;
import org.soaplab.services.Reporter;
import org.soaplab.services.JobState;

import org.glite.wmsui.apij.Api;
import org.glite.wmsui.apij.JobStatus;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.io.IOException;
import java.io.File;

/**
 * Several utilities used by GridJob. Some of them are copied here
 * from the package <tt>org.glite.wmsui.guij</tt>. Their copyright
 * message is:
 *<pre>
 * Copyright (c) Members of the EGEE Collaboration. 2004.
 * See http://public.eu-egee.org/partners/ for details on the copyright holders.
 * For license conditions see the license file or http://www.eu-egee.org/license.html
 *</pre>
 *
 * @author <A HREF="mailto:martin.senger@gmail.com">Martin Senger</A>
 * @version $Id: GridJobUtils.java,v 1.6 2007/04/17 12:25:41 p-ernst-dkfz Exp $
 */
public abstract class GridJobUtils
    implements BioinfogridConstants {

    private static org.apache.commons.logging.Log log =
       org.apache.commons.logging.LogFactory.getLog (GridJobUtils.class);

    /**************************************************************************
     * Convert the 'eventTime' from the format
     * "[0123456789].[0123456789] s" into a more human readable
     * format. <p>
     *
     * If the 'eventTime' is in a different format, it is returned
     * unchanged. If the 'eventTime' is null, an emty string is
     * returned.
     **************************************************************************/
    public static String toTime (String eventTime) {
	if (eventTime == null)
	    return "";
	int end = eventTime.indexOf (" ");
	if (end == -1)
	    return eventTime;

	eventTime = eventTime.substring (0, end);
	try {
	    // Time returned from LB is divided by 1000.
	    double time =
		Double.parseDouble (eventTime.substring (0, end)) * 1000;
	    Calendar cl = new GregorianCalendar();
	    cl.setTimeInMillis ((long) time);
	    return cl.getTime().toString();
	} catch (NumberFormatException exc) {
	    return eventTime;
	}
    }

    /**************************************************************************
     * Find a configuration file (using 'service' and possibly 'owner'
     * for that), make its copy to a temporary file (because of the
     * COG properties) and return it. <p>
     *
     * @return the name of the found configuration file
     *
     * @throws ConfigurationException if the find cannot be copied
     **************************************************************************/
    public static String addGridConfiguration (String service, Object owner)
	throws ConfigurationException {
	String configFilename =
	    Config.getString (PROP_GRID_CONFIGURATION,
			      GRID_CONFIG_FILENAME,
			      service,
			      owner);

	// if there is an error it is reported by Config
	Config.addConfigPropertyFile (configFilename);

	// the same file may serve also as the cog.properties config
	// file - but only if its native System property is not
	// already set
	if (System.getProperty ("org.globus.config.file") != null)
	    return configFilename;

	// - but I have to replicate it because the properties have to
	// be first read in order to make there all necessary
	// substitutions
	File[] files = Config.getConfigFiles();
	for (File file: files) {
	    String filename = file.getAbsolutePath();
	    if (filename.endsWith (configFilename)) {
		File cogPropsFile = null;
		try {
		    cogPropsFile = File.createTempFile ("cog", ".properties");
		    cogPropsFile.deleteOnExit();
		    PropertiesConfiguration cogCfg = new PropertiesConfiguration();
		    cogCfg.setFile (cogPropsFile);
		    for (Iterator it = Config.get().getKeys(); it.hasNext(); ) {
			String key = (String)it.next();
			cogCfg.addProperty (key, Config.get().getString (key));
		    }
		    cogCfg.save (cogPropsFile);
		    log.info ("COG properties stored in " + cogPropsFile.getAbsolutePath());
		} catch (IOException e) {
		    throw new ConfigurationException ("Cannot create COG properties file: " +
						      e.toString());
		}
		System.setProperty ("org.globus.config.file",
				    cogPropsFile.getAbsolutePath());
		break;
	    }
	}

	return configFilename;
    }

    /**************************************************************************
     * Return a base filename (no path attached) with the
     * user-certificate proxy. <p>
     *
     * It uses several configuration properties to create a different
     * file name for different virtual organizations and different
     * users.
     **************************************************************************/
    public static String getProxyFileName (String service,
					   Object owner) {
	String vo = Config.getString (PROP_GRID_VO,
				      DEFAULT_GRID_VO,
				      service, owner);
	return Config.getString (PROP_GRID_PROXY_FILE,
				 DEFAULT_PROXY_FILE + vo + "_" +
				 System.getProperty ("user.name"),
				 service, owner);
    }

    /**************************************************************************
     * Return true if this JVM is able to use grid native
     * methods. This check is done only once (so it can be costly).
     *
     * TBD: Not sure how to do it more elegantly (or more correctly) :-)
     **************************************************************************/
    public static boolean checkGridEnvironment() {
	try {
	    Api.setEnv ("any_name", "any_value");
	    return true;
	} catch (UnsatisfiedLinkError e) {
	    log.error ("Cannot check/set grid envoronment variables: " + e.getMessage());
	    return false;
	}
    }

    /**************************************************************************
     * Create properties that will be turned into environment
     * variables later. They constitute a working grid
     * environment. <p>
     *
     * Do not include properties that already exist as real
     * environment variables. Otherwise, take their values first from
     * the configuration, and - if still not found - use some default
     * values. <p>
     **************************************************************************/
    public static Properties getGridEnvironment (String service,
						 Object owner) {
	Properties env = new Properties();
	String proxyDir = Config.getString (PROP_GRID_PROXY_DIR,
					    System.getProperty ("java.io.tmpdir") +
					    File.separator +
					    DEFAULT_PROXY_DIR,
					    service, owner);
	String proxyFile = getProxyFileName (service, owner);

	env.put ("X509_USER_PROXY", proxyDir + File.separator + proxyFile);
	env.put ("GRID_PROXY_FILE", proxyDir + File.separator + proxyFile);
	env.put ("X509_CERT_DIR",   Config.get().getString (PROP_COG_CACERT,
							    "grid-security/certificates"));
	return env;
    }

    /**************************************************************************
     * Put given properties as new environment variables. This makes
     * sense only if the "grid is enabled" (read: a bit of grid
     * environment is already preset) - so we *can* set environment
     * variables to this JVM (using native methods).
     **************************************************************************/
    public static void setGridEnvironment (Properties env) {
	try {
	    for (Enumeration en = env.keys(); en.hasMoreElements(); ) {
		String key = (String)en.nextElement();
		Api.setEnv (key, env.getProperty (key));
	    }
	    if (log.isDebugEnabled()) {
		StringBuffer buf = new StringBuffer();
		for (Enumeration en = env.keys(); en.hasMoreElements(); ) {
		    String key = (String)en.nextElement();
		    buf.append (key + "=" + System.getenv (key) + "\n");
		}
		log.debug ("Set environment variables:\n" +
			   StringUtils.trim (new String (buf)));
	    }

	} catch (UnsatisfiedLinkError e) {
	    log.error ("Cannot set grid envoronment variables: " + e.getMessage());
	}
    }


    /**************************************************************************
     * Get details from the given 'status', select some, potentially
     * adjust them to be more readable, and pass them to the Reporter.
     **************************************************************************/
    public static void reportDetails (JobStatus status, Reporter reporter) {
	for (Iterator it = status.keySet().iterator(); it.hasNext(); ) {
	    Object key = it.next();
	    Object value = status.get (key);
	    if (value == null)
		continue;
	    String strValue = value.toString();
	    if (StringUtils.isBlank (strValue))
		continue;
	    if (key instanceof Integer) {
		int attrCode = ((Integer)key).intValue();

		// some attributes need an adjustment
		switch (attrCode) {
		case JobStatus.JDL:
		case JobStatus.CONDOR_JDL:
		case JobStatus.MATCHED_JDL:
		    continue;   // too long, not (hopefully) interested, TBD?

		case JobStatus.STATE_ENTER_TIME:
		case JobStatus.LAST_UPDATE_TIME:
		    strValue = toTime (strValue);
		    break;

		case JobStatus.STATE_ENTER_TIMES:
		    Vector stateTimeVector = (Vector)value;
		    String currentTime;
		    for (int j = 0; j < stateTimeVector.size(); j++) {
			currentTime = stateTimeVector.get(j).toString().trim();
			if (! currentTime.equals ("0.0 s")) {
			    try {
				reporter.report ("State (" + j + ") " +
						 String.format ("%1$-9s",
								JobStatus.code [j].toUpperCase()) +
						 " entered time",
						 toTime (currentTime));
			    } catch (ArrayIndexOutOfBoundsException e) {
			    }
			}
		    }
		    continue;

		case JobStatus.RESUBMITTED:
		case JobStatus.SUBJOB_FAILED:
		case JobStatus.EXPECT_UPDATE:
		    switch (((Integer) value).intValue()) {
		    case 0:
			strValue = "No";
			break;
		    case 1:
			strValue = "Yes";
			break;
		    }
		    break;
		}

		try {
		    // find the name for the attribute/property
		    reporter.report (JobStatus.attNames [attrCode],strValue); 
		} catch (ArrayIndexOutOfBoundsException e) {
		    continue;
		}
	    }
	}
    }


}
