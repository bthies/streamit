/*
 * @(#)GPDialog.java 1.0 06.08.2003
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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;


/**
 * One Layer between the JDialog 
 * and our implementation. 
 * Currently we add a load and store 
 * management for the window position.
 * and a locale change listener support 
 *
 * @author sven_luzar
 * @version 1.0
 * 
 */
public class GPDialog extends JDialog {

	/** Key description for the ESC key.
	 * 
	 */
	protected KeyStroke escKeystroke =
		KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog() throws HeadlessException {
		super();
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Dialog owner) throws HeadlessException {
		super(owner);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param modal
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Dialog owner, boolean modal) throws HeadlessException {
		super(owner, modal);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Frame owner) throws HeadlessException {
		super(owner);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param modal
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Frame owner, boolean modal) throws HeadlessException {
		super(owner, modal);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param title
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Dialog owner, String title) throws HeadlessException {
		super(owner, title);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param title
	 * @param modal
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Dialog owner, String title, boolean modal)
		throws HeadlessException {
		super(owner, title, modal);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param title
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Frame owner, String title) throws HeadlessException {
		super(owner, title);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param title
	 * @param modal
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(Frame owner, String title, boolean modal)
		throws HeadlessException {
		super(owner, title, modal);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param title
	 * @param modal
	 * @param gc
	 * @throws java.awt.HeadlessException
	 */
	public GPDialog(
		Dialog owner,
		String title,
		boolean modal,
		GraphicsConfiguration gc)
		throws HeadlessException {
		super(owner, title, modal, gc);
		initDialog();		
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param owner
	 * @param title
	 * @param modal
	 * @param gc
	 */
	public GPDialog(
		Frame owner,
		String title,
		boolean modal,
		GraphicsConfiguration gc) {
		super(owner, title, modal, gc);
		initDialog();		
	}
	
	/**
	 *  Adds the instance to the position manager and to
	 *  the locale change adapter. 
	 *  Registers the default esc action.
	 * 
	 */
	
	private void initDialog(){
		LocaleChangeAdapter.addContainer(this);
		PositionManager.addComponent(this);
		//registerDefaultEscAction();		
	}
	
	
	/** Removes the current instance from the  
	 *  position manager and from
	 *  the locale change adapter.
	 *  After that the method calls
	 *  the super implementation. 
	 *  
	 * @see java.lang.Object#finalize()
	 */
	
	protected void finalize() throws Throwable {
		LocaleChangeAdapter.removeContainer(this);
		PositionManager.removeComponent(this);
		super.finalize();
	}
	
	/** Calls the super implementation
	 *  and makes an update for the
	 *  component by using the locale
	 *  change adapter and the 
	 *  position manager.
	 *  
	 *  @param name the new name
	 *  @see PositionManager#updateComponent(Component)
	 *  @see LocaleChangeAdapter#updateComponent(Component)
	 *  @see java.awt.Component#setName(java.lang.String)
	 * 
	 */
	public void setName(String name) {
		super.setName(name);
		PositionManager.updateComponent(this);
		LocaleChangeAdapter.updateComponent(this);
	}

	/** makes an update for the locale
	 *  dependent values from the whole
	 *  container and calls
	 *  the super implementation 
	 *  
	 * @see java.awt.Component#validate()
	 * @see LocaleChangeAdapter#updateContainer(Container)
	 * @see java.awt.Container#validate()
	 */
	public void validate() {
		LocaleChangeAdapter.updateContainer(this);
		super.validate();
	}

	/** Registers the default 
	 *  window esc action for this
	 *  frame. 
	 * 
	 *  @see GPEscAction
	 *  
	 */
	public void registerDefaultEscAction() {
		registerEscAction(new GPEscAction());
	}

	/** Registers the specified
	 *  action for a esc action 
	 *  of this frame. 
	 * 
	 *  @param action the action 
	 *  
	 */
	public void registerEscAction(Action action) {
		this.getRootPane().registerKeyboardAction(
			action,
			escKeystroke,
			JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/** Unregisters the esc action 
	 *  of this frame. 
	 *  
	 */
	public void unregisterEscAction() {
		this.getRootPane().unregisterKeyboardAction(escKeystroke);

	}

	/** Registers the specified button
	 *  for the default esc button.
	 * 
	 * @param button
	 */	
	public void setEscButton(JButton button){
		registerEscAction(new GPEscAction(button));
	}
	
	/** Registers the specified button
	 *  for the default button.
	 * 
	 * @param button
	 */	
	public void setDefaultButton(JButton button){
		getRootPane().setDefaultButton(button);
	}

}
