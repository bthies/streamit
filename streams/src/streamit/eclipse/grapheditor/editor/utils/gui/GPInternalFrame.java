/*
 * @(#)GPInternalFrame.java 1.0 06.08.2003
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

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;

/**
 * One Layer between the JInternalFrame 
 * and our implementation. 
 * Currently we add a load and store 
 * management for the window position.
 * and a locale change listener support 
 *
 * @author sven_luzar
 * @version 1.0
 * 
 */
public class GPInternalFrame extends JInternalFrame {

	/** Key description for the ESC key.
	 * 
	 */
	protected KeyStroke escKeystroke =
		KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 */
	public GPInternalFrame() {
		super();
		initInternalFrame();
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 * 
	 * @param title
	 */
	public GPInternalFrame(String title) {
		super(title);
		initInternalFrame();
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
     *
	 * @param title
	 * @param resizable
	 */
	public GPInternalFrame(String title, boolean resizable) {
		super(title, resizable);
		initInternalFrame();
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 *
	 * @param title
	 * @param resizable
	 * @param closable
	 */
	public GPInternalFrame(String title, boolean resizable, boolean closable) {
		super(title, resizable, closable);
		initInternalFrame();
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 *
	 * @param title
	 * @param resizable
	 * @param closable
	 * @param maximizable
	 */
	public GPInternalFrame(
		String title,
		boolean resizable,
		boolean closable,
		boolean maximizable) {
		super(title, resizable, closable, maximizable);
		initInternalFrame();
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 *  and the locale change adapter
	 *
	 * @param title
	 * @param resizable
	 * @param closable
	 * @param maximizable
	 * @param iconifiable
	 */
	public GPInternalFrame(
		String title,
		boolean resizable,
		boolean closable,
		boolean maximizable,
		boolean iconifiable) {
		super(title, resizable, closable, maximizable, iconifiable);
		initInternalFrame();
	}
	
	/**
	 *  Adds the instance to the position manager and to
	 *  the locale change adapter. 
	 *  Registers the default esc action.
	 * 
	 */
	
	private void initInternalFrame(){
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
