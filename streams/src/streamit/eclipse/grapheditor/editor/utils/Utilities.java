/*
 * @(#)Utilities.java	1.2 30.01.2003
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package streamit.eclipse.grapheditor.editor.utils;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

/**
 * Utility methods.  A utility method is characterized as a method which is
 * of general utility and is not specific to JGraphpad or JGraph.
 *
 * For example, this would include things like generic sorting algorithms,
 * parsing routines, standard error handling methods, etc.
 *
 * It is important that this code be optimized, and secondly you should be
 * concerned about not reinventing the wheel...before adding content here
 * you should try and find another open source project that already implements
 * said functionality in a robust manner.  A good place to look is:
 * Apache/Jakarta Commons.
 *
 * There are many methods commented out in this class as many of these methods
 * were imported from different projects but not yet currently used.  Please take
 * a look here first to see if anything that you need has already been implemented.
 *
 * @author sven.luzar
 * @author van.woods
 * @version 1.0
 *
 */
public final class Utilities {
	// switch to be set by developer while coding, not at runtime
//	private static boolean developerVersion = false;

	// flag to indicate whether to display error msgs
//	private static boolean displayErrorMsgs = true;
//	private static boolean displayToConsole = false;

	// Log messages switch
//	private static boolean m_logMessages = false;
//	private static Vector m_loggedMessages = new Vector();
//	private static String m_logfilePath = System.getProperty("user.dir");
//	private static String m_logfileName = "log.txt";


	/**
	 * The Utilities class should never be instantiated and should not have any
	 * state data associated with it, and this constructor enforces that.
	 */
	private Utilities() {
	}

	/**
	 * Take the given string and chop it up into a series
	 * of strings on whitespace boundries.  This is useful
	 * for trying to get an array of strings out of the
	 * resource file.
	 */
	public static String[] tokenize(String input) {
		return tokenize(input, " \t\n\r\f");
	}

	public static String[] tokenize(String input, String delim) {
		if (input == null)
			return new String[]{};
		StringTokenizer t = new StringTokenizer(input, delim);
		String cmd[];

		cmd = new String[t.countTokens()];
		int i = 0;
		while (t.hasMoreTokens()){
			cmd[i] = (String) t.nextToken();
			i++;
		}

		return cmd;
	}

	/** Returns a random number between 0 and max.
	 * */
	public static int rnd(int max) {
		return (int) (Math.random() * max);
	}

	/** parses the pattern and tries to
	 *  parse each token as a float.
	 *
	 *  @return array with the float value for each token
	 */
	public static float[] parsePattern(String pattern) {
		StringTokenizer st = new StringTokenizer(pattern, ",");
		float[] f = new float[st.countTokens()];
		if (f.length > 0) {
			int i = 0;
			while (st.hasMoreTokens())
				f[i++] = Float.parseFloat(st.nextToken());
		}
		return f;
	}
	/** Returns the classname without the package.
	 *  Example: If the input class is java.lang.String
	 *  than the return value is String.
	 *
	 *  @param cl The class to inspect
	 *  @return The classname
	 *
	 */
	public static String getClassNameWithoutPackage(Class cl){
		// build the name for this action
		// without the package prefix
		String className = cl.getName();
		int pos = className.lastIndexOf('.') + 1;
		if (pos == -1)
			pos = 0;
		String name = className.substring(pos);
		return name;
	}

	public static void center(Window frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		frame.setLocation(
			screenSize.width / 2 - (frameSize.width / 2),
			screenSize.height / 2 - (frameSize.height / 2));
	}



// ==============================================================================
// Van's contributions; any source that is commented is not currently used but
// is of potential value. Please uncomment and use as needed.
// ==============================================================================

	// ==============================================================================
	// Math utils
	// ==============================================================================

	// =========================================================================
	/**
	 * Round a value to some number of decimal places.
	 * <P>
	 * Examples:<BR>
	 * &nbsp;&nbsp; round(12345.6789,3)  => 12345.679<BR>
	 * &nbsp;&nbsp; round(12345.6789,-3) => 12000.0
	 *
	 * @param value Value to round.
	 * @param places Number of places.
	 *        places > 0 are to the right; this many digits will be kept.
	 *        places = 0 has no effect.
	 *        places < 0 are to the left of the decimal; this many digits will be zeroed.
	 */
//	public static double round( double value, int places ) {
//		double shift = Math.pow( 10, places );
//		// Don't use Math.round, because it returns a long
//		return Math.floor( value * shift + 0.5 ) / shift;
//	}

