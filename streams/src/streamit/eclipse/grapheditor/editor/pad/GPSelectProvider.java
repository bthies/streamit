/*
 * @(#)GPSelectProvider.java	1.0 17.02.2003
 *
 * Copyright (C) 2003 luzar
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;

/**A Dialog to select a graph model provider.
 *
 * If only one graph model provider is available the
 * show method returns directly and the method
 * <tt>getSelectedGraphModelProvider</tt> returns
 * this graph model provider. Otherwise
 * the dialog was shown and the user can select
 * one graph model.
 *
 * @author luzar
 * @version 1.0
 */
public class GPSelectProvider extends JDialog {

	/** main panel for the list and the ok button
	 */
	JPanel pnlMain = new JPanel(new BorderLayout());

	/** List for the graph model providers
	 */
	JList lstGraphModelProviders = new JList();

	/** Scroll pane for the graph model providers
	 */
	JScrollPane paneScroll = new JScrollPane(lstGraphModelProviders);

	/** ok button
	 */
	JButton cmdOk = new JButton(Translator.getString("OK"));

	/** const to specify the ok answer
	 */
	public static final int OPTION_OK = 0;

	/** const to specify the cancel answer
	 */
	public static final int OPTION_CANCEL = 1;

	/** The answer. One of the option const values
	 */
	int answer = OPTION_OK;

	/** init  method to initialize the
	 *  gui.
	 */
	protected void init() {
		this.setTitle(Translator.getString("GPSelectProvider.Title"));

		this.addWindowListener( new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				answer = OPTION_CANCEL;
			}

		});

		lstGraphModelProviders.setCellRenderer(new GraphModelProviderRenderer());
		lstGraphModelProviders.setPreferredSize(new Dimension(300,200));
		GraphModelProvider[] providers = GraphModelProviderRegistry .getGraphModelProviders();
		if (providers.length == 0)
			JOptionPane.showMessageDialog(this, Translator.getString("Error.No_GraphModelProvider_available"), Translator.getString("Error"), JOptionPane.ERROR_MESSAGE );
		lstGraphModelProviders.setListData( providers );
		lstGraphModelProviders.setSelectedIndex(0);
		pnlMain.add(paneScroll, BorderLayout.CENTER);
		paneScroll.setAutoscrolls(true);
		pnlMain.add(cmdOk, BorderLayout.SOUTH);
		cmdOk.addActionListener(new ActionListener() {
			/**
			 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
			 */
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		getContentPane().add(pnlMain);
		pack();
		Utilities.center(this);

	}

	/**
	 * Constructor for GPSelectProvider.
	 *
	 * @param owner
	 * @throws HeadlessException
	 */
	public GPSelectProvider(Frame owner) throws HeadlessException {
		super(owner, true);
		init();
	}

	/**Returns the selected graph model provider
	 *
	 */
	public GraphModelProvider getSelectedGraphModelProvider(){
		if (lstGraphModelProviders.getModel() .getSize() == 0)
			return null;
		return (GraphModelProvider)lstGraphModelProviders.getSelectedValue() ;
	}

	/**
	 * Constructor for GPSelectProvider.
	 * @param owner
	 * @throws HeadlessException
	 */
	public GPSelectProvider(Dialog owner) throws HeadlessException {
		super(owner, true);
		init();
	}

	/** Renderer class for the graph model provider objects.
	 *  The renderer uses the method
	 *  <tt>getPresentationName</tt> from the
	 *  graph model provider for the
	 *  visible text.
	 *
	 *  @see GraphModelProvider
	 */
	class GraphModelProviderRenderer extends JLabel implements ListCellRenderer {
		/** Default constructor
		 */
		public GraphModelProviderRenderer() {
		}

		/** Returns a JLabel with the presentation name
		 *  of the graph model provider.
		 */
		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {
			GraphModelProvider gmp = (GraphModelProvider) value;
			setText(gmp.getPresentationName());
			setOpaque(true);

			setForeground(isSelected ? list.getSelectionForeground(): list.getForeground() );
			setBackground(isSelected ? list.getSelectionBackground(): list.getBackground() );
			return this;
		}
	}


	/**
	 * Returns the answer.
	 * @return int
	 */
	public int getAnswer() {
		return answer;
	}

	/** shows the select dialog. If only one
	 *  graph model provider is available the
	 *  method returns directly.
	 */
	public void show(){

		if (lstGraphModelProviders .getModel() .getSize() <= 1)
			return;
		else
			super.show();
	}

}
