/*
 * @(#)TranslatorConstants.java	1.2 02.02.2003
 *
 * Copyright (C) 2003 sven.luzar
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
package streamit.eclipse.grapheditor.editor.pad.resources;

/** Constant values for a part of the 
 *  language key
 * 
 * @author sven.luzar
 * @version 1.0
 * @see org.jgraph.pad.resources.Translator
 *
 */
public interface TranslatorConstants {

	/**
	 * Prefix for component requests
	 * 
	 */
	public static final String PREFIX_COMPONENT = "Component.";
	
	/**
	 * Prefix for information message requests
	 * 
	 */
	public static final String PREFIX_MESSAGE_INFORMATION = "Message.Information.";
	
	/**
	 * Prefix for warning message requests
	 * 
	 */
	public static final String PREFIX_MESSAGE_WARING = "Message.Warning.";	

	/**
	 * Prefix for error message requests
	 * 
	 */
	public static final String PREFIX_MESSAGE_ERROR = "Message.Error.";	

	/**
	 * Suffix applied to the key used in resource file
	 * lookups for an image.
	 */
	public static final String SUFFIX_IMAGE = ".Image";

	/**
	 * Suffix applied to the key used in resource file
	 * lookups for a label.
	 */
	public static final String SUFFIX_LABEL = ".Label";


	/**
	 * Suffix applied to the key used in resource file
	 * lookups for a menuitem (instead of action)
	 */
	public static final String SUFFIX_ACCELERATOR = ".Accelerator";

	/**
	 * Suffix applied to the key used in resource file
	 * lookups for a menuitem (instead of action)
	 */
	public static final String SUFFIX_MNEMONIC = ".Mnemonic";

	/**
	 * Suffix applied to the key used in resource file
	 * lookups for tooltip text.
	 */
	public static final String SUFFIX_TOOL_TIP_TEXT = ".ToolTipText";

	/**
	 * Suffix applied to the key used in resource file
	 * lookups for menu.
	 */
	public static final String SUFFIX_ACTION = ".Action";
	
	/**
	 * Suffix applied to the key used in resource file
	 * lookups for a submenu
	 */
	public static final String SUFFIX_MENU = ".Menu";

	/**
	 * suffix for the text key
	 */
	public static final String SUFFIX_TEXT = ".Text";

	/**
	 * suffix for the title key
	 */
	public static final String SUFFIX_TITLE = ".Title";
	/**
	 * suffix for the icon key
	 */
	public static final String SUFFIX_ICON = ".Icon";

}
