/*
 * @(#)Version.java	1.0 28/01/02
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

import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Reads the Version from the file version.properies
 * 
 * @author sven.luzar
 *
 */
public class Version {
	/** The Resource Bundle handles the version file
	 */
	private static ResourceBundle versionResourceBundle;

	/** The current version number
	 */
	private static String version = "";

	/** Initializer loads the ResourceBundle and sets the const value VERSION
	 * 
	 */
	static {
		try {
			InputStream is =
				Version.class.getResourceAsStream("version.properties");
			if (is != null) {
				versionResourceBundle = new PropertyResourceBundle(is);
				String rc =
					versionResourceBundle.getString("version.number.rc");
				if (rc != null) {
					if (rc.equals("0") || rc.equals("")) {
						rc = "";
					} else {
						if (rc.equals("999")) {
							rc = " Final" ;
						} else {
							rc = "-RC" + rc;							
						}
					}
				} else {
					rc = "";
				}
				String build =
					versionResourceBundle.getString("version.build");
				if (build != null && !(build.equals("0")) && !(build.equals("1")))
				    build = " (build "+build+")";
				else
				    build = "";
				version =
					versionResourceBundle.getString("version.number.1")
						+ "."
						+ versionResourceBundle.getString("version.number.2")
				                + "."
						+ versionResourceBundle.getString("version.number.3")
						+ rc
				                + build;
			}
		} catch (java.io.IOException e) {
			System.err.println(e.getMessage());
			version = "{File version.properties not available.}";
		}
	}

	/** Returns the current version number
	 */
	public static String getVersion() {
		return version;
	}

}
