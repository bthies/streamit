/*
 * @(#)GPBarFactory.java	1.2 30.01.2003
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
package streamit.eclipse.grapheditor.editor.pad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.actions.AbstractActionDefault;
import streamit.eclipse.grapheditor.editor.pad.resources.ImageLoader;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.pad.resources.TranslatorConstants;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.editor.utils.gui.LocaleChangeAdapter;

/** The bar factory creates the menubars
 *  and the toolbars.
 *
 *  For Framework users:
 *  You can insert you own bar entries by register each entry
 *  at the static method <tt>addBarEntry</tt>.<br>
 *  <br>
 *  Example:
 *  <pre>
 * 	GPBarFactory.addBarEntry(new GPBarEntry("File", 15, "FileCopy"));
 *  </pre>
 *
 *
 * @see GPBarEntry
 * @author sven.luzar
 * @version 1.0
 *
 */
public class GPBarFactory implements TranslatorConstants {

	/** Main key for the menu bar
	 */
	public static final String MENUBAR = "Menubar";

	/** Main key for the toolbars
	 */
	public static final String TOOLBARS = "Toolbars";

	/** Main key for the graph popup menu
	 */
	public static final String GRAPH_POPUP = "GraphPopup";

	/** Main key for the library popup menu
	 */
	public static final String LIBRARY_POPUP = "LibraryPopup";

	/** Vector with Bar entries
	 */
	protected static Hashtable barEntries = new Hashtable();

	/** a reference to the joint graphpad
	 */
	protected GPGraphpad graphpad;

	/** creates an instance and memorizes the gaphpad
	 */
	public GPBarFactory(GPGraphpad graphpad) {
		this.graphpad = graphpad;
	}

	/**
	 * This is the hook through which all menu items are
	 * created.  It registers the result with the menuitem
	 * hashtable so that it can be fetched with getMenuItem().
	 */
	protected Component[] createMenuItem(String cmd) {
		if (cmd == null)
			return new Component[] {
		};

		String subMenu = Translator.getString(cmd + SUFFIX_MENU);
		if (subMenu != null) {
			String[] itemKeys = tokenize(cmd + SUFFIX_MENU, subMenu);
			return new Component[] { createMenu(cmd, itemKeys)};
		} else {
			Action a = getAction(cmd);

			if (a == null)
				return new Component[] {
			};

			if (a instanceof AbstractActionDefault) {
				return ((AbstractActionDefault) a).getMenuComponents();
			} else {
				JMenuItem item = new JMenuItem();
				item.setAction(a);
				fillMenuButton(item, cmd, "");
				return new Component[] { item };
			}
		}
	}

	/**
	 * Create the menubar for the app.  By default this pulls the
	 * definition of the menu from the associated resource file.
	 */
	public JMenuBar createMenubar() {
		JMenuBar mb = new JMenuBar();

		String[] menuKeys = tokenize(MENUBAR, Translator.getString(MENUBAR));
		for (int i = 0; i < menuKeys.length; i++) {
			String itemKey = Translator.getString(menuKeys[i] + SUFFIX_MENU);
			if (itemKey == null) {
				System.err.println(
					"Can't find MenuKey: '"
						+ menuKeys[i]
						+ "'. I'm ignoring the MenuKey!");
				continue;
			}
			String[] itemKeys = tokenize(menuKeys[i], itemKey);
			JMenu m = createMenu(menuKeys[i], itemKeys);
			if (m != null)
				mb.add(m);
		}
		return mb;
	}

	/** creates the popup menu for the graph
	 */
	public JPopupMenu createGraphPopupMenu() {
		return createPopupMenu(GRAPH_POPUP);
	}

	/** creates the popup menu for the library
	 */
	public JPopupMenu createLibraryPopupMenu() {
		return createPopupMenu(LIBRARY_POPUP);
	}

	/** creates a popup menu for the specified key.
	 */
	protected JPopupMenu createPopupMenu(String key) {
		JPopupMenu pop = new JPopupMenu();
		String[] itemKeys = tokenize(key, Translator.getString(key));
		for (int i = 0; i < itemKeys.length; i++) {
			if (itemKeys[i].equals("-")) {
				pop.addSeparator();
			} else {
				Component[] mi = createMenuItem(itemKeys[i]);
				for (int j = 0; j < mi.length; j++) {
					pop.add(mi[j]);
				}
			}
		}

		LocaleChangeAdapter.updateContainer(pop);

		return pop;
	}

	/** creates a menu for the specified key
	 */
	protected JMenu createMenu(String key) {

		return createMenu(key, tokenize(key, Translator.getString(key)));
	}

