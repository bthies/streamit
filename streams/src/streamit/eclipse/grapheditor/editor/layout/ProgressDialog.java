/*
 * @(#)ProgressDialog.java	1.0 01/20/03
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
package streamit.eclipse.grapheditor.editor.layout;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**Shows a progress dialog for the layout algorithm.
 *
 * The user can use the cancel button to cancel
 * the layout algorithm. After canceling the
 * method isCanceld returned true.
 *
 *
 * @author <a href="mailto:Sven.Luzar@web.de">Sven Luzar</a>
 * @since 1.2.2
 * @version 1.0 init
 *
 */
public class ProgressDialog extends JDialog {
//TODO: Need to be externalized.
	static String progress = Translator.getString("Progress"); /* #Finished */
	static String cancel = Translator.getString("Cancel"); /* #Finished */
	static String sum = Translator.getString("Sum"); /* #Finished */

	/** GUI Object */
	JPanel pnlMain = new JPanel();
	/** GUI Object */
	JPanel pnlButton = new JPanel();
	/** GUI Object */
	JLabel lblMessage = new JLabel();
	/** GUI Object */
	JLabel lblProgressSum = new JLabel(sum);
	/** GUI Object */
	JLabel lblProgressSumVal = new JLabel("  0 %"/*#Frozen*/);
	/** GUI Object */
	BorderLayout layBorderMain = new BorderLayout();
	/** GUI Object */
	JProgressBar pbProgressSum = new JProgressBar();
	/** true if the dialog was canceled */
	boolean isCanceled = false;
	/** GUI Object */
	JPanel pnlProgress = new JPanel();
	/** GUI Object */
	GridBagLayout layProgress = new GridBagLayout();
	/** GUI Object */
	JButton cmdCancel = new JButton();



	/**
	 * Constructor for the Progress Dialog.
	 */
	public ProgressDialog() {
		this((Frame) null, progress, false);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 */
	public ProgressDialog(Frame owner) {
		this(owner, progress, false);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 * @param modal
	 */
	public ProgressDialog(Frame owner, boolean modal)
		{
		this(owner, progress, modal);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 * @param title
	 */
	public ProgressDialog(Frame owner, String title) {
		this(owner, title, false);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 * @param title
	 * @param modal
	 */
	public ProgressDialog(Frame owner, String title, boolean modal)
		{
		super(owner, title, modal);
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 */
	public ProgressDialog(Dialog owner) {
		this(owner, false);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 * @param modal
	 */
	public ProgressDialog(Dialog owner, boolean modal)
		{
		this(owner, progress + ":", modal);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 * @param title
	 */
	public ProgressDialog(Dialog owner, String title)
		{
		this(owner, title, false);
	}

	/**
	 * Constructor for the Progress Dialog.
	 * @param owner
	 * @param title
	 * @param modal
	 */
	public ProgressDialog(Dialog owner, String title, boolean modal)
		{
		super(owner, title, modal);

		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** main method to test this dialog
	 */
	public static void main(String[] args) {
		final ProgressDialog dlg =
			new ProgressDialog((Frame) null, progress, false);

		dlg.setVisible(true);

		Thread t = new Thread() {
			public void run() {
				dlg.setMaximum(2235);
				dlg.setValue(234);
				dlg.setValue(500);
				dlg.setValue(1211);
				dlg.setValue(2235);
			}
		};
		t.start();
	}

	/** initializes the GUI
	 */
	private void jbInit() throws Exception {
		pnlMain.setLayout(layBorderMain);
		lblMessage.setText(progress);
		lblMessage.setName(Translator.getString("Progress")); /* #Finished */
		pnlProgress.setLayout(layProgress);

		lblProgressSum.setName(Translator.getString("Sum")); /* #Finished */

		lblProgressSumVal.setHorizontalAlignment(SwingConstants.RIGHT);
		cmdCancel.setText(cancel);
		cmdCancel.setName(Translator.getString("Cancel")); /* #Finished */
		cmdCancel.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(ActionEvent e) {
				cmdCancel_actionPerformed(e);
			}
		});
		this.getContentPane().add(pnlMain, BorderLayout.CENTER);
		pnlMain.add(lblMessage, BorderLayout.NORTH);
		pnlMain.add(pnlButton, BorderLayout.SOUTH);

		pnlMain.add(pnlProgress, BorderLayout.CENTER);
		pnlProgress.add(
			lblProgressSum,
			new GridBagConstraints(
				0,
				1,
				2,
				1,
				0.0,
				0.0,
				GridBagConstraints.NORTHWEST,
				GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0),
				25,
				0));
		pnlProgress.add(
			pbProgressSum,
			new GridBagConstraints(
				2,
				1,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.NORTHWEST,
				GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0),
				50,
				0));
		pnlProgress.add(
			lblProgressSumVal,
			new GridBagConstraints(
				3,
				1,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.CENTER,
				GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0),
				25,
				0));
		pnlButton.add(cmdCancel, null);

		int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
		pack();

		this.setBounds(
			(screenWidth - 400) / 2,
			(screenHeight - 100) / 2,
			400,
			100);

	}

	/** sets the minimum value to the progressbar
	 */
	public void setMinimum(int min) {
		pbProgressSum.setMinimum(min);
	}

	/** sets the maximum value to the progressbar
	 */
	public void setMaximum(int max) {
		pbProgressSum.setMaximum(max);
	}

	/** sets the progressbar to the maximumvalue
	 */
	public void setToMaximum() {
		setValue(pbProgressSum.getMaximum());
	}

	/** sets the specified value to the progressbar
	 */
	public void setValue(int value) {
		pbProgressSum.setValue(value);

		lblProgressSumVal.setText(
			java.text.NumberFormat.getInstance().format(
				Math.round(pbProgressSum.getPercentComplete() * 100))
				+ "%");
	}
	/** returns the current value from the progressbar
	 */
	public int getValue() {
		return pbProgressSum.getValue();
	}

	/** sets the progress message
	 */
	public void setMessage(String message) {
		lblMessage.setText(message);
	}

	/** sets the boolean isCanceled to true.
	 */
	void cmdCancel_actionPerformed(ActionEvent e) {
		isCanceled = true;
	}

	/** returns true if the user
	 *  has clicked on the cancel button
	 */
	public boolean isCanceled() {
		return isCanceled;
	}

	/** sets the cancel button visible or not
	 *
	 *  @param visible the parameter specifies state
	 */
	public void setCancelVisible(boolean visible) {
		cmdCancel.setVisible(visible);
	}

	/** sets the cancel button enabled or not
	 *
	 *  @param visible the parameter specifies state
	 */
	public void setCancelEnabled(boolean enabled) {
		cmdCancel.setEnabled(enabled);
	}
	/** Returns the cancel button
	 */
	public JButton getCancelButton() {
		return cmdCancel;
	}
}