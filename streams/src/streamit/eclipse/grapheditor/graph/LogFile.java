/*
 * Created on Dec 13, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import org.eclipse.core.resources.IFile;

/**
 * 
 * @author jcarlos
 *
 */
public class LogFile 
{

	private static IFile ifile = null;
	
	public static void setIFile(IFile file)
	{
		LogFile.ifile = file;
	}
	
	public static IFile getIFile()
	{
		return LogFile.ifile;
	}

}