	// ==============================================================================
	// Array utils
	// ==============================================================================

	// =========================================================================
	/** Add elements in source array into destination vector. */
//	public static final void addInto(Vector dest, Object[] src) {
//		if (dest==null || src==null) return;
//		for (int i=0; i<src.length; i++) {
//			dest.addElement(src[i]);
//		}
//	}

	// =========================================================================
	/** Add elements from source vector into destination vector. */
//	public static final void addInto(Vector dest, Vector src) {
//		if (src==null || dest==null) return;
//		int size = src.size();
//		for (int i = 0; i<src.size(); i++) {
//			dest.addElement(src.elementAt(i));
//		}
//	}

	// =========================================================================
	/** Add elements from source enumeration into destination vector. */
//	public static final void addInto(Vector dest, Enumeration src) {
//		if (dest==null || src==null) return;
//		while (src.hasMoreElements()) {
//			dest.addElement(src.nextElement());
//		}
//	}

	// =========================================================================
	/**
	 *  Removes empty (null) array elements, and returns a new, smaller, array
	 *  instance of the same type. Must be cast back to original type to use.
	 *
	 *@param  array  array to be compressed
	 *@return        new array instance of the same type with no null elements
	 */
//	public static final Object[] compressArray(Object[] array) {
//		if (array==null) return null;
//		int newSize = 0;
//		for (int i = 0; i < array.length; i++) {
//			if (array[i] != null) {
//				newSize++;
//			}
//		}
//
//		Object[] newArray = (Object[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), newSize);
//
//		newSize = 0;
//		for (int i = 0; i < array.length; i++) {
//			if (array[i] != null) {
//				newArray[newSize] = array[i];
//				newSize++;
//			}
//		}
//		return newArray;
//	}

	// ==============================================================================
	// String utils
	// ==============================================================================

	// =========================================================================
	/** Get substring prior to seperator.  Excludes seperator on returned string.
	Returns an empty string if seperator not found. */
//	public static final String getPrefix(String incomingString, String seperator) {
//		if (incomingString==null || seperator==null)
//			throw new IllegalArgumentException();
//
//		String outgoingString = new String("");
//		int tmpIndex = incomingString.indexOf(seperator);
//		if (tmpIndex != -1) {
//			outgoingString = incomingString.substring(0, tmpIndex);
//		}
//		return outgoingString;
//	}

	// =========================================================================
	/** Get the string after a seperator.  Excludes seperator on returned string.
	Returns an empty string if seperator not found. */
//	public static final String getSuffix(String incomingString, String seperator) {
//		if (incomingString==null || seperator==null)
//			throw new IllegalArgumentException();
//
//		String outgoingString = new String("");
//		int tmpIndex = incomingString.indexOf(seperator);
//		if (tmpIndex != -1) {
//			outgoingString = incomingString.substring(tmpIndex+1, incomingString.length());
//		}
//		return outgoingString;
//	}

	// =========================================================================
	/** Remove the stringToStrip from the incomingString, and return the new string */
//	public static final String strip(String incomingString, String stringToStrip) {
//		if (incomingString==null || stringToStrip==null)
//			throw new IllegalArgumentException();
//
//		String outgoingString = incomingString;
//		int tmpIndex = outgoingString.indexOf(stringToStrip);
//		while (tmpIndex != -1) {
//			outgoingString = outgoingString.substring(0, tmpIndex) + outgoingString.substring(tmpIndex+stringToStrip.length(), outgoingString.length());
//			tmpIndex = outgoingString.indexOf(stringToStrip);
//		}
//		return outgoingString;
//	}

	// =========================================================================
	/**
	 * Remove each of the stringsToStrip from the incomingString, and return
	 * the new string
	 */
//	public static final String strip(String incomingString, String[] stringsToStrip) {
//		if (incomingString==null || stringsToStrip==null)
//			throw new IllegalArgumentException();
//
//		String outgoingString = incomingString;
//		for (int i=0; i < stringsToStrip.length; i++) {
//			outgoingString = strip(outgoingString, stringsToStrip[i]);
//		}
//		return outgoingString;
//	}

