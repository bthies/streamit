/*
 * @(#)LocaleChangeAdapter.java 1.0 04.08.2003
 *
 * Copyright (C) 2003 sven_luzar
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
package streamit.eclipse.grapheditor.editor.utils.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Label;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import streamit.eclipse.grapheditor.editor.pad.resources.ImageLoader;
import streamit.eclipse.grapheditor.editor.pad.resources.LocaleChangeEvent;
import streamit.eclipse.grapheditor.editor.pad.resources.LocaleChangeListener;
import streamit.eclipse.grapheditor.editor.pad.resources.ProperNameProvider;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.pad.resources.TranslatorConstants;

/** The adapter is a listener for local change events.
 *  If you change the locale at the translator then this
 *  adapter will update all registered GUI Elements.
 * 
 *  To register a GUI Element you can use the 
 *  {@link #addContainer(Container)} method.
 *  To unregister a GUI Element you can use the 
 *  {@link #removeContainer(Container)} method.
 * 
 *  A typical frame implementation can look like this:
 *  
 *  <pre>
 *  public class MyFrame extend JFrame {
 * 
 * 		public MyFrame(){
 * 			setName("MyFrame");
 * 			JButton b = new JButton();
 * 			b.setName("OK");
 * 			getContentPane().add(b);
 * 
 * 			// after con catting the whole GUI tree
 * 			// register the frame for locale
 * 			// events
 * 			// The LocalChangeAdapter will update
 * 			// all labels and co automatically
 * 			LocaleChangeAdapter.addContainer(this);
 * 			pack();
 * 			setVisible(true);
 * 		}
 * 
 * 		public void dispose(){
 * 			super.dispose();
 * 			// unregister the locale change adapter
 * 			LocaleChangeAdapter.removeContainer(this);
 * 		}
 *  }
 *  </pre> 
 *  
 *  Furthermore this class is a proper name provider for 
 *  proper names from the swing gui. For example the 
 *  look and feel names are proper names and we do not
 *  need to add the look and feel names at each 
 *  property language file. This class contains
 *  this proper names. 
 *
 * @author <a href="mailto:Sven.Luzar@web.de">Sven luzar</a>
 * @version 1.0
 */

