/*
 * @(#)ImageLoader.java	1.0 23/01/02
 *
 * Copyright (C) 2003 Sven Luzar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */
package streamit.eclipse.grapheditor.editor.pad.resources;

import java.awt.Image;
import java.net.*;
import java.util.*;

import javax.swing.ImageIcon;

/**Loader for the resource images. The class
 * uses the getResource Method to get the
 * Resource from the relative path.
 * 
 * @author sven.luzar
 * @see java.lang.ClassLoader#getResource
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
		pushSearchPath("/streamit/eclipse/grapheditor/editor/pad/resources"); 
		//pushSearchPath("/org/jgraph/pad/resources");
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