	// =========================================================================
	/**
	 * Strip a particular character from a string.  If the target character
	 * doesn't appear in the string, the original string is returned.  If it
	 * does, then a new string is returned that consists of the original string
	 * except with every occurrence of the target character removed.
	 */
//	public static final String stripChar(String str, char tc) {
//		if (str==null)
//			throw new IllegalArgumentException();
//
//		int len = str.length();
//		int i = -1;
//		int j = 0;
//
//		while (++i < len) {
//			if (str.charAt(i) == tc) {
//				break;
//			}
//		}
//		if (i < len) {
//			char buf[] = new char[len];
//			for (; j < i ; j++) {
//				buf[j] = str.charAt(j);
//			}
//			while (i < len) {
//				char c = str.charAt(i++);
//				if (c != tc) {
//					buf[j++] = c;
//				}
//			}
//			return new String(buf, 0, j);
//		}
//		return str;
//	}

	// =========================================================================
	/**
	 * Replace all occurances of stringToRemove with stringToBeAdded in
	 * incomingString, and return the new string
	 */
//	public static final String replace(String incomingString, String stringToRemove, String stringToBeAdded) {
//		if (incomingString==null || stringToRemove==null)
//			throw new IllegalArgumentException();
//
//		if (stringToBeAdded == null) stringToBeAdded = new String();
//
//		String outgoingString = new String(incomingString);
//		int tmpIndex = 0;
//		while (outgoingString.indexOf(stringToRemove) != -1) {
//			tmpIndex = outgoingString.indexOf(stringToRemove);
//			outgoingString = outgoingString.substring(0, tmpIndex) + stringToBeAdded + outgoingString.substring(tmpIndex+stringToRemove.length(), outgoingString.length());
//		}
//		return outgoingString;
//	}

	// =========================================================================
	/**
	 * Find indices of occurences of stringToFind in incomingString.
	 * Returns vector of Integer indices.  Each subsequent search is
	 * exclusive of the previous find, i.e. ("aaaaaaa", "aa") returns
	 * Vector.size()==3
	 */
//	public static final Vector find(String incomingString, String stringToFind) {
//		if (incomingString==null || stringToFind==null)
//			throw new IllegalArgumentException();
//
//		Vector indexList = new Vector();
//		int index = 0;
//		for (int i = 0; i < incomingString.length(); i += index + stringToFind.length()) {
//			try {
//				index = incomingString.indexOf(stringToFind, i);
//				// if found, add to list, else done
//				if (index >= 0) {
//					indexList.addElement(new Integer(index));
//				}
//				else
//					break;
//			}
//			catch (Exception e) {
//				e.printStackTrace();
//				continue;
//			}
//		}
//		return indexList;
//	}

	// ==============================================================================
	// File utils
	// ==============================================================================

	/**
	 * Copy a file from the source specified to the destination specified
	 *
	 * @param parentFrame  frame for which error messages are centered within; <code>null</code> is valid
	 * @param source  Description of Parameter
	 * @param dest    Description of Parameter
	 * @return        <code>true</code> if successful
	 *                <code>false</code> if source equals dest or source does not exist, or any other error condition whic prevents the copy from happening
	 */
//	public static final boolean copyFile(Frame parentFrame, String source, String dest) {
//		try {
//			return copyFile(source, dest);
//		}
//		catch (Exception e) {
//			errorMessage(parentFrame, "Error copying file:\n" + e);
//			return false;
//		}
//	}
//	public static final boolean copyFile(String source, String dest) throws IOException, Exception {
//		FileInputStream in = null;
//		FileOutputStream out = null;
//		try {
//			if (source.equals(dest)) {
//				return false;
//			}
//
//			informationMessage("Attempting to copy " + source + " to " + dest + "...");
//
//			File sourceFile = new File(source);
//			if (sourceFile.exists() == false) {
//				informationMessage("Source file " + source + " does not exist!");
//				return false;
//			}
//
//			// if dest exists, avoid readonly test (1.2 only) and just delete it to avoid any problems
//			File destFile = new File(dest);
//			if (destFile.exists() && destFile.isFile()) {
//				destFile.delete();
//			}
//
//			in = new FileInputStream(source);
//			out = new FileOutputStream(dest);
//
//			byte[] buf = new byte[1024];
//			int i = 0;
//			while ((i = in.read(buf)) != -1) {
//				out.write(buf, 0, i);
//			}
//
//			in.close();
//			out.close();
//
//			informationMessage("done.");
//		}
//		catch (IOException e) {
//			throw e;
//		}
//		catch (Exception e) {
//			throw e;
//		}
//		finally {
//			if (in != null) {
//				try {
//					in.close();
//				}
//				catch (IOException e) {
//					in = null;
//					throw e;
//				}
//				in = null;
//			}
//			if (out != null) {
//				try {
//					out.close();
//				}
//				catch (IOException e) {
//					out = null;
//					throw e;
//				}
//				out = null;
//			}
//		}
//		return true;
//	}