	/**
	 * Create a menu for the app.  By default this pulls the
	 * definition of the menu from the associated resource file.
	 */
	protected JMenu createMenu(String key, String[] itemKeys) {
		JMenu menu = new JMenu();
		menu.setName(key);
		for (int i = 0; i < itemKeys.length; i++) {
			if (itemKeys[i].equals("-")) {
				menu.addSeparator();
			} else {
				Component[] mi = createMenuItem(itemKeys[i]);
				for (int j = 0; j < mi.length; j++) {
					if (mi[j] != null)
						menu.add(mi[j]);
				}
			}
		}

		ImageIcon icon =
			ImageLoader.getImageIcon(Translator.getString(key + SUFFIX_IMAGE));
		if (icon != null) {
			menu.setHorizontalTextPosition(JButton.RIGHT);
			menu.setIcon(icon);
		}

		// set mnemonic for the JMenus
		String mnemonic = Translator.getString(key + SUFFIX_MNEMONIC);
		if (mnemonic != null && mnemonic.length() > 0)
			menu.setMnemonic(mnemonic.toCharArray()[0]);

		return menu;
	}

	/** creates a panel with the toolbars into.
	 *  For each toolbar a panel was created.
	 *  The inner panel is the return value.
	 *  The outside panel is the parameter.
	 *
	 *  @param toolBarMainPanel The outside panel.
	 *  @return The inner panel
	 */
	public JPanel createToolBars(JPanel toolBarMainPanel) {
		String toolBarsKey = Translator.getString(TOOLBARS);
		if (toolBarsKey == null) {
			System.err.println(
				"Can't find Key: 'toolbars'. I'm ignoring the MenuKey!");
			return toolBarMainPanel;
		}

		String[] toolBars = tokenize(TOOLBARS, toolBarsKey);
		//, JTabbedPane.SCROLL_TAB_LAYOUT); // JDK 1.3
		JPanel innerPanel = toolBarMainPanel;
		for (int i = 0; i < toolBars.length; i++) {
			String label = Translator.getString(toolBars[i] + SUFFIX_LABEL);
			innerPanel.add(
				createToolbar(toolBars[i], label),
				BorderLayout.NORTH);
			JPanel oldInnerPanel = innerPanel;
			innerPanel = new JPanel(new BorderLayout());
			oldInnerPanel.add(innerPanel, BorderLayout.CENTER);
		}
		return innerPanel;
	}

	/**
	 * Create the toolbar.  By default this reads the
	 * resource file for the definition of the toolbar.
	 */
	protected Component createToolbar(String key, String label) {
		JToolBar toolbar = new JToolBar(label);
		//toolbar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		//toolbar.setFloatable(false);
		String toolKey = Translator.getString(key);
		if (toolKey == null) {
			System.err.println(
				"Can't find ToolBarKey: '"
					+ toolKey
					+ "'. I'm ignoring the MenuKey!");
			return toolbar;
		}
		String[] toolKeys = tokenize(key, toolKey);
		for (int i = 0; i < toolKeys.length; i++) {
			if (toolKeys[i].equals("-")) {
				toolbar.add(Box.createHorizontalStrut(5));
			} else {
				Component[] comps = createTool(toolKeys[i]);
				for (int j = 0; j < comps.length; j++) {
					toolbar.add(comps[j]);
				}
			}
		}
		toolbar.add(Box.createHorizontalGlue());
		return toolbar;
	}

	/**
	 * Hook through which every toolbar item is created.
	 */
	protected Component[] createTool(String key) {
		return createToolbarButton(key);
	}

	/**
	 * Create a button to go inside of the toolbar.  By default this
	 * will load an image resource.  The image filename is relative to
	 * the classpath (including the '.' directory if its a part of the
	 * classpath), and may either be in a JAR file or a separate file.
	 *
	 * @param key The key in the resource file to serve as the basis
	 *  of lookups.
	 */
	protected Component[] createToolbarButton(String key) {
		Action a = graphpad.getCurrentActionMap().get(key);

		if (a instanceof AbstractActionDefault) {
			return ((AbstractActionDefault) a).getToolComponents();
		} else {
			JButton item = new JButton();
			item.setAction(a);
			fillToolbarButton(item, key, "");
			return new Component[] { item };
		}

	}