public class LocaleChangeAdapter
	implements LocaleChangeListener, ProperNameProvider, TranslatorConstants {

	/** Key for the special jcomponent client property.
	 *  If the JComponent contains a client property with this key
	 *  and a true Boolean object, then the local change adapter
	 *  will display the Text
	 *  only if an icon is not available
	 * 
	 */
	public static final String SET_TEXT_IF_ICON_NOT_AVAILABLE =
		"SetTextIfIconNotAvailable";

	/** Key for the special jcomponent client property.
	 *  If the JComponent contains a client property with this key
	 *  and a true Boolean object, then the local change adapter
	 *  will not set the ToolTipText
	 * 
	 */
	public static final String DONT_SET_TOOL_TIP_TEXT =
		"DontSetToolTipText";

	/** Key for the special jcomponent client property.
	 *  If the JComponent contains a client property with this key
	 *  and a true Boolean object, then the local change adapter
	 *  will not set the mnemonic
	 * 
	 */
	public static final String DONT_SET_MNEMONIC = 
		"DontSetMnemonic";

	/** contains the proper names for the swing GUI
	 * 
	 */
	Hashtable properNames = new Hashtable();

	/** For example if we have got 
	 *  a TabbedPane with multiple titles we
	 *  need to number the titles.
	 *  This number is formated with 
	 *  the pattern "000".
	 *  
	 */
	static final NumberFormat numberFormat = new DecimalFormat("000");

	/**
	 *  Creates a new local change adapter and
	 *  registers itself at the translator
	 *  for a locale change listener and for a
	 *  proper name provider. 
	 *
	 */
	public LocaleChangeAdapter() {
		Translator.addLocaleChangeListener(this);

		// fill properNames
		UIManager.LookAndFeelInfo[] lafi = UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < lafi.length; i++) {
			if (lafi[i].getName() != null) {
				properNames.put(
					PREFIX_COMPONENT + lafi[i].getName() + SUFFIX_TEXT,
					lafi[i].getName());
				properNames.put(
					PREFIX_COMPONENT + lafi[i].getName() + SUFFIX_TOOL_TIP_TEXT,
					lafi[i].getName());
				properNames.put(
					PREFIX_COMPONENT + lafi[i].getName() + SUFFIX_MNEMONIC,
					lafi[i].getName());

				if (lafi[i].getClassName() != null) {
					properNames.put(
						PREFIX_COMPONENT + lafi[i].getClassName() + SUFFIX_TEXT,
						lafi[i].getName());
					properNames.put(
						PREFIX_COMPONENT
							+ lafi[i].getClassName()
							+ SUFFIX_TOOL_TIP_TEXT,
						lafi[i].getName());
					properNames.put(
						PREFIX_COMPONENT
							+ lafi[i].getClassName()
							+ SUFFIX_MNEMONIC,
						lafi[i].getName());
				}
			}
		}
		Translator.getDefaultResourceBundle().addProperNameProvider(this);

	}

	/** A reference to the default locale change adapter
	 * 
	 */
	protected static LocaleChangeAdapter localeChangeAdapter =
		new LocaleChangeAdapter();

	/** returns the default local change adapter
	 * 
	 * @return the default adapter
	 */
	protected static LocaleChangeAdapter getLocaleChangeAdapter() {
		return localeChangeAdapter;
	}

	/** vector with all registered containers
	 * 
	 */
	protected static Vector containers = new Vector();

	/** Adds a container to the control of the local change adapter
	 * 
	 * @param comp the specified container
	 */
	public static void addContainer(Container comp) {
		containers.add(comp);
		updateContainer(comp);
	}
	/** Removes a container from the control of the local change adapter
	 * 
	 * @param comp the specified container
	 */
	public static void removeContainer(Container comp) {
		containers.remove(comp);
	}

	/** Will be called from the translator 
	 *  if a locale was changed
	 *  
	 */
	public void localeChanged(LocaleChangeEvent e) {
		Vector copy;
		synchronized (containers) {
			copy = (Vector) containers.clone();
		}
		Enumeration oEnum = copy.elements();
		while (oEnum.hasMoreElements()) {
			updateContainer((Container) oEnum.nextElement());
		}
	}

	/** This method will be called from the localeChanged
	 *  method or it will be called recursively.
	 *  The method will update the container and
	 *  all children with the new locale values. 
	 * 
	 * @param oComp the concerning container
	 * @see #localeChanged(LocaleChangeEvent)
	 */
	public static void updateContainer(Container oComp) {
		if (oComp == null)
			return;

		updateComponent(oComp);

		if (oComp instanceof JFrame) {
			updateContainer((Container) (((JFrame) oComp).getJMenuBar()));
			updateContainer((Container) (((JFrame) oComp).getContentPane()));
		}
		// procedure for the childs
		for (int i = 0; i < oComp.getComponentCount(); i++) {
			Component oChild = oComp.getComponent(i);

			if (oChild instanceof Container) {
				updateContainer((Container) oChild);
			} else {
				updateComponent(oChild);
			}
		}
		// procedure for the menu items
		if (oComp instanceof JMenu) {
			JMenu menu = (JMenu) oComp;
			for (int i = 0; i < menu.getItemCount(); i++) {
				Component oChild = menu.getItem(i);
				if (oChild instanceof Container) {
					updateContainer((Container) oChild);
				} else {
					updateComponent(oChild);
				}
			}
		}
	}

	/** This method will be called from the updateContainer
	 *  method.
	 *  The method will update the component 
	 *  with the new locale values. 
	 * 
	 * @param oComp the concerning component
	 * @see #updateContainer(Container)
	 */
	public static void updateComponent(Component comp) {
		if (comp == null || comp.getName() == null)
			return;

		// single titles
		try {
			if (comp instanceof Frame
				|| comp instanceof Dialog
				|| comp instanceof JInternalFrame) {
				String title =
					Translator.getString(
						PREFIX_COMPONENT + comp.getName() + SUFFIX_TITLE);
				if (title != null) {
					if (comp instanceof Frame) {
						((Frame) comp).setTitle(title);
					}
					if (comp instanceof Dialog) {
						((Dialog) comp).setTitle(title);
					}
					if (comp instanceof JInternalFrame) {
						((JInternalFrame) comp).setTitle(title);
					}
				}
			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

		// single tool tip text
		// don't show the tool tip at the menu
		// it's not really nice :-)
		try {

			if (comp instanceof JComponent) {

				String toolTipText =
					Translator.getString(
						PREFIX_COMPONENT
							+ comp.getName()
							+ SUFFIX_TOOL_TIP_TEXT);

				if (toolTipText == null)
					toolTipText =
						Translator.getString(
							PREFIX_COMPONENT + comp.getName() + SUFFIX_TEXT);

				Boolean dontDisplayToolTipText =
					(Boolean) ((JComponent) comp).getClientProperty(
						LocaleChangeAdapter.DONT_SET_TOOL_TIP_TEXT);

				if (dontDisplayToolTipText != null
					&& dontDisplayToolTipText.booleanValue()) {
					toolTipText = null;
				}

				if (toolTipText != null) {
					((JComponent) comp).setToolTipText(toolTipText);
				}

			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

		// set mnemonic
		try {

			if (comp instanceof AbstractButton) {
				String mnemonic =
					Translator.getString(
						PREFIX_COMPONENT + comp.getName() + SUFFIX_MNEMONIC);
						
				Boolean noMnemonic =
					(Boolean) ((JComponent) comp).getClientProperty(
						LocaleChangeAdapter.DONT_SET_MNEMONIC);

				if (noMnemonic != null
					&& noMnemonic.booleanValue()) {
					mnemonic = null;
				}
										
				if (mnemonic != null && mnemonic.length() > 0) {
					((AbstractButton) comp).setMnemonic(
						mnemonic.toCharArray()[0]);
				}
			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

		// set Accelerator
		try {

			if (comp instanceof JMenuItem) {
				String accelerator =
					Translator.getString(
						PREFIX_COMPONENT + comp.getName() + SUFFIX_ACCELERATOR);
				if (accelerator != null) {
					if (accelerator != null && accelerator.length() > 0) {
						KeyStroke keyStroke =
							KeyStroke.getKeyStroke(accelerator);
						((JMenuItem) comp).setAccelerator(keyStroke);
					}
				}
			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

		// set the icon
		boolean iconAvailable = false;
		try {

			if (comp instanceof JOptionPane
				|| comp instanceof AbstractButton
				|| comp instanceof JLabel) {

				// test: is an old text available?				
				//boolean update = false;
				Icon icon =
					ImageLoader.getImageIcon(
						Translator.getString(
							PREFIX_COMPONENT + comp.getName() + SUFFIX_ICON));
				if (icon != null) {
					iconAvailable = true;
					if (comp instanceof JOptionPane) {
						((JOptionPane) comp).setIcon(icon);
					}
					if (comp instanceof AbstractButton) {
						((AbstractButton) comp).setIcon(icon);
					}
					if (comp instanceof JLabel) {
						((JLabel) comp).setIcon(icon);
					}
				}
			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

		// set the text for 
		// non text field components
		try {

			if (comp instanceof Label
				|| comp instanceof AbstractButton
				|| comp instanceof JLabel) {

				// update the text only if 
				// the text was set before
				String text =
					Translator.getString(
						PREFIX_COMPONENT + comp.getName() + SUFFIX_TEXT);
				if (text != null) {
					Boolean displayTextIfIconNotAvailable = null;

					if (comp instanceof JComponent) {
						displayTextIfIconNotAvailable =
							(Boolean) ((JComponent) comp).getClientProperty(
								LocaleChangeAdapter
									.SET_TEXT_IF_ICON_NOT_AVAILABLE);
					}

					if (displayTextIfIconNotAvailable != null
						&& displayTextIfIconNotAvailable.booleanValue()
						&& iconAvailable == true) {
						text = null;
					}

					if (comp instanceof Label) {
						((Label) comp).setText(text);
					}
					if (comp instanceof AbstractButton) {
						((AbstractButton) comp).setText(text);
					}
					if (comp instanceof JLabel) {
						((JLabel) comp).setText(text);
					}

				}
			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

		// multiple titles && tool tip texts
		try {

			if (comp instanceof JTabbedPane) {
				JTabbedPane jtp = (JTabbedPane) comp;
				for (int i = 0; i < jtp.getTabCount(); i++) {
					String title =
						Translator.getString(
							PREFIX_COMPONENT
								+ comp.getName()
								+ SUFFIX_TITLE
								+ numberFormat.format(i));
					if (title != null)
						jtp.setTitleAt(i, title);
					String toolTipText =
						Translator.getString(
							PREFIX_COMPONENT
								+ comp.getName()
								+ SUFFIX_TOOL_TIP_TEXT
								+ numberFormat.format(i));
					if (toolTipText != null)
						jtp.setToolTipTextAt(i, toolTipText);

					String mnemonic =
						Translator.getString(
							PREFIX_COMPONENT
								+ comp.getName()
								+ SUFFIX_MNEMONIC
								+ numberFormat.format(i));
					if (mnemonic != null && mnemonic.length() > 0) {
						((JTabbedPane) comp).setMnemonicAt(
							i,
							mnemonic.toCharArray()[0]);
					}
				}
			}
		} catch (Exception ex) {
			// print each exception
			// and do nothing more
			System.err.println(ex);
		}

	}

	/** returns the keys for the proper names
	 * 
	 */
	public Enumeration getKeys() {
		return properNames.keys();
	}

	/** returns the value for a proper name key
	 * 
	 */
	public String getString(String key) {
		return (String) properNames.get(key);
	}
}