	/**
	 * Create a directory, plus all parent directories necessary to contain it.
	 * For example, if c:\this\is\an\example\file.name was encapsulated in File,
	 * and c:\this existed but contained no subdirectories, then the remaining
	 * directories is\an\example\ would all be created.
	 *
	 * @param f      Directory path to create.
	 * @return true if successful.
	 */
	public static boolean createDirectoryRecursively(File f) {
		if (f==null) return false;

		if (f.exists()) {
			return true;
		}
		if (!f.isAbsolute()) {
			f = new File(f.getAbsolutePath());
		}
		String par = f.getParent();
		if (par == null) {
			return false;
		}
		if (!createDirectoryRecursively(new File(par))) {
			return false;
		}
		f.mkdir();
		return f.exists();
	}

	/**
	 * Description of the Method
	 *
	 * @param fileName  Description of Parameter
	 * @return          Description of the Returned Value
	 */
	public static final Properties readPropertiesFromFile(Frame parentFrame, File file) {
		try {
			return readPropertiesFromFile(parentFrame, file.getAbsolutePath());
		}
		catch (Exception e) {
//			errorMessage(parentFrame, "Error reading properties file: " + file);
			e.printStackTrace();
			return null;
		}
	}
	public static final Properties readPropertiesFromFile(Frame parentFrame, String fileName) {
		try {
			return readPropertiesFromFile(fileName);
		}
		catch (Exception e) {
//			errorMessage(parentFrame, "Error reading properties file: " + fileName);
			e.printStackTrace();
			return null;
		}
	}
	public static final Properties readPropertiesFromFile(String fileName) throws IOException, Exception {
		File propFile = null;
		FileInputStream in = null;
		Properties properties = new Properties();
		try {
//			informationMessage("Looking for properties file: " + fileName);
			propFile = new File(fileName);
			if (propFile.exists()) {
				in = new FileInputStream(propFile);
//				informationMessage("Reading properties file: " + fileName);
				properties.load(in);
			}
			else {
//				informationMessage("Missing properties file: " + fileName);
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw e;
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
					in = null;
					throw e;
				}
				in = null;
			}
		}
		return properties;
	}

	// =========================================================================
	/**
	 * Description of the Method
	 *
	 * @param properties  Description of Parameter
	 * @param fileName    Description of Parameter
	 */
	public static final void writePropertiesToFile(Frame parentFrame, Properties properties, File file, String fileHeader) {
		try {
			writePropertiesToFile(parentFrame, properties, file.getAbsolutePath(), fileHeader);
		}
		catch (Exception e) {
//			errorMessage(parentFrame, "Error writing properties file: " + file);
			e.printStackTrace();
		}
	}
	public static final void writePropertiesToFile(Frame parentFrame, Properties properties, String fileName, String fileHeader) {
		try {
			writePropertiesToFile(properties, fileName, fileHeader);
		}
		catch (Exception e) {
//			errorMessage(parentFrame, "Error writing properties file: " + fileName);
			e.printStackTrace();
		}
	}
	public static final void writePropertiesToFile(Properties properties, String fileName, String fileHeader) throws IOException, Exception {
		FileOutputStream out = null;
		try {
			if (properties != null) {
				out = new FileOutputStream(fileName);
//				informationMessage("Writing properties file: " + fileName);
				System.out.println("Writing properties file: " + fileName);
				// should use store(), but it's missing from Java 1.1
				properties.save(out, fileHeader);
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw e;
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
					out = null;
					throw e;
				}
				out = null;
			}
		}
	}

	// =========================================================================
	/**
	 *  This is the preferred way to access resources in order to be compatible
	 *  with 1)normal execution, 2)running from a jar, and 3)running from Webstart.
	 *  This requires that the name include the full package directory structure.
	 *  For example if root package is c:\proj\src\rootpkg, and icons are in
	 *  c:\proj\src\rootpkg\subpackage1\icons\ then an icon named 'anIcon.gif'
	 *  would be referenced using the following string:
	 *  rootpkg/subpackage1/icons/anIcon.gif
	 *
	 *@param  fileName  Description of Parameter
	 *@return           An ImageIcon instance if icon is found or null if resource is not found.
	 *@see              For naming issues see
	 *      http://java.sun.com/products/javawebstart/docs/developersguide.html#dev
	 *      and http://www.vamphq.com/resources.html
	 */
