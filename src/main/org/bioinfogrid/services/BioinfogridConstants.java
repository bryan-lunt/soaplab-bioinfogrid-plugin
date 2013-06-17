// BioinfogridConstants.java
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

/**
 * A set of property names, file names and other constants. <p>
 *
 * @author <A HREF="mailto:martin.senger@gmail.com">Martin Senger</A>
 * @version $Id: BioinfogridConstants.java,v 1.8 2007/04/08 19:05:46 marsenger Exp $
 */

public interface BioinfogridConstants {

    /**
     * A filename indicating a file containing Bioinfogrid run-time
     * properties. This file name can be changed by setting Java
     * property {@link #PROP_GRID_CONFIGURATION} to point to a
     * different file name.
     */
    public static final String GRID_CONFIG_FILENAME = "bioinfogrid.properties";

    /**
     * A property name. Its value contains a filename indicating a
     * file containing Bioinfogrid configuration properties. Default
     * value is {@link #GRID_CONFIG_FILENAME}.
     */
    public static final String PROP_GRID_CONFIGURATION = "bioinfogrid.configuration";

    /**
     * A property name. Its value contains a directory name where all
     * grid-plugin jobs create their files (before they are submitted
     * to the real grid). <p>
     *
     * Default value is &lt;current-dir&gt;/{@link #DEFAULT_JOBS_DIR}.
     */
    public static final String PROP_GRID_JOBS_DIR = "grid.jobs.dir";

    /**
     * A default value for property {@link #PROP_GRID_JOBS_DIR}.
     */
    public static final String DEFAULT_JOBS_DIR = "JOBS";

    /**
     * A property name. Its value is a name of a virtual
     * organization. <p>
     *
     * Default value is {@link #DEFAULT_GRID_VO}.
     */
    public static final String PROP_GRID_VO = "grid.vo";

    /**
     * A default value for property {@link #PROP_GRID_VO}.
     */
    public static final String DEFAULT_GRID_VO = "biomed";

    /**
     * A property name. Its value is a name of a JDL file. This
     * property usually makes sense only when its name is prefixed
     * with a service name whose JDL file is being located. Default
     * value is {@link #DEFAULT_JDL_TEMPLATE}.
     */
    public static final String PROP_JDL_FILE = "jdl.file";

    /**
     * A default value for property {@link #PROP_JDL_FILE}.
     */
    public static final String DEFAULT_JDL_TEMPLATE = "universal.jdl";

    /**
     * A standard name of a JDL file once it is created/copied in the
     * job directory. Please note that this is <b>not</b> a default
     * value for property {@link #PROP_JDL_FILE}. The value of such
     * property - if used - is a source file to be copied into a file
     * given by this <tt>STANDARD_JDL_FILENAME</tt>.
     */
    public static final String STANDARD_JDL_FILENAME = "job.jdl";

    /**
     * A property name. Its value contains a Network Server address
     * (NSAddress), a hostname with a port number. It is an address of
     * a broker that will be used to submit and monitor grid jobs. <p>
     *
     * Default value is {@link #DEFAULT_NSADDRESS}.
     */
    public static final String PROP_GRID_NSADDRESS = "grid.nsaddress";

    /**
     * A default value for property {@link #PROP_GRID_NSADDRESS}.
     */
    public static final String DEFAULT_NSADDRESS = "glite-rb-00.cnaf.infn.it:7772";

    /**
     * A property name. Its value contains a Logging and Bookkeeping
     * address (LBAddress), a hostname with a port number. It can be
     * also a list of such addresses (comma-separated). <p>
     *
     * Default value is {@link #DEFAULT_LBADDRESS}.
     */
    public static final String PROP_GRID_LBADDRESS = "grid.lbaddress";

    /**
     * A default value for property {@link #PROP_GRID_LBADDRESS}.
     */
    public static final String DEFAULT_LBADDRESS = "glite-rb-00.cnaf.infn.it:9000";

    /**
     * A property name. Its value contains a URL (without the protocol
     * part) of a Computing Element assigned to perform a grid
     * job. <p>
     *
     * There is no default value (because it should be up to the grid
     * itself to find a suitable (matching) computing element).
     */
    public static final String PROP_GRID_CE = "grid.ce";

    /**
     * A property name. Its value contains a directory name where the
     * user-certificate proxy can be found. Note that the proxy must
     * be created there manually, it is not (at least not for now) a
     * task of this plugin. <p>
     *
     * Default value is created by concatenating Java System
     * properties <tt>java.io.tmpdir</tt>, <tt>file.separator</tt>,
     * and {@link #DEFAULT_PROXY_DIR}.
     */
    public static final String PROP_GRID_PROXY_DIR = "grid.proxy.dir";

    /**
     * A default value for property {@link #PROP_GRID_PROXY_DIR}.
     */
    public static final String DEFAULT_PROXY_DIR = "proxies";

