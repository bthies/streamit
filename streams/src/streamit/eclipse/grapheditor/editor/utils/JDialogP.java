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
import javax.swing.*;

public abstract class JDialogP extends JDialog {
	// set default property value
	// all properties should be prefixed with instanceID in order to
	// ensure uniqueness across properties in the shared pool of user properties
	String property_bounds = getInstanceID() + ".bounds";
	String nameSpace = "." + getNameSpace();
	UserProperties guiProperties = UserProperties.getInstance(nameSpace);
	boolean hasBeenVisibleAtLeastOnce = false;


	/**
	 * @.todo persist iconify/maximized attribute
	 */

	/**
	 * Constructor for JDialogP
	 *
	 */
	public JDialogP() {
		super();
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 */
	public JDialogP(Dialog owner) {
		super(owner);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 * @param modal Description of Parameter
	 */
	public JDialogP(Dialog owner, boolean modal) {
		super(owner, modal);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 * @param title Description of Parameter
	 */
	public JDialogP(Dialog owner, String title) {
		super(owner, title);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 * @param title Description of Parameter
	 * @param modal Description of Parameter
	 */
	public JDialogP(Dialog owner, String title, boolean modal) {
		super(owner, title, modal);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 */
	public JDialogP(Frame owner) {
		super(owner);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 * @param modal Description of Parameter
	 */
	public JDialogP(Frame owner, boolean modal) {
		super(owner, modal);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 * @param title Description of Parameter
	 */
	public JDialogP(Frame owner, String title) {
		super(owner, title);
	}


	/**
	 * Constructor for JDialogP
	 *
	 * @param owner Description of Parameter
	 * @param title Description of Parameter
	 * @param modal Description of Parameter
	 */
	public JDialogP(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
	}


	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @param x The new {3} value
	 * @param y The new {3} value
	 * @param width The new {3} value
	 * @param height The new {3} value
	 */
	public void setBounds(int x, int y, int width, int height) {
		// if persisted, and already displayed at least once, then allow programmatic change
		if (hasBeenVisibleAtLeastOnce) {
			super.setBounds(x, y, width, height);
			guiProperties.setRect(property_bounds, this.getBounds());
		}
		// else, if not been visible at least once, and not yet ever persisted then allow programmatic change to set initial location
		else if (guiProperties == null || guiProperties.getRect(property_bounds) == null) {
			super.setBounds(x, y, width, height);
		}
		// else, ignore request and set to the previously persisted value
		else {
			Rectangle r = guiProperties.getRect(property_bounds);
			super.setBounds(r.x, r.y, r.width, r.height);
		}
	}


	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @param buttonRectangle The new {3} value
	 */
	public void setBounds(Rectangle r) {
		this.setBounds(r.x, r.y, r.width, r.height);
	}


	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @param x The new {3} value
	 * @param y The new {3} value
	 */
	public void setLocation(int x, int y) {
		// if persisted, and already displayed at least once, then allow programmatic move
		if (hasBeenVisibleAtLeastOnce) {
			super.setLocation(x, y);
			guiProperties.setRect(property_bounds, this.getBounds());
		}
		// else, if not been visible at least once, and not yet ever persisted then allow programmatic move to set initial location
		else if (guiProperties == null || guiProperties.getRect(property_bounds) == null) {
			super.setLocation(x, y);
		}
		// else, ignore request and set to the previously persisted value
		else {
			Rectangle r = guiProperties.getRect(property_bounds);
			super.setLocation(r.x, r.y);
		}
	}


	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @param p The new {3} value
	 */
	public void setLocation(Point p) {
		this.setLocation(p.x, p.y);
	}


	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @param d The new {3} value
	 */
	public void setSize(Dimension d) {
		this.setSize(d.width, d.height);
	}


	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @param width The new {3} value
	 * @param height The new {3} value
	 */
	public void setSize(int width, int height) {
		// if persisted, and already displayed at least once, then allow programmatic resize
		if (hasBeenVisibleAtLeastOnce) {
			super.setSize(width, height);
			guiProperties.setRect(property_bounds, this.getBounds());
		}
		// else, if not been visible at least once, and not yet ever persisted then allow programmatic resize to set initial location
		else if (guiProperties == null || guiProperties.getRect(property_bounds) == null) {
			super.setSize(width, height);
		}
		// else, ignore request and set to the previously persisted value
		else {
			Rectangle r = guiProperties.getRect(property_bounds);
			super.setSize(r.width, r.height);
		}
	}


	// changing forces a recreation with new directory
	// prepends '.'
	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @return The {3} value
	 */
	public abstract String getNameSpace();


	// prefix to key, doesn'buttonText create a new directory
	// automatically prepends full class name to ensure uniqueness
	// this.getClass().getName()
	// recommend implementors implement a setInstanceID() method if unique instance location is desired.  as it currently stands every instance of the implemented class will share the persisted bounds.
	/**
	 * Sets the {3} attribute of the JDialogP object
	 *
	 * @return The {3} value
	 */
	public abstract String getInstanceID();


	/**
	 * Description of the Method
	 */
	public void show() {
		init();

		if (hasBeenVisibleAtLeastOnce == false) {
			// set default location to be centered
			// this'buttonSelect implementation of setBounds will deal with checking for persisted value
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (screenSize.width - this.getWidth()) / 2;
			int y = (screenSize.height - this.getHeight()) / 2;
			this.setBounds(x, y, this.getWidth(), this.getHeight());
		}

		hasBeenVisibleAtLeastOnce = true;
		super.show();
	}


	/**
	 * Description of the Method
	 *
	 * @param x Description of Parameter
	 * @param y Description of Parameter
	 * @param width Description of Parameter
	 * @param height Description of Parameter
	 */
	// not needed internal setBounds(x, y, width, height)
	// was called
	/*
	public void reshape(int x, int y, int width, int height) {
		// if persisted, and already displayed at least once, then allow programmatic change
		if (hasBeenVisibleAtLeastOnce) {
			super.reshape(x, y, width, height);
			guiProperties.setRect(property_bounds, this.getBounds());
		}
		// else, if not been visible at least once, and not yet ever persisted then allow programmatic change to set initial location
		else if (guiProperties == null || guiProperties.getRect(property_bounds) == null) {
			super.reshape(x, y, width, height);
		}
		// else, ignore request and set to the previously persisted value
		else {
			Rectangle buttonRectangle = guiProperties.getRect(property_bounds);
			super.reshape(buttonRectangle.x, buttonRectangle.y, buttonRectangle.width, buttonRectangle.height);
		}
	}*/


	/**
	 * Description of the Method
	 *
	 * @param x Description of Parameter
	 * @param y Description of Parameter
	 */
	// not needed internal setBounds(x, y, width, height)
	// was called
	/*
	public void move(int x, int y) {
//		this.setLocation(x, y);
		// if persisted, and already displayed at least once, then allow programmatic move
		if (hasBeenVisibleAtLeastOnce) {
			super.move(x, y);
			guiProperties.setRect(property_bounds, this.getBounds());
		}
		// else, if not been visible at least once, and not yet ever persisted then allow programmatic move to set initial location
		else if (guiProperties == null || guiProperties.getRect(property_bounds) == null) {
			super.move(x, y);
		}
		// else, ignore request and set to the previously persisted value
		else {
			Rectangle buttonRectangle = guiProperties.getRect(property_bounds);
			super.move(buttonRectangle.x, buttonRectangle.y);
		}
	}*/


	/**
	 * Description of the Method
	 *
	 * @param d Description of Parameter
	 */
	// not needed internal setBounds(x, y, width, height)
	// was called
	/*
	public void resize(Dimension d) {
		this.resize(d.width, d.height);
	}*/


	/**
	 * Description of the Method
	 *
	 * @param width Description of Parameter
	 * @param height Description of Parameter
	 */
	//not needed internal the method setBounds was called
	/*
	public void resize(int width, int height) {
//		this.setSize(width, height);
		// if persisted, and already displayed at least once, then allow programmatic resize
		if (hasBeenVisibleAtLeastOnce) {
			super.setBounds(super.getX(), super.getY(), width, height);
			guiProperties.setRect(property_bounds, this.getBounds());
		}
		// else, if not been visible at least once, and not yet ever persisted then allow programmatic resize to set initial location
		else if (guiProperties == null || guiProperties.getRect(property_bounds) == null) {
			super.setBounds(super.getX(), super.getY(), width, height);
		}
		// else, ignore request and set to the previously persisted value
		else {
			Rectangle buttonRectangle = guiProperties.getRect(property_bounds);
			super.setBounds(super.getX(), super.getY(), width, height);
		}
	}
	*/


	/**
	 * Description of the Method
	 */
	public void hide() {
		init();
		guiProperties.setRect(property_bounds, this.getBounds());
		guiProperties.save();
		super.hide();
	}


	/**
	 * Description of the Method
	 */
	protected void init() {
		// check if not already init'd
		if (guiProperties == null) {
			guiProperties = UserProperties.getInstance(nameSpace);
		}
	}

// NOT guaranteed to be called prior to exit!!!! need to find javaworld article on how to guarantee finalize...and if it doesn'buttonText require implementor intervention then use it rather than requiring explicit call to save().
//	/**
//	 * Description of the Method
//	 *
//	 * @exception Throwable Description of Exception
//	 */
//	protected void finalize()
//		throws Throwable {
//		init();
//		System.out.println("finalize");
//		guiProperties.save();
//		super.finalize();
//	}
}
