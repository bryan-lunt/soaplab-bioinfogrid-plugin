// SaveOutput.java
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

package org.bioinfogrid.services;

import java.io.PrintStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashSet;

/**
 * Redirecting standard output to a file, with chaining and
 * thread-safety. <p>
 *
 * This is here temporarily (should go to general tools later). <p>
 *
 * @author <A HREF="mailto:martin.senger@gmail.com">Martin Senger</A>
 * @version $Id: SaveOutput.java,v 1.1 2007/03/30 09:12:01 marsenger Exp $
 */

public class SaveOutput
    extends PrintStream {

    static HashSet<SaveOutput> allSaved = new HashSet<SaveOutput>();

    OutputStream file;
    PrintStream oldStdout;
    long creatorId;

    /**************************************************************************
     *
     **************************************************************************/
    public SaveOutput (PrintStream oldStdout, String destFile)
	throws IOException {
	super (oldStdout);

	this.oldStdout = oldStdout;
	creatorId = Thread.currentThread().getId();

	file = new PrintStream
	    (new BufferedOutputStream
	     (new FileOutputStream (destFile)));

    }

    /**************************************************************************
     *
     **************************************************************************/
    public PrintStream getOldStdout() {
	return oldStdout;
    }

    /**************************************************************************
     *
     **************************************************************************/
    public void setOldStdout (PrintStream oldStdout) {
	this.oldStdout = oldStdout;
    }

    /**************************************************************************
     *
     **************************************************************************/
    public long getId() {
	return creatorId;
    }

    /**************************************************************************
     *
     **************************************************************************/
    public void closeOutput()
	throws IOException {
	file.close();
    }

    // starts copying stdout to file f
    /**************************************************************************
     *
     **************************************************************************/
    public static synchronized void start (String f)
	throws IOException {

	SaveOutput current = new SaveOutput (System.out, f);
	allSaved.add (current);

	// Start redirecting the output.
	System.setOut (current);
    }

    // Restores the original settings.
    /**************************************************************************
     *
     **************************************************************************/
    public static synchronized void stop() {

	// find myself
	long currentId = Thread.currentThread().getId();
	SaveOutput current = null;
	for (Iterator<SaveOutput> it = allSaved.iterator(); it.hasNext(); ) {
	    SaveOutput so = it.next();
	    if (currentId == so.getId()) {
		current = so;
		break;
	    }
	}
	if (current == null) return;   // should never happen :-)

	// find who has me as an old
	boolean found = false;
	for (Iterator<SaveOutput> it = allSaved.iterator(); it.hasNext(); ) {
	    SaveOutput so = it.next();
	    if (so.getOldStdout().equals (current)) {
		so.setOldStdout (current.getOldStdout());
		found = true;
		break;
	    }
	}
	if (! found) {
	    System.setOut (current.getOldStdout());
	}
        try {
	    current.closeOutput();
        } catch (Exception e) {
	    current.setError();
        }

	// remove myself
	allSaved.remove (current);
    }

    // PrintStream override
    /**************************************************************************
     *
     **************************************************************************/
    public void write (int b) {
        try {
	    if (Thread.currentThread().getId() == creatorId)
		file.write (b);
	    else
		oldStdout.write (b);
        } catch (Exception e) {
            setError();
        }
    }

    // PrintStream override
    /**************************************************************************
     *
     **************************************************************************/
    public void write (byte buf[], int off, int len) {
        try { 
	    if (Thread.currentThread().getId() == creatorId)
		file.write (buf, off, len);
	    else
		oldStdout.write (buf, off, len);
        } catch (Exception e) {
            setError();
        }
    }

}
