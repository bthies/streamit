/*
 * Created on Feb 11, 2004
 */
package streamit.eclipse.grapheditor.graph;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;

/**
 * IFileMappings maintains the mapping between a GEStreamNode and an IFile. 
 * 
 * @author jcarlos
 */
public class IFileMappings {

	/**
	 * HashMap that contains the mapping between the GEStreamNodes and their corresponding IFiles.
	 * The keys in the HashMap are the GEStreamNodes and the values are going to be the IFiles. 
	 */
	public static HashMap IFileMappings = new HashMap();

	/**
	 * Map GEStreamNodes to an IFile.
	 * @param cells Object[] Array of GEStreamNode that have ifile as their IFile.
	 * @param ifile IFile
	 */
	public static void addIFileMappings(Object[] cells, IFile ifile)
	{
		for (int i = 0; i < cells.length; i++)
		{
			if (cells[i] instanceof GEStreamNode)
			{ 
				IFileMappings.put(cells[i], ifile);
			}
		}
	}

	/**
	 * Set ifile to correspond to the IFile belonging to node.
	 * @param cell Object
	 * @param ifile IFile
	 */
	public static void addIFileMappings (GEStreamNode node, IFile ifile)
	{
		IFileMappings.put(node, ifile);
	}

	/**
	 * Get the IFile that corresponds to node.
	 * @param node GEStreamNode
	 * @return IFile
	 */
	public static IFile getIFile(GEStreamNode node)
	{
		return (IFile) IFileMappings.get(node);
	}	
}