	/** returns the action for the cmd key.
	 *  The method inspects the action map at the graph pad
	 *  to get the correct action.
	 */
	protected Action getAction(String cmd) {
		Action a = null;
		if (cmd != null) {
			a = graphpad.getCurrentActionMap().get(cmd);
		}
		if (a != null)
			return a;
		else {
			// Check for Plugin
			String className = Translator.getString(cmd + "Plugin");
			System.out.println("cmd=" + cmd + " className=" + className);
			if (className != null) {
				try {
					final GPPlugin plugin =
						(GPPlugin) getClass()
							.getClassLoader()
							.loadClass(className)
							.newInstance();
					if (plugin != null) {
						Action action = new AbstractAction() {
							public void actionPerformed(ActionEvent e) {
								plugin.execute(graphpad);
							}
						};
						graphpad.getActionMap().put(cmd, action);
						return action;
					} else {
						System.err.println(
							"Can't find plugin Class: " + className);
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return null;
	}

	/** fills the abstract button with values from
	 *  the properties files.
	 *
	 */
	public static AbstractButton fillMenuButton(
		AbstractButton button,
		String key,
		String actionCommand) {
			button.putClientProperty(
				LocaleChangeAdapter.DONT_SET_TOOL_TIP_TEXT,
				new Boolean(true));
		return fillAbstractButton(button, key, actionCommand);
	}

	/** fills the abstract button with values from
	 *  the properties files.
	 *
	 */
	public static AbstractButton fillToolbarButton(
		AbstractButton button,
		String key,
		String actionCommand) {
		button.putClientProperty(
			LocaleChangeAdapter.DONT_SET_MNEMONIC,
			new Boolean(true));
		button.putClientProperty(
			LocaleChangeAdapter.SET_TEXT_IF_ICON_NOT_AVAILABLE,
			new Boolean(true));
		return fillAbstractButton(button, key, actionCommand);
	}

	/**
	 *  The method fills the AbstractButton with
	 *  the localized label, the image, the accelerator
	 *  and the mnemonic.
	 *
	 */
	public static AbstractButton fillAbstractButton(
		AbstractButton button,
		String key,
		String actionCommand) {
			
		String label = null;
		String tooltip = null;
		ImageIcon icon = null;
		if (key != null) {
			button.setName(key);
			//LocaleChangeAdapter.updateComponent(button);

			/*
			icon = ImageLoader.getImageIcon(
				Translator.getString(key + SUFFIX_IMAGE));
			if (icon != null) {
				button.setHorizontalTextPosition(JButton.RIGHT);
				button.setIcon(icon);
			}
			
			label = Translator.getString(key + SUFFIX_LABEL);
			if (label != null)
				button.setText(label);
			
			tooltip = Translator.getString(key + SUFFIX_TOOL_TIP_TEXT);
			if (tooltip==null)
				tooltip = label;
			if (tooltip!= null && (button instanceof JButton || button instanceof JToggleButton))
				button.setToolTipText(tooltip);
			
			if (icon != null && !setText)
				button.setText(null);
			
			// important that buttons in toolbars do not get 
			// mnemonics or accelerators as they compete with 
			// their menuitem counterparts
			if (button instanceof JMenuItem){
				String accel = Translator.getString(key + SUFFIX_ACCELERATOR);
				if (accel != null && accel.length() > 0) {
					KeyStroke keyStroke = KeyStroke.getKeyStroke(accel);
					((JMenuItem) button).setAccelerator(keyStroke);
				}
				String mnemonic = Translator.getString(key + SUFFIX_MNEMONIC);
				if (mnemonic != null && mnemonic.length() > 0)
					button.setMnemonic(mnemonic.toCharArray()[0]);
			}
			*/
		}

		/*
		if (icon == null && label == null)
			button.setText(key);
		*/

		button.setActionCommand(actionCommand);

		return button;
	}

	/** Tokenizes the value for the key and integrates
	 *  bar entries.
	 *
	 *
	 *
	 *  @see #integrateBarEntries(String, String[])
	 */
	protected String[] tokenize(String key, String value) {
		String[] values = Utilities.tokenize(value);

		values = integrateBarEntries(key, values);

		return values;
	}
	/** Integrates bar entries, if available, for the key.
	 *  If the position is out of the array the method
	 *  ignores the bar entry.
	 *
	 * 	@param key Current key for the values
	 *  @param values The tokenized values for the key.
	 *  @see #addBarEntry
	 *
	 */
	protected String[] integrateBarEntries(String key, String[] values) {
		Enumeration enum;

		// get the bar entries for the key
		Vector vector4BarKey = (Vector) barEntries.get(key);

		// if there is no bar entry return
		if (vector4BarKey == null || vector4BarKey.size() == 0)
			return values;

		// build a mutable list with the old values
		Vector listValues = new Vector();
		for (int i = 0; i < values.length; i++) {
			listValues.add(values[i]);
		}

		// insert the bar entries
		enum = vector4BarKey.elements();
		while (enum.hasMoreElements()) {
			GPBarEntry barEntry = (GPBarEntry) enum.nextElement();
			try {
				listValues.insertElementAt(
					barEntry.getBarValue(),
					barEntry.getPos());
			} catch (Exception ex) {
				System.err.println(
					"Error while integrating Bar Entry" + barEntry);
				System.err.println(ex.getMessage());
			}
		}

		// build the new array with the old values and the
		// bar entries
		String[] newValues = new String[listValues.size()];
		for (int i = 0; i < listValues.size(); i++) {
			newValues[i] = (String) listValues.get(i);
		}
		return newValues;

	}

	/** Here you can add your own bar entries.
	 *
	 */
	public static void addBarEntry(GPBarEntry entry) {
		Vector vector4BarKey = (Vector) barEntries.get(entry.getBarKey());
		if (vector4BarKey == null) {
			vector4BarKey = new Vector();
			barEntries.put(entry.getBarKey(), vector4BarKey);
		}

		vector4BarKey.add(entry);
	}

	/** Here you can remove your own bar entries.
	 */
	public static void removeBarEntry(GPBarEntry entry) {
		Vector vector4BarKey = (Vector) barEntries.get(entry.getBarKey());
		if (vector4BarKey == null) {
			return;
		}

		vector4BarKey.remove(entry);
	}

}
