/*
 * Created on Dec 13, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import org.eclipse.core.resources.IFile;

/**
 * Set the value for the LogFile.
 * @author jcarlos
 *
 */
public class LogFile 
{

	private static IFile ifile = null;
	
	/**
	 * Set the log file to the IFile passed as an argument.
	 * @param file IFile.
	 */
	public static void setIFile(IFile file)
	{
		LogFile.ifile = file;
	}
	
	/**
	 * Get the log file.
	 * @return IFile.
	 */
	public static IFile getIFile()
	{
		return LogFile.ifile;
	}

}
