/*
 * Created on Dec 13, 2003
 *
 */
package streamit.eclipse.grapheditor.graph.resources;

import java.awt.Image;
import java.net.URL;
import java.util.Stack;

import javax.swing.ImageIcon;

/**
 * @author jcarlos
 *
 */
public class ImageLoader {
	/** contains string objects which respresents the search paths
	 */	
	protected static Stack searchPath = new Stack();

	/** pushes the default search path to the stack
	 *  @see #pushSearchPath
	 */
	static {
		pushSearchPath("/streamit/eclipse/grapheditor/graph/resources"); 
	}
	
	/** Returns an Image from the same path.
	 *  @param imageName An image name with the file extension
	 *                    buttonEdge.g. Can.gif
	 */
	public static Image getImage(String imageName){
		return getImageIcon(imageName).getImage() ;
	}

	/** Returns an ImageIcon from the same path.
	 *  @param imageName An image name with the file extension
	 *                    buttonEdge.g. Can.gif
	 */
	public static ImageIcon getImageIcon(String imageName){
		return getImageIcon(searchPath.size()-1, imageName);
	}

	/** Returns an ImageIcon from the same path.
	 *  @param imageName An image name with the file extension
	 *                    buttonEdge.g. Can.gif
	 */
	public static ImageIcon getImageIcon(int searchPathIndex , String imageName){
		// precondition test
		if (imageName == null)
			return null;
			
		// image loading
		if (searchPathIndex < searchPath.size() && searchPathIndex >= 0){
			URL url = ImageLoader.class.getResource(((String)searchPath.get(searchPathIndex)) + imageName) ;
			if (url != null){
				return new ImageIcon(url);
			} else {
				return getImageIcon(searchPathIndex - 1, imageName);
			}
		} else {
				return null;
		}
	}
	
	/** pushes the specified path to the search path
	 *  
	 *  An example for a search path file name is 'com/jgraph/pad/resources'.
	 *  
	 */
	public static void pushSearchPath(String path){
		if (path == null)
			return;
			
		if (!path.endsWith("/")){
			path = path + "/";
		}
		
		searchPath.push(path);
	}

	/** removes the searchpath at the specified index
	 */
	public static void removeSearchPath(int index){
		searchPath.remove(index);
	}

	/** pops the highest search path
	 */
	public static void popSearchPath(){
		searchPath.pop() ;
	}
}

