package examples;

import org.bioinfogrid.services.SaveOutput;

import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Test {

    static class MyThread extends Thread {
	public void run() {
	    for (int i = 0; i < 10; i++) {
		new Thread() { public void run() {
		    long pause = (long)(Math.random() * 1000);
		    if (pause == 0) pause = 500;
		    String threadName = Thread.currentThread().getName();
		    System.out.println (threadName + " sleeps for " + pause);
		    try {
			SaveOutput.start (threadName + ".dat");
			try { Thread.sleep (pause); } catch (Exception e) {}
			System.out.println (threadName + " woke up");
			SaveOutput.stop();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		} }.start();
	    }
	}
    }

    public static void main (String[] args) {

	try {
	    System.out.println ("Before");
	    SaveOutput.start ("output.dat");
 	    new MyThread().start();
	    System.out.println ("to file");
  	    try { Thread.sleep (3000); } catch (Exception e) {}
	    SaveOutput.stop();
	    System.out.println ("After");
	} catch (Exception e) {
	    System.err.println ("ERROR: " + e.toString());
	}
    }

}
