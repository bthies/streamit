/*
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
import java.util.*;

public class UserProperties extends Properties {

	//protected static EnvProperties envProps = new EnvProperties();
	protected static Vector instances = new Vector();
	protected String nameSpace;
	protected File file;


	/**
	 * Constructor for UserProperties
	 */
	private UserProperties() {
	}


	/**
	 * Constructor for UserProperties
	 *
	 * @param nameSpace Description of Parameter
	 */
	private UserProperties(String nameSpace) {
		try {
		String USERHOME = System.getProperty("user.home");
		//envProps.getProperty("USERPROFILE");
		if (USERHOME == null) {
			USERHOME = "";
		}
		this.nameSpace = USERHOME + File.separator + nameSpace;
		String fullpath = this.nameSpace + File.separator + "user.properties";
		File file = new File(fullpath);

		// check for properties file
		if (!file.exists() || !file.isFile()) {
			try {
				Utilities.createDirectoryRecursively(new File(this.nameSpace));
				file.createNewFile();
			}
			catch (IOException e) {
				System.out.println(e);
				// throw an unchecked exception, and then create default
			}
		}
		// else if (!file.canRead() || !file.canWrite()) {
			//try to remedy, else create default
		// }

		this.file = file;

		// init properties
		this.clear();
		Utilities.readPropertiesFromFile((Frame)null, file);
		} catch (Exception ex){
			System.out.println(ex.toString() );
		}
	}


	/**
	 * Multi-Singleton instance factory method.
	 *
	 * @param nameSpace nameSpace is the name of the directory that user properties will be stored.
	 *      This acts as a 'namespace' by which sets of properties can collectively be stored. For each
	 *      new nameSpace, a new directory (prepended with '.') is created and properties stored in a
	 *      file named user.properties. For example, if 'example' is passed as nameSpace, the following
	 *      directory would be created C:\\Documents and Settings\\userlogin\\.testapp\\user.properties
	 *      (Win2k).
	 * @return the UserProperties instance
	 */
	public final static UserProperties getInstance(String nameSpace) {
		// check for existing instance with same nameSpace
		for (int i = 0; i < instances.size(); i++) {
			UserProperties instance = (UserProperties) instances.elementAt(i);
			if (nameSpace.equals(instance.nameSpace)) {
				return instance;
			}
		}

		// else, if not found, create
		UserProperties newInstance = new UserProperties(nameSpace);
		instances.add(newInstance);
		return newInstance;
	}


	/**
	 * Description of the Method
	 */
	public static void saveAll() {
		for (int i = 0; i < instances.size(); i++) {
			UserProperties instance = (UserProperties) instances.elementAt(i);
			instance.save();
		}
	}


	/**
	 * Sets the {3} attribute of the UserProperties object
	 *
	 * @param key The new {3} value
	 * @param buttonRectangle The new {3} value
	 */
	public void setRect(String key, Rectangle r) {
		String val = r.getX() + "," + r.getY() + "," + r.getWidth() + "," + r.getHeight();
		this.setProperty(key, val);
	}


	/**
	 * Gets the {3} attribute of the UserProperties object
	 *
	 * @param key Description of Parameter
	 * @return The {3} value
	 */
	public Rectangle getRect(String key) {
		int x = 0;
		int y = 0;
		int width = 0;
		int height = 0;
		String rect = this.getProperty(key);
		try {
			StringTokenizer st = new StringTokenizer(rect, ",");
			     x = (int)Double.parseDouble(st.nextToken());
			     y = (int)Double.parseDouble(st.nextToken());
			 width = (int)Double.parseDouble(st.nextToken());
			height = (int)Double.parseDouble(st.nextToken());
		}
		catch (Exception e) {
			return null;
		}
		return new Rectangle(x, y, width, height);
	}


	/**
	 * Description of the Method
	 */
	public void save() {
		Utilities.writePropertiesToFile((Frame)null, this, file,  "---User Properties--- output from UserProperties.java");
	}
}
