package examples;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.types.Environment;

import java.io.File;

public class MyExec {

    static public void execute() {

	long started = System.currentTimeMillis();
 	Java executor = new Java();
	executor.setFork (true);
// 	executor.createArg().setValue ("examples.Test");
	executor.setClassname ("examples.Test");
	executor.createJvmarg().setValue ("-cp");
	executor.createJvmarg().setValue (System.getProperty ("java.class.path"));

	Environment.Variable env = new Environment.Variable();
	env.setKey ("martin");
	env.setValue ("senger");
	executor.addEnv (env);
 	Project project = new Project();
 	project.init();

	DefaultLogger logger = new DefaultLogger();
	logger.setMessageOutputLevel (Project.MSG_INFO);
	logger.setOutputPrintStream (System.out);
	logger.setErrorPrintStream (System.err);
// 	Vector<BuildListener> listeners = new Vector<BuildListener> (1);
// 	listeners.addElement (logger);

// 	    BuildListener listener = (BuildListener)listeners.elementAt(i);
// 	    if (listener instanceof BuildLogger)
// 		((BuildLogger)listener).setMessageOutputLevel (msgOutputLevel);
	project.setProjectReference (logger);
	project.addBuildListener (logger);




	executor.setProject (project);
	System.out.println ("CODE: " + executor.executeJava());
	System.out.println ("MILLIS: " + (System.currentTimeMillis() - started));;

// 	System.out.println ("CLASSPATH: " + System.getProperty ("java.class.path"));

//  	executor.setJavaCommand (new Commandline ("examples.Test"));
// 	executor.setSystemProperties (CommandlineJava.SysProperties s)
//  	Project project = new Project();
//  	project.init();

// 	executor.execute (project);



// 	final class MyComponent extends ProjectComponent {
// 	    public MyComponent() {
// 	    }       
// 	}

// 	ExecuteJava executor = new ExecuteJava();
// 	executor.setJavaCommand (new Commandline ("examples.Test"));
// 	Project project = new Project();
// 	project.init();
// 	MyComponent pComp = new MyComponent();
// 	pComp.setProject (project);
// 	executor.fork (pComp);

    }

    static public void main (String[] args) {
	execute();
    }
}
