package streamit.io;

import streamit.Filter;
import streamit.Channel;

import java.io.*;

public class FileReader extends Filter
{
    Class fileType;
    File inputFile;
    String fileName;
    java.io.FileInputStream fileInputStream;
    ObjectInputStream inputStream;

    public FileReader (String fileName, Class type)
    {
        this.fileType = type;
	this.fileName = fileName;
	openFile();
    }

    private void closeFile() {
	try {
	    fileInputStream.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private void openFile() {
        try
        {
            inputFile = new File(fileName);
            fileInputStream = new java.io.FileInputStream (inputFile);
            inputStream = new ObjectInputStream (fileInputStream);
        }
        catch(Throwable e)
        {
	    e.printStackTrace();
            ERROR ("Could not open file " + fileName + " for reading (see above).");
        }
    }

    public void init ()
    {
        output = new Channel (fileType, 1);
    }

    public void work ()
    {
	boolean done = false;
	while (!done) {
	    try {
		if (fileType == Integer.TYPE) {
		    output.pushInt (inputStream.readInt ());
		} else if (fileType == Short.TYPE) {
		    output.pushShort (inputStream.readShort ());
		} else if (fileType == Character.TYPE) {
		    output.pushChar (inputStream.readChar ());
		} else if (fileType == Float.TYPE) {
		    output.pushFloat (inputStream.readFloat ());
		} else if (Class.forName ("java.io.Serializable").isAssignableFrom (fileType)) {
		    Object newObject = inputStream.readObject ();
		    ASSERT (newObject);
		    output.push (newObject);
		} else {
		    ERROR ("You must define a reader for your type here.\nIf you're trying to read an object, it should be a serializable object\n(and then you won't have to do anything special).");
		}
		done = true;
	    }
	    catch (EOFException e) {
		// try closing and opening file, to try again
		closeFile();
		openFile();
	    }
	    catch (Throwable e)
		{
		    e.printStackTrace();
		    ERROR ("There was an error reading from a file (See above)");
		}
	}
    }
}
