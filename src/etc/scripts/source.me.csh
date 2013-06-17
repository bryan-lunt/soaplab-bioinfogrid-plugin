#  Set environment variable CLASSPATH.
#  Usage: source run/source.me.csh
#
#  You do not need to use this source file if you use 'run/run-*'
#  scripts - those scripts set CLASSPATH for themselves. Use it if you
#  wish to run some clients that do not have their own run script, or
#  if you wish to set some Java properties on the command line
#  (because the run scripts do not support that).
#
#  $Id: source.me.csh,v 1.5 2007/10/31 15:12:15 marsenger Exp $
# ----------------------------------------------------

set PROJECT_HOME=@PROJECT_HOME@
set SOAPLAB_HOME=@SOAPLAB_HOME@
set GRID_UI_HOME=@UI_GRID_HOME@
set LIBS_PATH=@PROJECT_DEPS@

setenv CLASSPATH ${PROJECT_HOME}/build/classes
setenv CLASSPATH ${SOAPLAB_HOME}/build/classes:${CLASSPATH}
setenv CLASSPATH `echo ${PROJECT_HOME}/lib/*.jar | tr ' ' ':'`:${CLASSPATH}
setenv CLASSPATH `echo ${SOAPLAB_HOME}/build/lib/*.jar | tr ' ' ':'`:${CLASSPATH}
setenv CLASSPATH `echo ${SOAPLAB_HOME}/lib/*.jar | tr ' ' ':'`:${CLASSPATH}
setenv CLASSPATH `echo ${GRID_UI_HOME}/glite/share/java/*.jar | tr ' ' ':'`:${CLASSPATH}
setenv CLASSPATH `echo ${GRID_UI_HOME}/d-cache/srm/lib/globus/*.jar | tr ' ' ':'`:${CLASSPATH}
setenv CLASSPATH `echo ${GRID_UI_HOME}/glite/externals/jclassads-2.2/*.jar | tr ' ' ':'`:${CLASSPATH}
setenv CLASSPATH ${LIBS_PATH}:${CLASSPATH}