    /**
     * A property name. Its value contains a base filename (without
     * path) with a user-certificate proxy. This file will be looked
     * for in directory given by {@link #PROP_GRID_PROXY_DIR}. <p>
     *
     * Default value is created by concatenating {@link
     * #DEFAULT_PROXY_FILE}, {@link #DEFAULT_GRID_VO}, followed by an
     * underscore and Java System property <tt>user.name</tt>.
     */
    public static final String PROP_GRID_PROXY_FILE = "grid.proxy.file";

    /**
     * A (part of) the default value for property {@link
     * #PROP_GRID_PROXY_FILE}.
     */
    public static final String DEFAULT_PROXY_FILE = "x509";

    /**
     * A property name. Its value contains a full filename of a
     * user-proxy certificate. It is a property used by CoG Java
     * Kit. Therefore, it does not make sense to prepend it with a
     * service or class name (as it is possible with other
     * properties). <p>
     *
     * There is no default value. Recommended value is a concatenation
     * of {@link #PROP_GRID_PROXY_DIR}, Java System property
     * <tt>file.separator</tt> and {@link #PROP_GRID_PROXY_FILE}.
     */
    public static final String PROP_COG_PROXY = "proxy";

    /**
     * A property name. Its value contains a directory with CA cert
     * files. It is a property used by CoG Java Kit. Therefore, it
     * does not make sense to prepend it with a service or class name
     * (as it is possible with other properties). <p>
     *
     * There is no default value. These CA cert files are usually
     * located in the UIPandP packag in
     * <tt>grid-security/certificates</tt> directory.
     */
    public static final String PROP_COG_CACERT = "cacert";

    /**
     * A standard name of a file containing grid job ID (the file is
     * created in the job directory).
     */
    public static final String STANDARD_JOBID_FILENAME = "job.id";


    /**
     * A property name. Its integer value specifies a logger level for
     * the low-level grid commands. A value between 1 and 6 is
     * accepted. It is used for submitting jobs. <p>
     *
     * Default value is 3.
     */
    public static final String PROP_GRID_LOGGER_LEVEL = "grid.logger.level";


    /**
     * A property name. Its integer value, in seconds, specifies how
     * often to ask for a grid job status. This interval will be
     * dynamically increased after some time, but it never extends
     * value given by property {@link
     * #PROP_GRID_MAX_POLLING_INTERVAL}. Therefore, if you wish to
     * keep a fixed polling interval, set both properties to the same
     * value. <p>
     *
     * Default value is {@link #DEFAULT_POLLING_INTERVAL}.
     */
    public static final String PROP_GRID_POLLING_INTERVAL = "grid.polling.interval";

    /**
     * A default value for property {@link #PROP_GRID_POLLING_INTERVAL}.
     */
    public static final int DEFAULT_POLLING_INTERVAL = 10;

    /**
     * A property name. Its integer value, in seconds, specifies the
     * maximum delay between two pollings for a grid job status.
     *
     * Default value is {@link #DEFAULT_MAX_POLLING_INTERVAL}.
     */
    public static final String PROP_GRID_MAX_POLLING_INTERVAL = "grid.max.polling.interval";

    /**
     * A default value for property {@link #PROP_GRID_MAX_POLLING_INTERVAL}.
     */
    public static final int DEFAULT_MAX_POLLING_INTERVAL = 300; // 5 mins

    /**
     * A property name. Its integer value, in seconds, specifies the
     * maximum time to wait for the grid job to complete.
     *
     * Default value is {@link #DEFAULT_JOB_TIMEOUT}.
     */
    public static final String PROP_GRID_JOB_TIMEOUT = "grid.job.timeout";

    /**
     * A default value for property {@link #PROP_GRID_JOB_TIMEOUT}.
     */
    public static final int DEFAULT_JOB_TIMEOUT = 10 * 60 * 10; // 10 hours

    /**
     * A standard name of a directory (simple name, no path) where the
     * grid commands retrieve result to. This directory is created
     * inside every grid job directory.
     */
    public static final String STANDARD_GRID_RESULTS_DIR = "results";

    /**
     * A property name. Its value is a name of an executable (usually
     * a script) that is going to be executed on the grid. There is no
     * default value. If it does not contain an absolute path, it will
     * be looked for in the directory given by property {@link
     * #PROP_SCRIPTS_DIR}.
     */
    public static final String PROP_EXECUTABLE = "executable";

    /**
     * A property name. Its value is a directory name where the
     * executables (scripts) are looked for. There is no default
     * value.
     */
    public static final String PROP_SCRIPTS_DIR = "grid.scripts.dir";

    /**
     * A property name. Its value is a prefix used in names of those
     * properties who will become environment variables.
     */
    public static final String PROP_GRID_ENVAR = "grid.env";



}
