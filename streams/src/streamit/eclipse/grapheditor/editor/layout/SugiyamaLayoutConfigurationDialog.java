// This file is part of the Echidna project
// (C) 2002 Forschungszentrum Informatik (FZI) Karlsruhe
// Please visit our website at http://echidna.sf.net
package streamit.eclipse.grapheditor.editor.layout;

import java.awt.*;
import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * A Dialog for configuring a SugiyamaLayoutAlgorithm.
 * You can use horizontal and vertical spacing.<br>
 * <br>
 * Will be shown by the SugiyamaLayoutController.<br>
 *<br>
 *<br>
 * @author Sven Luzar<br>
 * @version 1.0 init
 */
public class SugiyamaLayoutConfigurationDialog extends javax.swing.JDialog {
    
    /** configurations
     */
    private static final String CAPTION = "SugiyamaLayoutConfiguration"/*#Frozen*/;
    
    
    /** Boolean for the cancel operation variables
     */
    private boolean canceled;
    
    /**
     * Creates new form SugiyamaLayoutConfigurationDialog
     */
    public SugiyamaLayoutConfigurationDialog(java.awt.Frame parent) {
        
        super(parent, true);
        initComponents();
        
        // size, title and location
        setTitle(CAPTION);
        setName(CAPTION);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - this.getWidth()) / 2;
        int y = (screen.height - this.getHeight()) / 2;
        setLocation(x, y);
    }
    

    
    /** Called by pressing the cancel button
     *
     */
    private void action_cancel() {
        setVisible(false);
        dispose();
        canceled = true;
    }
    
    /** Called by pressing the ok button
     *
     */
    private void action_ok() {
        try {
            Integer.parseInt(horizontalSpacingTextField.getText());
            Integer.parseInt(verticalSpacingTextField.getText());
        } catch (Exception e) {
			String message = Translator.getString("Error.SpacingMustBeNumbers"/*#Finished:Original="Spacing must be numbers"*/);
            JOptionPane.showMessageDialog(this, message, Translator.getString("Error"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        setVisible(false);
        dispose();
        canceled = false;
    }
    
    /**
     * Returns true if the dialog has been canceled.
     */
    public boolean canceled() {
        return canceled;
    }
    
    /**
     * Returns the value of the "Horizontal spacing" as text.
     */
    public String getIndention() {
        return horizontalSpacingTextField.getText().trim();
    }
    
    /**
     * Returns the value of the "Vertical spacing" as text.
     */
    public String getVerticalSpacing() {
        return verticalSpacingTextField.getText().trim();
    }
    
    /**
     * Set the value of the "Horizontal spacing" text field.
     */
    public void setIndention(String text) {
        horizontalSpacingTextField.setText(text);
    }
    
    /**
     * Set the value of the "Vertical Spacing" text field.
     */
    public void setVerticalSpacing(String text) {
        verticalSpacingTextField.setText(text);
    }

    /** Initialize the Swing Components
     */   
    private void initComponents() {//GEN-BEGIN:initComponents
        jPanel1 = new javax.swing.JPanel(new GridLayout(2,2));
        toolBar = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT ));
        cancelButton = new javax.swing.JButton();
        finishedButton = new javax.swing.JButton();
        horizontalSpacingTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        verticalSpacingTextField = new javax.swing.JTextField();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        finishedButton.setText(Translator.getString("OK"));
        finishedButton.setName(Translator.getString("OK"));
        finishedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                finishedButtonActionPerformed(evt);
            }
        });

        toolBar.add(finishedButton);
		getRootPane().setDefaultButton(finishedButton);

        cancelButton.setText(Translator.getString("Cancel"));
        cancelButton.setName(Translator.getString("Cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        toolBar.add(cancelButton);

		getContentPane().add(toolBar, BorderLayout.SOUTH );        


        jLabel1.setText(Translator.getString("HorizontalSpacing"/*#Finished:Original="Horizontal Spacing"*/));
        jLabel1.setName("HorizontalSpacing"/*#Frozen*/);
        jPanel1.add(jLabel1);
        jPanel1.add(horizontalSpacingTextField);

        jLabel2.setText(Translator.getString("VerticalSpacing"/*#Finished:Original="Vertical Spacing"*/));
        jLabel2.setName("VerticalSpacing"/*#Frozen*/);
        jPanel1.add(jLabel2);

        jPanel1.add(verticalSpacingTextField);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }//GEN-END:initComponents

    /** Calls the action_ok method
     *  @see #action_ok
     */    
    private void finishedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_finishedButtonActionPerformed
        action_ok();
    }//GEN-LAST:event_finishedButtonActionPerformed
    
    /** Calls the action_cancel method
     *  @see #action_cancel
     */    
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        action_cancel();
    }//GEN-LAST:event_cancelButtonActionPerformed
    
    /** Closes the dialog 
     * @see #action_cancel
     * */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        action_cancel();
    }//GEN-LAST:event_closeDialog
        // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField verticalSpacingTextField;
    private javax.swing.JPanel toolBar;
    private javax.swing.JTextField horizontalSpacingTextField;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton finishedButton;
    // End of variables declaration//GEN-END:variables
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new SugiyamaLayoutConfigurationDialog(new javax.swing.JFrame()).show();
    }
}