//	public static final ImageIcon getIcon(String fileName) {
//		URL url = null;
//		if (fileName == null) {
//			return null;
//		}
//		else {
//			url = Utils.class.getClassLoader().getResource(fileName);
//		}
//		return(url == null) ? null : new ImageIcon(url);
//	}

	// =========================================================================
//	public static final String fixTrailingBackSlash(String path) {
//		if (path == null)
//			return path;
//		else
//			return(path + (path.endsWith("\\") ? "" : "\\"));
//	}

	// ==============================================================================
	// Debug & Logging utils
	// ==============================================================================

//	public static final boolean getLogMessageStatus() {
//		return m_logMessages;
//	}
//	public static final void setLogMessageStatus(boolean status) {
//		m_logMessages = status;
//	}
//
//	public static final String getLogfilePath() {
//		return m_logfilePath;
//	}
//	public static final void setLogfilePath(String path) {
//		m_logfilePath = path;
//	}
//	public static final String getLogfileName() {
//		return m_logfileName;
//	}
//	public static final void setLogfileName(String filename) {
//		m_logfileName = filename;
//	}

//	// =========================================================================
//	/**
//	 * Initialize logging
//	 *
//	 * @param path            path for log file
//	 * @param filename        name of log file
//	 * @param deleteIfExists  delete old logfile if it exists
//	 */
//	public static final void initLogging(String path, String filename, boolean deleteIfExists) {
//		File logFile = new File(path + filename);
//		if (deleteIfExists && logFile.exists() && logFile.isFile()) {
//			logFile.delete();
//		}
//		setLogfilePath(path);
//		setLogfileName(filename);
//		setLogMessageStatus(true);
//	}
//
//	// =========================================================================
//	/**
//	 * Prints a debug message if in developer mode.
//	 *
//	 * @param msg  Description of Parameter
//	 */
//	public static final void debugMessage(String msg) {
//		if (developerVersion || displayToConsole) {
//			System.out.println(msg);
//		}
//		logMessage(msg);
//	}
//
//	/**
//	 * Prints a debug message if in developer mode.
//	 *
//	 * @param e  Description of Parameter
//	 */
//	public static final void debugMessage(Exception e) {
//		StringWriter s = new StringWriter();
//		e.printStackTrace(new PrintWriter(s));
//		debugMessage(s.toString());
//	}
//
//	// =========================================================================
//	/**
//	 * Prints an error message.
//	 *
//	 * @param obj  Description of Parameter
//	 */
//	public static final void errorMessage(Frame parentFrame, Object obj) {
//		errorMessage(parentFrame, obj.toString());
//	}
//
//	/**
//	 * Prints an error message.
//	 *
//	 * @param msg  Description of Parameter
//	 */
//	public static final void errorMessage(Frame parentFrame, String msg) {
//		errorMessage(parentFrame, null, msg);
//	}
//
//	/**
//	 * Prints an error message.
//	 *
//	 * @param msg  Description of Parameter
//	 */
//	public static final void errorMessage(Frame parentFrame, String dialogTitle, String msg) {
//		if (displayErrorMsgs) {
//			try {
//				JOptionPane.showMessageDialog(parentFrame, msg, (dialogTitle==null) ? "ERROR" : dialogTitle, JOptionPane.ERROR_MESSAGE);
//			}
//			catch (Exception e) {
//				// do nothing
//			}
//		}
//		debugMessage(msg);
//	}
//
//	/**
//	 * Prints an error message.
//	 *
//	 * @param e  Description of Parameter
//	 */
//	public static final void errorMessage(Exception e) {
//		errorMessage(null, "Exception Thrown:\n" + e.toString());
//	}
//
//	/**
//	 * Prints an error message.
//	 *
//	 * @param e  Description of Parameter
//	 */
//	public static final void errorMessage(Frame parentFrame, Exception e) {
//		errorMessage(parentFrame, "Exception Thrown:\n" + e.toString());
//	}
//
//	/**
//	 * Prints an error message.
//	 *
//	 * @param msg  Textual message of the error encountered.
//	 * @param e  Actual error exception.
//	 */
//	public static final void errorMessage(Frame parentFrame, String msg, Exception e) {
//		errorMessage(parentFrame, msg + "\n\n" + "Exception Thrown:\n" + e.toString());
//	}
//
//	/**
//	 * Prints an error message.
//	 *
//	 * @param msg  Textual message of the error encountered.
//	 * @param e  Actual error exception.
//	 */
//	public static final void errorMessage(String msg, Exception e) {
//		errorMessage(null, msg, "Exception Thrown:\n" + e.toString());
//	}
//
//	// =========================================================================
//	/**
//	 * Prints an information message.
//	 *
//	 * @param msg  Description of Parameter
//	 */
//	public static final void informationMessage(String msg) {
//		if (displayToConsole) {
//			System.out.println(msg);
//		}
//		logMessage(msg);
//	}
//
//	// =========================================================================
//	/**
//	 *  Prints an error message and exits system with error flag.
//	 *
//	 *@param  msg  Description of Parameter
//	 *@param  e    Description of Parameter
//	 */
//	public static final void fatalError(Frame parentFrame, String msg, Exception e) {
//		errorMessage(parentFrame, msg, e);
//		System.exit(-1);
//	}
//
//	// =========================================================================
//	/**
//	 * Writes out a message to the log file.
//	 *
//	 * @param msg  Description of Parameter
//	 */
//	public static final void logMessage(String msg) {
//		if (m_logMessages) {
//			m_loggedMessages.addElement(msg);
//			if (m_loggedMessages.size() == 1000) {
//				flushMessages();
//			}
//		}
//	}
//
//	// =========================================================================
//	/**
//	 * Writes out message cache to file.
//	 */
//	public static final void flushMessages() {
//		try {
//			RandomAccessFile logFile = new RandomAccessFile(m_logfilePath + m_logfileName, "rw");
//			logFile.seek(logFile.length());
//			for (int i = 0; i < m_loggedMessages.size(); ++i) {
//				logFile.writeBytes((String)m_loggedMessages.elementAt(i));
//				logFile.writeBytes(System.getProperty("line.separator"));
//			}
//			logFile.close();
//			m_loggedMessages.removeAllElements();
//		}
//		catch (Exception e) {
//		}
//	}

	// =========================================================================
	/** Print contents of an array.
	 * @param out PrintStream to print to.
	 * @param array Array to dump.
	 * @param prefix Prefix for each line of output (ie. tab or spaces).
	 */
//	public static final void printArray(PrintStream out, Object[] array, String prefix) {
//		if (array == null) {
//			out.println(prefix + "null");
//			return;
//		}
//
//		out.println(prefix + array.getClass().getComponentType().getName() + "[" + array.length + "]:");
//		prefix += "  ";
//
//		for (int i=0; i<array.length; i++) {
//			if (array[i].getClass().isArray()) {
//				out.println(prefix + i + " = Array:");
//				printArray(out, (Object[])array[i], prefix);
//			}
//			else {
//				out.println(prefix + i + " = " + array[i]);
//			}
//		}
//	}

}
