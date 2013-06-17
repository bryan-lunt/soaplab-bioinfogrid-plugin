#  Set environment variable CLASSPATH.
#  Usage: . build/run/source.me.sh
#
#  You do not need to use this source file if you use 'run-*'
#  scripts - those scripts set CLASSPATH for themselves. Use it if you
#  wish to run some clients that do not have their own run script.
#
#  $Id: source.me.sh,v 1.5 2007/10/31 15:12:15 marsenger Exp $
# ----------------------------------------------------

PROJECT_HOME=@PROJECT_HOME@
SOAPLAB_HOME=@SOAPLAB_HOME@
GRID_UI_HOME=@UI_GRID_HOME@
LIBS_PATH=@PROJECT_DEPS@

CLASSPATH=${PROJECT_HOME}/build/classes
CLASSPATH=${SOAPLAB_HOME}/build/classes:$CLASSPATH
CLASSPATH=`echo ${PROJECT_HOME}/lib/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=`echo ${SOAPLAB_HOME}/build/lib/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=`echo ${SOAPLAB_HOME}/lib/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=`echo ${GRID_UI_HOME}/glite/share/java/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=`echo ${GRID_UI_HOME}/d-cache/srm/lib/globus/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=`echo ${GRID_UI_HOME}/glite/externals/jclassads-2.2/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=$LIBS_PATH:$CLASSPATH
export CLASSPATH
