/*
 * @(#)GEMLayoutConfigurationDialog.java 1.0 12.08.2003
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
package streamit.eclipse.grapheditor.editor.layout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;


/******************************************************************************/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/******************************************************************************/
/**
 * @author winkler
 * @author Sven Luzar
 *
 */


public class GEMLayoutConfigurationDialog extends JDialog {

    private JTextField tf_initTemperature;
    private JTextField tf_minTemperature;
    private JTextField tf_maxTemperature;
    private JTextField tf_prefEdgeLength;
    private JTextField tf_gravitation;
    private JTextField tf_randomImpulseRange;
    private JTextField tf_overlapDetectWidth;
    private JTextField tf_overlapPrefDistance;
    private JTextField tf_alphaOsc;
    private JTextField tf_alphaRot;
    private JTextField tf_sigmaOsc;
    private JTextField tf_sigmaRot;
    
    private JTextField tf_lu_initTemperature;
    private JTextField tf_lu_minTemperature;
    private JTextField tf_lu_maxTemperature;
    private JTextField tf_lu_prefEdgeLength;
    private JTextField tf_lu_gravitation;
    private JTextField tf_lu_randomImpulseRange;
    private JTextField tf_lu_overlapDetectWidth;
    private JTextField tf_lu_overlapPrefDistance;
    private JTextField tf_lu_alphaOsc;
    private JTextField tf_lu_alphaRot;
    private JTextField tf_lu_sigmaOsc;
    private JTextField tf_lu_sigmaRot;
    private JTextField tf_lu_depth;
    private JTextField tf_lu_cluster_initTemperature;
    private JTextField tf_lu_cluster_forceScalingFactor;
    private JTextField tf_lu_cluster_factor;
    private JTextField tf_lu_perimeter_initSize;
    private JTextField tf_lu_perimeter_sizeInc;
    
    private JCheckBox  cb_computePermutation;
    private JCheckBox  cb_endPerAverage;
    private JCheckBox  cb_avoidOverlapping;
    private JCheckBox  cb_useOptimizationAlg;
    
    private JCheckBox  cb_lu_enableLayoutUpdate;
    private JCheckBox  cb_lu_computePermutation;
    private JCheckBox  cb_lu_endPerAverage;
    private JCheckBox  cb_lu_avoidOverlapping;
    private JCheckBox  cb_lu_useOptimizationAlg;
    private JCheckBox  cb_lu_cluster_enable;    

    private JComboBox  comb_lu_method;
    
    private JComboBox  comb_loadPreSets;
    
    private JButton    button_ConfigOptimizeAlg;
    private JButton    button_lu_ConfigOptimizeAlg;
    
    private JPanel panelLayoutUpdate;
	private JScrollPane panelLayoutRunWrapper;
	private JScrollPane panelLayoutUpdateWrapper;
	//private JPanel panelLUClusterWrapper;

    private Properties[] presetConfigs;
    
    private AnnealingLayoutController optimizeAlgController;
    private AnnealingLayoutController lu_optimizeAlgController;
    
    private JTabbedPane tp_main;
    
    /** configurations
     */
    private static final String CAPTION = "GEM Layout Configuration";
    
    
    /** Boolean for the cancel operation variables
     */
    private boolean canceled = false;
    
/******************************************************************************/    
    /**
     * Creates new form GEMLayoutConfigurationDialog
     */
    public GEMLayoutConfigurationDialog(Frame parent, Properties[] preSets, AnnealingLayoutController optimizer, AnnealingLayoutController lu_optimizer) {
        
        super(parent, true);
        
        presetConfigs = preSets;
        
        optimizeAlgController = optimizer;
        lu_optimizeAlgController = lu_optimizer;
        
        initComponents();
        
        // size, title and location
        setTitle(CAPTION);
        setName(CAPTION);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int)((double)(screen.width  - this.getWidth() ) / 2.0);
        int y = (int)((double)(screen.height - this.getHeight()) / 2.0);
        setLocation(x, y);
    }
    
/******************************************************************************/        

    protected Properties[] getPresetConfigs(){
        return presetConfigs;
    }

/******************************************************************************/        

    protected Properties getPresetConfig(int index){
        return presetConfigs[index];
    }

/******************************************************************************/        

    protected void setPresetConfigs(Properties[] preSets){
        presetConfigs = preSets;
    }

/******************************************************************************/        
/**
 * Changes the configuration of the dialog. This is one, of the array of
 * configurations, gained in the constructor or with 
 * {@link #setPresetConfigs(Properties[])}. Therfore the index has to be
 * whithin the bounds of this array.
 * 
 * @param index Index of the configuration set
 */
    protected void switchPreferences(int index){
        
        Properties config = presetConfigs[index];        
        
        double alphaOsc = Math.toDegrees(Double.parseDouble((String)config.get(GEMLayoutController.KEY_ALPHA_OSC)));
        double alphaRot = Math.toDegrees(Double.parseDouble((String)config.get(GEMLayoutController.KEY_ALPHA_ROT)));
        
        double lu_alphaOsc = Math.toDegrees(Double.parseDouble((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_OSC)));
        double lu_alphaRot = Math.toDegrees(Double.parseDouble((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_ROT)));
        
        tf_initTemperature       .setText((String)config.get(GEMLayoutController.KEY_INIT_TEMPERATURE));
        tf_minTemperature        .setText((String)config.get(GEMLayoutController.KEY_MIN_TEMPERATURE));
        tf_maxTemperature        .setText((String)config.get(GEMLayoutController.KEY_MAX_TEMPERATURE));
        tf_prefEdgeLength        .setText((String)config.get(GEMLayoutController.KEY_PREF_EDGE_LENGTH));
        tf_gravitation           .setText((String)config.get(GEMLayoutController.KEY_GRAVITATION));
        tf_randomImpulseRange    .setText((String)config.get(GEMLayoutController.KEY_RANDOM_IMPULSE_RANGE));
        tf_overlapDetectWidth    .setText((String)config.get(GEMLayoutController.KEY_OVERLAPPING_DETECTION_WIDTH));
        tf_overlapPrefDistance   .setText((String)config.get(GEMLayoutController.KEY_OVERLAPPING_PREF_DISTANCE));
        
        tf_lu_initTemperature    .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE));
        tf_lu_minTemperature     .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE));
        tf_lu_maxTemperature     .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_MAX_TEMPERATURE));
        tf_lu_prefEdgeLength     .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH));
        tf_lu_gravitation        .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_GRAVITATION));
        tf_lu_randomImpulseRange .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE));
        tf_lu_overlapDetectWidth .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH));
        tf_lu_overlapPrefDistance.setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE));
        tf_lu_cluster_initTemperature.setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE));
        tf_lu_cluster_forceScalingFactor.setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR));
        tf_lu_cluster_factor     .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR));
                
        tf_alphaOsc              .setText(String.valueOf(alphaOsc));
        tf_alphaRot              .setText(String.valueOf(alphaRot));
        tf_sigmaOsc              .setText((String)config.get(GEMLayoutController.KEY_SIGMA_OSC));
        tf_sigmaRot              .setText((String)config.get(GEMLayoutController.KEY_SIGMA_ROT));
                
        tf_lu_alphaOsc           .setText(String.valueOf(lu_alphaOsc));
        tf_lu_alphaRot           .setText(String.valueOf(lu_alphaRot));
        tf_lu_sigmaOsc           .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_OSC));
        tf_lu_sigmaRot           .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_ROT));
        
        tf_lu_depth              .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_DEPTH));
        tf_lu_perimeter_initSize .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE));
        tf_lu_perimeter_sizeInc  .setText((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC));
                
        cb_computePermutation   .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_COMPUTE_PERMUTATION)));
        cb_endPerAverage        .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_END_CONDITION_AVERAGE)));
        cb_avoidOverlapping     .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_AVOID_OVERLAPPING)));
        cb_useOptimizationAlg   .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_OPTIMIZE_ALGORITHM_ENABLED)));
        
        cb_lu_enableLayoutUpdate.setSelected(isTrue((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_ENABLED)));
        cb_lu_computePermutation.setSelected(isTrue((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION)));
        cb_lu_endPerAverage     .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE)));
        cb_lu_avoidOverlapping  .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING)));
        cb_lu_useOptimizationAlg.setSelected(isTrue((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED)));
        cb_lu_cluster_enable    .setSelected(isTrue((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED)));
        
        comb_lu_method.setSelectedItem((String)config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD));
    }
        
/******************************************************************************/        
/**
 * Transforms a string value in a boolean value. The string has be "true" or
 * "false". Lower or upper case doesn't matter.
 * 
 * @param boolValue A String, consisting of the word "true" or "false".
 * @return The boolean expression of the string.
 */
    protected boolean isTrue(String boolValue){
        if( boolValue != null ){
            if( "TRUE".equals(boolValue.toUpperCase()) ){
                return true;
            }
            else if( "FALSE".equals(boolValue.toUpperCase()) ){
                return false;
            }
        }
        return false;
    }

/******************************************************************************/    
    /** 
     * Called by pressing the cancel button. Leaves the dialog without
     * saving.
     */
    private void action_cancel() {
        setVisible(false);
        dispose();
        canceled = true;
    }
    
/******************************************************************************/    

    protected void action_ok() {
        
        action_ok(new ArrayList());
    }

/******************************************************************************/    
    /** 
     * Called by pressing the ok button. Checks all Textfields for their value
     * and produce a list of errors, while checking. If an error has appeared,
     * an error dialog is displayed, else the configuration dialog disappears.
     */
    protected void action_ok(ArrayList errList) {

        boolean isOK = true;

        isOK &= assertDouble(tf_initTemperature.getText(),    GEMLayoutController.KEY_INIT_TEMPERATURE           ,errList);
        isOK &= assertDouble(tf_minTemperature.getText(),     GEMLayoutController.KEY_MIN_TEMPERATURE            ,errList);
        isOK &= assertDouble(tf_maxTemperature.getText(),     GEMLayoutController.KEY_MIN_TEMPERATURE            ,errList);
        isOK &= assertDouble(tf_prefEdgeLength.getText(),     GEMLayoutController.KEY_PREF_EDGE_LENGTH           ,errList);
        isOK &= assertDouble(tf_gravitation.getText(),        GEMLayoutController.KEY_GRAVITATION                ,errList);
        isOK &= assertDouble(tf_randomImpulseRange.getText(), GEMLayoutController.KEY_RANDOM_IMPULSE_RANGE       ,errList);
        isOK &= assertDouble(tf_overlapDetectWidth.getText(), GEMLayoutController.KEY_OVERLAPPING_DETECTION_WIDTH,errList);
        isOK &= assertDouble(tf_overlapPrefDistance.getText(),GEMLayoutController.KEY_OVERLAPPING_PREF_DISTANCE  ,errList);
        isOK &= assertDouble(tf_alphaOsc.getText(),           GEMLayoutController.KEY_ALPHA_OSC                  ,errList);
        isOK &= assertDouble(tf_alphaRot.getText(),           GEMLayoutController.KEY_ALPHA_ROT                  ,errList);
        isOK &= assertDouble(tf_sigmaOsc.getText(),           GEMLayoutController.KEY_SIGMA_OSC                  ,errList);
        isOK &= assertDouble(tf_sigmaRot.getText(),           GEMLayoutController.KEY_SIGMA_ROT                  ,errList);
        
        isOK &= assertDouble(tf_lu_initTemperature.getText(),    GEMLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE           ,errList);
        isOK &= assertDouble(tf_lu_minTemperature.getText(),     GEMLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE            ,errList);
        isOK &= assertDouble(tf_lu_maxTemperature.getText(),     GEMLayoutController.KEY_LAYOUT_UPDATE_MAX_TEMPERATURE            ,errList);
        isOK &= assertDouble(tf_lu_prefEdgeLength.getText(),     GEMLayoutController.KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH           ,errList);
        isOK &= assertDouble(tf_lu_gravitation.getText(),        GEMLayoutController.KEY_LAYOUT_UPDATE_GRAVITATION                ,errList);
        isOK &= assertDouble(tf_lu_randomImpulseRange.getText(), GEMLayoutController.KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE       ,errList);
        isOK &= assertDouble(tf_lu_overlapDetectWidth.getText(), GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH,errList);
        isOK &= assertDouble(tf_lu_overlapPrefDistance.getText(),GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE  ,errList);
        isOK &= assertDouble(tf_lu_alphaOsc.getText(),           GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_OSC                  ,errList);
        isOK &= assertDouble(tf_lu_alphaRot.getText(),           GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_ROT                  ,errList);
        isOK &= assertDouble(tf_lu_sigmaOsc.getText(),           GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_OSC                  ,errList);
        isOK &= assertDouble(tf_lu_sigmaRot.getText(),           GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_ROT                  ,errList);
        
        isOK &= assertInteger(tf_lu_depth.getText(),             GEMLayoutController.KEY_LAYOUT_UPDATE_DEPTH               ,errList);
        isOK &= assertDouble(tf_lu_perimeter_initSize.getText(), GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE,errList);
        isOK &= assertDouble(tf_lu_perimeter_sizeInc.getText(),  GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC,errList);
        isOK &= assertDouble(tf_lu_cluster_initTemperature.getText(),GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE,errList);
        isOK &= assertDouble(tf_lu_cluster_forceScalingFactor.getText(),GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR,errList);
        isOK &= assertDouble(tf_lu_cluster_factor.getText(),     GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR          ,errList);
        
        if( isOK ){
            isOK &= assertMinimum(tf_initTemperature.getText(),0.0,false,GEMLayoutController.KEY_INIT_TEMPERATURE,errList);
            isOK &= assertMinimum(tf_minTemperature.getText(),0.0,false,GEMLayoutController.KEY_MIN_TEMPERATURE,errList);
            isOK &= assertMinimum(tf_maxTemperature.getText(),0.0,false,GEMLayoutController.KEY_MAX_TEMPERATURE,errList);
            isOK &= assertMinimum(tf_prefEdgeLength.getText(),0.0,false,GEMLayoutController.KEY_PREF_EDGE_LENGTH,errList);
            isOK &= assertRange(tf_gravitation.getText(),0.0,1.0,true,true,GEMLayoutController.KEY_GRAVITATION,errList);
            isOK &= assertMinimum(tf_randomImpulseRange.getText(),0.0,false,GEMLayoutController.KEY_RANDOM_IMPULSE_RANGE,errList);
            isOK &= assertMinimum(tf_overlapDetectWidth.getText(),0.0,false,GEMLayoutController.KEY_OVERLAPPING_DETECTION_WIDTH,errList);
            isOK &= assertMinimum(tf_overlapPrefDistance.getText(),0.0,false,GEMLayoutController.KEY_OVERLAPPING_PREF_DISTANCE,errList);
            isOK &= assertRange(tf_alphaOsc.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_ALPHA_OSC,errList);
            isOK &= assertRange(tf_alphaRot.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_ALPHA_ROT,errList);
            isOK &= assertRange(tf_sigmaOsc.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_SIGMA_OSC,errList);
            isOK &= assertRange(tf_sigmaRot.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_SIGMA_ROT,errList);
            
            isOK &= assertMinimum(tf_lu_initTemperature.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE,errList);
            isOK &= assertMinimum(tf_lu_minTemperature.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE,errList);
            isOK &= assertMinimum(tf_lu_maxTemperature.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_MAX_TEMPERATURE,errList);
            isOK &= assertMinimum(tf_lu_prefEdgeLength.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH,errList);
            isOK &= assertRange(tf_lu_gravitation.getText(),0.0,1.0,true,true,GEMLayoutController.KEY_LAYOUT_UPDATE_GRAVITATION,errList);
            isOK &= assertMinimum(tf_lu_randomImpulseRange.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE,errList);
            isOK &= assertMinimum(tf_lu_overlapDetectWidth.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH,errList);
            isOK &= assertMinimum(tf_lu_overlapPrefDistance.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE,errList);
            isOK &= assertRange(tf_lu_alphaOsc.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_OSC,errList);
            isOK &= assertRange(tf_lu_alphaRot.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_ROT,errList);
            isOK &= assertRange(tf_lu_sigmaOsc.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_OSC,errList);
            isOK &= assertRange(tf_lu_sigmaRot.getText(),0.0,360.0,true,false,GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_ROT,errList);
            
            isOK &= assertMinimum(tf_lu_depth.getText(),0,true,GEMLayoutController.KEY_LAYOUT_UPDATE_DEPTH,errList);
            isOK &= assertMinimum(tf_lu_perimeter_initSize.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE,errList);
            isOK &= assertMinimum(tf_lu_perimeter_sizeInc.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC,errList);
            isOK &= assertMinimum(tf_lu_cluster_initTemperature.getText(),0.0,false,GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE,errList);
            isOK &= assertRange(tf_lu_cluster_forceScalingFactor.getText(),0.0,1.0,false,true,GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR,errList);
            isOK &= assertMinimum(tf_lu_cluster_factor.getText(),1.0,true,GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,errList);
        }

        if( isOK ){
            setVisible(false);
            dispose();
            canceled = false;
        }
        else {
            //writing all errors in one string and adding to every error a 
            //return, except to the last one
            String errorMsg = new String("Error\n\n");
            for( int i = 0; i < errList.size(); i++ ){
                errorMsg += (String)errList.get(i);
                if( i != errList.size() - 1 )
                    errorMsg += "\n";
            }
                
            JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
        }
    }
        
/******************************************************************************/    

    protected void action_LoadPreSets(int index){
        switchPreferences(index);
        switchLayoutUpdatePanel();
        switchVisibility();
    }
    
/******************************************************************************/
/**
 * Enables/Disables Textfields and other components, depending on the state of
 * checkboxes and comboboxes on the dialog.
 */
    private void switchVisibility(){            
        tf_overlapDetectWidth.setEnabled(cb_avoidOverlapping.isSelected());
        tf_overlapPrefDistance.setEnabled(cb_avoidOverlapping.isSelected());
        tf_lu_overlapDetectWidth.setEnabled(cb_lu_avoidOverlapping.isSelected());
        tf_lu_overlapPrefDistance.setEnabled(cb_lu_avoidOverlapping.isSelected());
        button_ConfigOptimizeAlg.setEnabled(cb_useOptimizationAlg.isSelected());
        button_lu_ConfigOptimizeAlg.setEnabled(cb_lu_useOptimizationAlg.isSelected());
        tf_lu_cluster_initTemperature.setEnabled(cb_lu_cluster_enable.isSelected());
        tf_lu_cluster_forceScalingFactor.setEnabled(cb_lu_cluster_enable.isSelected());
        tf_lu_cluster_factor.setEnabled(cb_lu_cluster_enable.isSelected());
        if(comb_lu_method.getSelectedItem() == GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY){
            tf_lu_perimeter_initSize.setEnabled(false);
            tf_lu_perimeter_sizeInc .setEnabled(false);
        }
        else if(comb_lu_method.getSelectedItem() == GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETERS){
            tf_lu_perimeter_initSize.setEnabled(true);
            tf_lu_perimeter_sizeInc .setEnabled(true);            
        }
    }

/******************************************************************************/    
    /**
     * Returns true if the dialog has been canceled.
     */
    public boolean isCanceled() {
        return canceled;
    }
        
/******************************************************************************/    

    private void switchLayoutUpdatePanel(){
        if( cb_lu_enableLayoutUpdate.isSelected() ){
			tp_main.addTab("Update Values", panelLayoutUpdateWrapper);
        }
        else {
        	tp_main.remove(panelLayoutUpdateWrapper);
        }
        pack();
    }

/******************************************************************************/    
    /** Initialize the Swing Components
     */    
    private void initComponents() {
        
        JPanel panelGlobalValues = new JPanel(new GridBagLayout());
        JPanel panelLabels    = new JPanel(new GridLayout(12,1, 0,5));
        JPanel panelFields    = new JPanel(new GridLayout(12,1, 0,5));
        JPanel panelLayoutRun = new JPanel(new BorderLayout(5,10));
        JPanel panelButtons   = new JPanel(new GridLayout(1,3,10,0));
        JPanel panelDefaults  = new JPanel(new GridLayout(1,2,10,4));
        JPanel panelOverlap   = new JPanel(new GridLayout(3,2,10,4));
        JPanel panelOptimize  = new JPanel(new BorderLayout());
        JPanel panelOptiAlg   = new JPanel(new GridLayout(2,2));
        tp_main = new JTabbedPane();
        
        panelLayoutUpdate = new JPanel(new BorderLayout(5,10));
        panelLayoutUpdate.setBackground(new Color(205,215,215));
        //panelLayoutUpdate.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Update Values"));
        
        panelDefaults.add(panelLabels);
        panelDefaults.add(panelFields);
        
        //panelLayoutRun.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Apply Values"));
        panelLayoutRun.add(panelDefaults,BorderLayout.NORTH);
        panelLayoutRun.add(panelOptimize,BorderLayout.CENTER);
        panelLayoutRun.add(  getAdditionalForceConfig()  ,BorderLayout.SOUTH);        
        panelLayoutRun.setBackground(new Color(215,215,205));
        
        panelOptimize.add(panelOptiAlg,BorderLayout.NORTH);
        panelOptimize.add(panelOverlap,BorderLayout.CENTER);
        panelOptimize.setOpaque(false);
        
        
        panelDefaults.setOpaque(false);
        panelOverlap.setOpaque(false);
        panelLabels.setOpaque(false);
        panelFields.setOpaque(false);
        

        
        getContentPane().setLayout(new BorderLayout(5,10));
        getContentPane().add(  panelGlobalValues ,BorderLayout.NORTH);
		getContentPane().add(  tp_main    ,BorderLayout.CENTER);
        getContentPane().add(  panelButtons      ,BorderLayout.SOUTH);
        
        panelGlobalValues.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Global Settings"));
		
		
        panelGlobalValues.add(new JLabel("run permanent :"),                                    
                              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
                                                     GridBagConstraints.WEST,
                                                     GridBagConstraints.NONE,
                                                     new Insets(0,5,0,30), 
                                                     0,0) );

        cb_lu_enableLayoutUpdate = new JCheckBox();
        panelGlobalValues.add(cb_lu_enableLayoutUpdate,
                              new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 
                                                     GridBagConstraints.WEST,
                                                     GridBagConstraints.HORIZONTAL,
                                                     new Insets(0,0,0,0), 
                                                     0,0) );

       
        
        
        panelLabels.add( new JLabel("start temperature :"));
        panelLabels.add( new JLabel("min. temperature :"));
        panelLabels.add( new JLabel("max. temperature :"));
        panelLabels.add( new JLabel("preferred Edge length :"));
        panelLabels.add( new JLabel("gravitation :"));
        panelLabels.add( new JLabel("range of random impulse :"));
        panelLabels.add( new JLabel("compute permutations :"));
        panelLabels.add( new JLabel("end condition is average :"));
        panelLabels.add( new JLabel("alpha oscillation :"));
        panelLabels.add( new JLabel("alpha rotation :"));
        panelLabels.add( new JLabel("sigma oscillation :"));
        panelLabels.add( new JLabel("sigma rotation :"));
        
        

        tf_initTemperature    = new JTextField();
        tf_minTemperature     = new JTextField();
        tf_maxTemperature     = new JTextField();
        tf_prefEdgeLength     = new JTextField();
        tf_gravitation        = new JTextField();
        tf_randomImpulseRange = new JTextField();
        
        cb_computePermutation    = new JCheckBox();
        cb_endPerAverage         = new JCheckBox();
        
        cb_computePermutation   .setOpaque(false);
        cb_endPerAverage        .setOpaque(false);
        
        tf_alphaOsc           = new JTextField();
        tf_alphaRot           = new JTextField();
        
        tf_sigmaOsc           = new JTextField();
        tf_sigmaRot           = new JTextField();
    
        panelFields.add(tf_initTemperature);
        panelFields.add(tf_minTemperature);
        panelFields.add(tf_maxTemperature);
        panelFields.add(tf_prefEdgeLength);
        panelFields.add(tf_gravitation);
        panelFields.add(tf_randomImpulseRange);
        panelFields.add(cb_computePermutation);
        panelFields.add(cb_endPerAverage);
        panelFields.add(tf_alphaOsc);
        panelFields.add(tf_alphaRot);
        panelFields.add(tf_sigmaOsc);
        panelFields.add(tf_sigmaRot);
        
        
        cb_avoidOverlapping = new JCheckBox();
        cb_avoidOverlapping.setOpaque(false);
        tf_overlapDetectWidth = new JTextField();
        tf_overlapPrefDistance = new JTextField();
        
        
        JPanel panelOverlapSwitch = new JPanel(new GridLayout(1,2,10,4));
        panelOverlapSwitch.setOpaque(false);
        panelOverlapSwitch.add(new JLabel("on/off"));
        panelOverlapSwitch.add(cb_avoidOverlapping);

        JPanel panelEmpty = new JPanel();
        panelEmpty.setOpaque(false);

        panelOverlap.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Force for overlapping Cells",TitledBorder.LEADING,TitledBorder.ABOVE_TOP));
        panelOverlap.add(panelOverlapSwitch);
        panelOverlap.add(panelEmpty);
        panelOverlap.add(new JLabel("detection width :"));
        panelOverlap.add(tf_overlapDetectWidth);
        panelOverlap.add(new JLabel("pref. distance :"));
        panelOverlap.add(tf_overlapPrefDistance);
        
        
        cb_useOptimizationAlg = new JCheckBox();
        cb_useOptimizationAlg.setOpaque(false);
        cb_useOptimizationAlg.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                switchVisibility();
            }
        });
        
        button_ConfigOptimizeAlg = new JButton("Configure");
        button_ConfigOptimizeAlg.setBackground(panelLayoutRun.getBackground());
        
        JPanel panelEmpty2 = new JPanel();
        panelEmpty2.setOpaque(false);
        
        panelOptiAlg.add(new JLabel("Use optimization Algorithm:"));
        panelOptiAlg.add(cb_useOptimizationAlg);
        panelOptiAlg.add(panelEmpty2);
        panelOptiAlg.add(button_ConfigOptimizeAlg);
        panelOptiAlg.setOpaque(false);
        
        button_ConfigOptimizeAlg.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                optimizeAlgController.configure();
            }
        });
        
        
        cb_avoidOverlapping.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                switchVisibility();
            }
        });
                

        cb_lu_enableLayoutUpdate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                switchLayoutUpdatePanel();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                closeDialog();
            }
        });
        
        JButton buttonFinished = new JButton("OK");
        JButton buttonCancel   = new JButton("Cancel");
        
        comb_loadPreSets = new JComboBox();
        
        for( int i = 0; i < presetConfigs.length; i++ )
            comb_loadPreSets.addItem(presetConfigs[i].get(GEMLayoutController.KEY_CONFIG_NAME));
        
        
        panelButtons.add(buttonFinished);
        panelButtons.add(comb_loadPreSets);
        panelButtons.add(buttonCancel);

        buttonFinished.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action_ok();
            }
        });
        
        comb_loadPreSets.addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e){
                action_LoadPreSets(((JComboBox)e.getSource()).getSelectedIndex());
            }
            
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                action_cancel();
            }
        });
                
        getRootPane().setDefaultButton(buttonFinished);

        /*-----------------LAYOUT-UPDATE-------------------------*/
        
        JPanel panelLULabels  = new JPanel(new GridLayout(16,1, 0,5));
        JPanel panelLUFields  = new JPanel(new GridLayout(16,1, 0,5));
        JPanel panelLUDefaults = new JPanel(new BorderLayout(10,5));
        JPanel panelLUOverlap = new JPanel(new GridLayout(3,2,10,4));
        JPanel panelLUOptimize = new JPanel(new BorderLayout());
        JPanel panelLUOptiAlg  = new JPanel(new GridLayout(2,2,10,4));
        JPanel panelLUCluster  = new JPanel(new GridLayout(4,2,10,4));
                
        panelLUDefaults.setOpaque(false);
        panelLULabels.setOpaque(false);
        panelLUFields.setOpaque(false);
        panelLUOverlap.setOpaque(false);
        panelLUOptimize.setOpaque(false);
        panelLUOptiAlg.setOpaque(false);
        panelLUCluster.setOpaque(false);
        
        panelLUDefaults.add(panelLULabels,BorderLayout.WEST);
        panelLUDefaults.add(panelLUFields,BorderLayout.EAST);
        panelLUDefaults.add(panelLUCluster,BorderLayout.SOUTH);

        panelLUOptimize.add(panelLUOptiAlg,BorderLayout.NORTH);
        panelLUOptimize.add(panelLUOverlap,BorderLayout.CENTER);
        
        panelLayoutUpdate.add(panelLUDefaults,BorderLayout.NORTH);
        panelLayoutUpdate.add(panelLUOptimize,BorderLayout.CENTER);
        panelLayoutUpdate.add(getAdditionalForceLUConfig(),BorderLayout.SOUTH);
        
        
        panelLULabels.add( new JLabel("start temperature :"));
        panelLULabels.add( new JLabel("min. temperature :"));
        panelLULabels.add( new JLabel("max. temperature :"));
        panelLULabels.add( new JLabel("preferred Edge length :"));
        panelLULabels.add( new JLabel("gravitation :"));
        panelLULabels.add( new JLabel("range of random impulse :"));
        panelLULabels.add( new JLabel("compute permutations :"));
        panelLULabels.add( new JLabel("end condition is average :"));
        panelLULabels.add( new JLabel("alpha oscillation :"));
        panelLULabels.add( new JLabel("alpha rotation :"));
        panelLULabels.add( new JLabel("sigma oscillation :"));
        panelLULabels.add( new JLabel("sigma rotation :"));
        panelLULabels.add( new JLabel("layout update method :"));
        panelLULabels.add( new JLabel("layout update depth :"));
        panelLULabels.add( new JLabel("layout update perimeter init size :"));
        panelLULabels.add( new JLabel("layout update perimeter increase value :"));
        
        

        tf_lu_initTemperature    = new JTextField();
        tf_lu_minTemperature     = new JTextField();
        tf_lu_maxTemperature     = new JTextField();
        tf_lu_prefEdgeLength     = new JTextField();
        tf_lu_gravitation        = new JTextField();
        tf_lu_randomImpulseRange = new JTextField();
        
        cb_lu_computePermutation   = new JCheckBox();
        cb_lu_endPerAverage        = new JCheckBox();
        
        cb_lu_computePermutation  .setOpaque(false);
        cb_lu_endPerAverage       .setOpaque(false);
        
        tf_lu_alphaOsc           = new JTextField();
        tf_lu_alphaRot           = new JTextField();
        
        tf_lu_sigmaOsc           = new JTextField();
        tf_lu_sigmaRot           = new JTextField();
        
        tf_lu_depth              = new JTextField();
        
        comb_lu_method           = new JComboBox();
        comb_lu_method.addItem(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY);
        comb_lu_method.addItem(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETERS);
        comb_lu_method.setBackground(panelLayoutUpdate.getBackground());
        
        tf_lu_perimeter_initSize = new JTextField();
        tf_lu_perimeter_sizeInc  = new JTextField();
    
        panelLUFields.add(tf_lu_initTemperature);
        panelLUFields.add(tf_lu_minTemperature);
        panelLUFields.add(tf_lu_maxTemperature);
        panelLUFields.add(tf_lu_prefEdgeLength);
        panelLUFields.add(tf_lu_gravitation);
        panelLUFields.add(tf_lu_randomImpulseRange);
        panelLUFields.add(cb_lu_computePermutation);
        panelLUFields.add(cb_lu_endPerAverage);
        panelLUFields.add(tf_lu_alphaOsc);
        panelLUFields.add(tf_lu_alphaRot);
        panelLUFields.add(tf_lu_sigmaOsc);
        panelLUFields.add(tf_lu_sigmaRot);
        panelLUFields.add(comb_lu_method);
        panelLUFields.add(tf_lu_depth);
        panelLUFields.add(tf_lu_perimeter_initSize);
        panelLUFields.add(tf_lu_perimeter_sizeInc);
        
        tf_lu_cluster_initTemperature = new JTextField();
        tf_lu_cluster_forceScalingFactor = new JTextField();
        tf_lu_cluster_factor = new JTextField();
        cb_lu_cluster_enable = new JCheckBox();
        cb_lu_cluster_enable.setOpaque(false);
        cb_lu_cluster_enable.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                switchVisibility();          
            }
        });
        
        JPanel panelLUClusteringSwitch = new JPanel(new GridLayout(1,2,10,4));
        panelLUClusteringSwitch.setOpaque(false);
        panelLUClusteringSwitch.add(new JLabel("on/off"));
        panelLUClusteringSwitch.add(cb_lu_cluster_enable);
        
        JPanel panelLUClusterEmpty = new JPanel();
        panelLUClusterEmpty.setOpaque(false);
        
        panelLUCluster.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Clustering",TitledBorder.LEADING,TitledBorder.ABOVE_TOP));

        panelLUCluster.add(panelLUClusteringSwitch);
        panelLUCluster.add(panelLUClusterEmpty);
        panelLUCluster.add(new JLabel("clustering factor :"));
        panelLUCluster.add(tf_lu_cluster_factor);
        panelLUCluster.add(new JLabel("init temperature :"));
        panelLUCluster.add(tf_lu_cluster_initTemperature);
        panelLUCluster.add(new JLabel("force scaling factor :"));
        panelLUCluster.add(tf_lu_cluster_forceScalingFactor);


        cb_lu_avoidOverlapping = new JCheckBox();
        cb_lu_avoidOverlapping.setOpaque(false);
        tf_lu_overlapDetectWidth = new JTextField();
        tf_lu_overlapPrefDistance = new JTextField();
        

        JPanel panelLUOverlapSwitch = new JPanel(new GridLayout(1,2,10,4));
        panelLUOverlapSwitch.setOpaque(false);
        panelLUOverlapSwitch.add(new JLabel("on/off"));
        panelLUOverlapSwitch.add(cb_lu_avoidOverlapping);
        
        
        
        JPanel panelLUEmpty = new JPanel();
        panelLUEmpty.setOpaque(false);
        
        panelLUOverlap.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Force for overlapping Cells",TitledBorder.LEADING,TitledBorder.ABOVE_TOP));
        panelLUOverlap.add(panelLUOverlapSwitch);
        panelLUOverlap.add(panelLUEmpty);
        panelLUOverlap.add(new JLabel("detection width :"));
        panelLUOverlap.add(tf_lu_overlapDetectWidth);
        panelLUOverlap.add(new JLabel("pref. distance :"));
        panelLUOverlap.add(tf_lu_overlapPrefDistance);
        
        cb_lu_avoidOverlapping.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                switchVisibility();
            }
        });
        
        
        
        cb_lu_useOptimizationAlg = new JCheckBox();
        cb_lu_useOptimizationAlg.setOpaque(false);
        cb_lu_useOptimizationAlg.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                switchVisibility();
            }
        });
        
        button_lu_ConfigOptimizeAlg = new JButton(button_ConfigOptimizeAlg.getText());
        button_lu_ConfigOptimizeAlg.setBackground(panelLayoutUpdate.getBackground());
        button_lu_ConfigOptimizeAlg.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                lu_optimizeAlgController.configure();
            }
        });
        
        JPanel emptyPanel3 = new JPanel();
        emptyPanel3.setOpaque(false);
        
        panelLUOptiAlg.add(new JLabel("Use optimization Algorithm:"));
        panelLUOptiAlg.add(cb_lu_useOptimizationAlg);
        panelLUOptiAlg.add(emptyPanel3);
        panelLUOptiAlg.add(button_lu_ConfigOptimizeAlg);
		
		panelLayoutRunWrapper = new JScrollPane(panelLayoutRun);
		Dimension innerSize = new Dimension(500, 350);
		panelLayoutRunWrapper .setPreferredSize(innerSize);
		panelLayoutUpdateWrapper = new JScrollPane(panelLayoutUpdate);
		panelLayoutUpdateWrapper.setPreferredSize(innerSize);
		
		tp_main.addTab("Apply Values", panelLayoutRunWrapper);
        
        setToolTipText();
                
        pack();
    }
    
/******************************************************************************/    

    protected JPanel getAdditionalForceConfig(){
        return new JPanel();
    }

/******************************************************************************/    

    protected JPanel getAdditionalForceLUConfig(){
        return new JPanel();
    }
    
/******************************************************************************/    

    protected void setToolTipText(){
        tf_initTemperature       .setToolTipText("Temperature Cells will be initialised with");
        tf_minTemperature        .setToolTipText("Algorithm stops when all Temperatures/the average of all Temperatures is below this value");
        tf_maxTemperature        .setToolTipText("Value, no temperature will surmount");
        tf_prefEdgeLength        .setToolTipText("preferred Length of Edges");
        tf_gravitation           .setToolTipText("attracting Force to the Barycenter of the Graph");
        tf_randomImpulseRange    .setToolTipText("Power of disturbing Forces");
        tf_overlapDetectWidth    .setToolTipText("Distance around a cell that will be searched for overlapping of other cells");
        tf_overlapPrefDistance   .setToolTipText("Distance the Algorithm try's to keep as mimimum between overlapped cells");
        tf_alphaOsc              .setToolTipText("Angle in witch Oscillations are detected");
        tf_alphaRot              .setToolTipText("Angle in witch Rotations are detected");
        tf_sigmaOsc              .setToolTipText("Penaltyvalue for Oscillations (with a higher Value Temperature decreases more slowly)");
        tf_sigmaRot              .setToolTipText("Penaltyvalue for Rotations (with a small Value Temperature decreases more slowly)");
    
        tf_lu_initTemperature    .setToolTipText(tf_initTemperature.getToolTipText());
        tf_lu_minTemperature     .setToolTipText(tf_minTemperature.getToolTipText());
        tf_lu_maxTemperature     .setToolTipText(tf_maxTemperature.getToolTipText());
        tf_lu_prefEdgeLength     .setToolTipText(tf_prefEdgeLength.getToolTipText());
        tf_lu_gravitation        .setToolTipText(tf_gravitation.getToolTipText());
        tf_lu_randomImpulseRange .setToolTipText(tf_randomImpulseRange.getToolTipText());
        tf_lu_overlapDetectWidth .setToolTipText(tf_overlapDetectWidth.getToolTipText());
        tf_lu_overlapPrefDistance.setToolTipText(tf_overlapPrefDistance.getToolTipText());
        tf_lu_alphaOsc           .setToolTipText(tf_alphaOsc.getToolTipText());
        tf_lu_alphaRot           .setToolTipText(tf_alphaRot.getToolTipText());
        tf_lu_sigmaOsc           .setToolTipText(tf_sigmaOsc.getToolTipText());
        tf_lu_sigmaRot           .setToolTipText(tf_sigmaRot.getToolTipText());
        tf_lu_depth              .setToolTipText("Cells depth Edges away from the inserted Cells will also be updated");
        tf_lu_cluster_initTemperature   .setToolTipText("Initial temperature for clusters. It is recommended, that this value is below the initial temperature for normal vertices.");
        tf_lu_cluster_forceScalingFactor.setToolTipText("Scales the forces, affecting clusters. Possible values are between 0.0 and 1.0. A small value ensures, that clusters move slow and over short distances.");
        tf_lu_cluster_factor            .setToolTipText("Factor the creation of clusters. A lower value leads to more clusters, with smaller size, a higher value to fewer, bigger clusters");
        
    
        cb_computePermutation    .setToolTipText("Algorithms will be calculated with the same/permutating order of Cells");
        cb_endPerAverage         .setToolTipText("Should the Algorithm stop when all Temperatures/the average of all Temperatures is below the min. Temperature");
        cb_avoidOverlapping      .setToolTipText("Switches the calculation of a Force, that appears when two Cells are overlapping");
        cb_useOptimizationAlg    .setToolTipText("Enables finetuning after the "+GEMLayoutAlgorithm.KEY_CAPTION+"-Algorithm with a short run of the "+AnnealingLayoutController.KEY_TITLE+"-Algorithm");
    
        cb_lu_enableLayoutUpdate .setToolTipText("Should the Algorithm be performed, when Cells collapses/expand");
        cb_lu_computePermutation .setToolTipText(cb_computePermutation.getToolTipText());
        cb_lu_endPerAverage      .setToolTipText(cb_endPerAverage.getToolTipText());
        cb_lu_avoidOverlapping   .setToolTipText(cb_avoidOverlapping.getToolTipText());
        cb_lu_useOptimizationAlg .setToolTipText(cb_useOptimizationAlg.getToolTipText());
        cb_lu_cluster_enable     .setToolTipText("Switches clustering for the layout update process on/off");
        
        comb_lu_method           .setToolTipText("Method of organizing place for inserted Cells");
        
        button_ConfigOptimizeAlg .setToolTipText("Configures the "+AnnealingLayoutController.KEY_TITLE+"-Algorithm for finetuning purpose");
        button_lu_ConfigOptimizeAlg.setToolTipText(button_ConfigOptimizeAlg.getToolTipText());
    }

/******************************************************************************/    
    /** Closes the dialog 
     * @see #action_cancel
     * */
    private void closeDialog() {
        action_cancel();
    }
            
/******************************************************************************/    

    protected boolean assertDouble(String text,String name,ArrayList errList){
        try {
            Double.parseDouble(text);
        }
        catch( NumberFormatException e ){
            String errMsg = name+" is NOT a double value";
            errList.add(errMsg);
            return false;
        }
        return true;
    }
        
/******************************************************************************/    

    protected boolean assertInteger(String text,String name,ArrayList errList){
        try {
            Integer.parseInt(text);
        }
        catch( NumberFormatException e ){
            String errMsg = name+" is NOT a integer value";
            errList.add(errMsg);
            return false;
        }
        return true;
    }
        
/******************************************************************************/    

    protected boolean assertRange(String value, double minValue, 
                                    double maxValue, boolean includeMinimum, 
                                    boolean includeMaximum, String name, 
                                    ArrayList errList){
        double val = Double.parseDouble(value);
        boolean isOK = true;
        if( includeMinimum ){
            isOK = minValue <= val;
        }
        else {
            isOK = minValue < val;
        }
        if( isOK ){
            if( includeMaximum ){
                isOK = maxValue >= val;
            }
            else {
                isOK = maxValue > val;
            }            
            if( isOK )
                return true;
        }
        
        //if isOK == false
        String errMsg = name+" is out of the interval ";
        if( includeMinimum ){
            errMsg += "[";
        }
        else {
            errMsg += "]";
        }
        errMsg += minValue+";"+maxValue;
        if( includeMaximum ){
            errMsg += "]";
        }
        else {
            errMsg += "[";
        }
        errList.add(errMsg);
        return false;        
    }

/******************************************************************************/    

    protected boolean assertRange(String value, int minValue, int maxValue, 
                                    boolean includeMinimum, 
                                    boolean includeMaximum, 
                                    String name, ArrayList errList){
        int val = Integer.parseInt(value);
        boolean isOK = true;
        if( includeMinimum ){
            isOK = minValue <= val;
        }
        else {
            isOK = minValue < val;
        }
        if( isOK ){
            if( includeMaximum ){
                isOK = maxValue >= val;
            }
            else {
                isOK = maxValue > val;
            }
            if( isOK )
                return true;
        }
        
        //if isOK == false        
        String errMsg = name+" is out of the interval ";
        if( includeMinimum ){
            errMsg += "[";
        }
        else {
            errMsg += "]";
        }
        errMsg += minValue+";"+maxValue;
        if( includeMaximum ){
            errMsg += "]";
        }
        else {
            errMsg += "[";
        }
        errList.add(errMsg);
        return false;
    }

/******************************************************************************/    

    protected boolean assertMinimum(String value,double minValue,boolean include,String name,ArrayList errList){
        double val = Double.parseDouble(value);
        boolean isOK = true;
        if( include ){
            isOK = minValue <= val;
        }
        else {
            isOK = minValue < val;
        }
        if( isOK )
            return true;
        String errMsg = name+" has to be bigger";
        if( include ){
            errMsg += " or equal to ";
        }
        else {
            errMsg += " then ";
        }
        errMsg += String.valueOf(minValue);
        errList.add(errMsg);
        return false;
    }

/******************************************************************************/    

    protected boolean assertMinimum(String value,int minValue,boolean include,String name,ArrayList errList){
        int val = Integer.parseInt(value);
        boolean isOK = true;
        if( include ){
            isOK = minValue <= val;
        }
        else {
            isOK = minValue < val;
        }
        if( isOK )
            return true;
        String errMsg = name+" has to be bigger";
        if( include ){
            errMsg += " or equal to ";
        }
        else {
            errMsg += " then ";
        }
        errMsg += String.valueOf(minValue);
        errList.add(errMsg);
        return false;
    }

/******************************************************************************/    

    public double getInitTemperature(){
        return Double.parseDouble(tf_initTemperature.getText());
    }
        
/******************************************************************************/    

    public void setInitTemperature(double temperature){
        tf_initTemperature.setText(String.valueOf(temperature));
    }
        
/******************************************************************************/    

    public double getMinTemperature(){
        return Double.parseDouble(tf_minTemperature.getText());
    }
    
/******************************************************************************/    
    
    public void setMinTemperature(double temperature){
        tf_minTemperature.setText(String.valueOf(temperature));
    }
    
/******************************************************************************/    
    
    public double getMaxTemperature(){
        return Double.parseDouble(tf_maxTemperature.getText());
    }
    
/******************************************************************************/    
    
    public void setMaxTemperature(double temperature){
        tf_maxTemperature.setText(String.valueOf(temperature));
    }
    
/******************************************************************************/    
    
    public double getPreferredEdgeLength(){
        return Double.parseDouble(tf_prefEdgeLength.getText());
    }
    
/******************************************************************************/    
    
    public void setPreferredEdgeLength(double length){
        tf_prefEdgeLength.setText(String.valueOf(length));
    }
    
/******************************************************************************/    
    
    public double getGravitation(){
        return Double.parseDouble(tf_gravitation.getText());
    }
    
/******************************************************************************/    
    
    public void setGravitation(double grav){
        tf_gravitation.setText(String.valueOf(grav));
    }
    
/******************************************************************************/    
    
    public double getRandomImpulseRange(){
        return Double.parseDouble(tf_randomImpulseRange.getText());
    }
    
/******************************************************************************/    
    
    public void setRandomImpulseRange(double range){
        tf_randomImpulseRange.setText(String.valueOf(range));
    }
    
/******************************************************************************/    

    public double getOverlapDetectionWidth(){
        return Double.parseDouble(tf_overlapDetectWidth.getText());
    }
    
/******************************************************************************/    
    
    public void setOverlapDetectionWidth(double width){
        tf_overlapDetectWidth.setText(String.valueOf(width));
    }
    
/******************************************************************************/    
    
    public double getOverlapPreferredDistance(){
        return Double.parseDouble(tf_overlapPrefDistance.getText());
    }
    
/******************************************************************************/    

    public void setOverlapPreferredDistance(double distance){
        tf_overlapPrefDistance.setText(String.valueOf(distance));
    }
    
/******************************************************************************/    
    
    public void setAvoidOverlapping(boolean avoid){
        cb_avoidOverlapping.setSelected(avoid);
        switchVisibility();
    }
    
/******************************************************************************/    
    
    public boolean getAvoidOverlapping(){
        return cb_avoidOverlapping.isSelected();
    }
    
/******************************************************************************/    
    
    public double getAlphaOsc(){
        return Math.toRadians(Double.parseDouble(tf_alphaOsc.getText()));
    }
    
/******************************************************************************/    
    
    public void setAlphaOsc(double alpha){
        tf_alphaOsc.setText(String.valueOf(Math.toDegrees(alpha)));
    }
    
/******************************************************************************/    
    
    public double getAlphaRot(){
        return Math.toRadians(Double.parseDouble(tf_alphaRot.getText()));
    }
    
/******************************************************************************/    
    
    public void setAlphaRot(double alpha){
        tf_alphaRot.setText(String.valueOf(Math.toDegrees(alpha)));
    }
    
/******************************************************************************/    
    
    public double getSigmaOsc(){
        return Double.parseDouble(tf_sigmaOsc.getText());
    }
    
/******************************************************************************/    
    
    public void setSigmaOsc(double sigma){
        tf_sigmaOsc.setText(String.valueOf(sigma));
    }
    
/******************************************************************************/    
    
    public double getSigmaRot(){
        return Double.parseDouble(tf_sigmaRot.getText());
    }
    
/******************************************************************************/    
    
    public void setSigmaRot(double sigma){
        tf_sigmaRot.setText(String.valueOf(sigma));
    }
    
/******************************************************************************/    
    
    public boolean getComputePermutation(){
        return cb_computePermutation.isSelected();        
    }
    
/******************************************************************************/    
    
    public void setComputePermutation(boolean isSelected){
        cb_computePermutation.setSelected(isSelected);
    }
    
/******************************************************************************/    
    
    public boolean getEndPerAverage(){
        return cb_endPerAverage.isSelected();
    }
    
/******************************************************************************/    
    
    public void setEndPerAverage(boolean isSelected){
        cb_computePermutation.setSelected(isSelected);
    }
    
/******************************************************************************/    

    public double getLayoutUpdateInitTemperature(){
        return Double.parseDouble(tf_lu_initTemperature.getText());
    }
        
/******************************************************************************/    

    public void setLayoutUpdateInitTemperature(double temperature){
        tf_lu_initTemperature.setText(String.valueOf(temperature));
    }
        
/******************************************************************************/    

    public double getLayoutUpdateMinTemperature(){
        return Double.parseDouble(tf_lu_minTemperature.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateMinTemperature(double temperature){
        tf_lu_minTemperature.setText(String.valueOf(temperature));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateMaxTemperature(){
        return Double.parseDouble(tf_lu_maxTemperature.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateMaxTemperature(double temperature){
        tf_lu_maxTemperature.setText(String.valueOf(temperature));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdatePreferredEdgeLength(){
        return Double.parseDouble(tf_lu_prefEdgeLength.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdatePreferredEdgeLength(double length){
        tf_lu_prefEdgeLength.setText(String.valueOf(length));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateGravitation(){
        return Double.parseDouble(tf_lu_gravitation.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateGravitation(double grav){
        tf_lu_gravitation.setText(String.valueOf(grav));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateRandomImpulseRange(){
        return Double.parseDouble(tf_lu_randomImpulseRange.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateRandomImpulseRange(double range){
        tf_lu_randomImpulseRange.setText(String.valueOf(range));
    }
    
/******************************************************************************/    

    public double getLayoutUpdateOverlapDetectionWidth(){
        return Double.parseDouble(tf_lu_overlapDetectWidth.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateOverlapDetectionWidth(double width){
        tf_lu_overlapDetectWidth.setText(String.valueOf(width));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateOverlapPreferredDistance(){
        return Double.parseDouble(tf_lu_overlapPrefDistance.getText());
    }
    
/******************************************************************************/    

    public void setLayoutUpdateOverlapPreferredDistance(double distance){
        tf_lu_overlapPrefDistance.setText(String.valueOf(distance));
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateAvoidOverlapping(boolean avoid){
        cb_lu_avoidOverlapping.setSelected(avoid);
        switchVisibility();
    }
    
/******************************************************************************/    
    
    public boolean getLayoutUpdateAvoidOverlapping(){
        return cb_lu_avoidOverlapping.isSelected();
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateAlphaOsc(){
        return Math.toRadians(Double.parseDouble(tf_lu_alphaOsc.getText()));
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateAlphaOsc(double alpha){
        tf_lu_alphaOsc.setText(String.valueOf(Math.toDegrees(alpha)));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateAlphaRot(){
        return Math.toRadians(Double.parseDouble(tf_lu_alphaRot.getText()));
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateAlphaRot(double alpha){
        tf_lu_alphaRot.setText(String.valueOf(Math.toDegrees(alpha)));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateSigmaOsc(){
        return Double.parseDouble(tf_lu_sigmaOsc.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateSigmaOsc(double sigma){
        tf_lu_sigmaOsc.setText(String.valueOf(sigma));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateSigmaRot(){
        return Double.parseDouble(tf_lu_sigmaRot.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateSigmaRot(double sigma){
        tf_lu_sigmaRot.setText(String.valueOf(sigma));
    }
    
/******************************************************************************/    
    
    public boolean getLayoutUpdateComputePermutation(){
        return cb_lu_computePermutation.isSelected();        
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateComputePermutation(boolean isSelected){
        cb_lu_computePermutation.setSelected(isSelected);
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateEndPerAverage(boolean computeAverage){
        cb_lu_endPerAverage.setSelected(computeAverage);
    }

/******************************************************************************/    
    
    public boolean getLayoutUpdateEndPerAverage(){
        return cb_lu_endPerAverage.isSelected();
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateEnabled(boolean isEnabled){
        cb_lu_enableLayoutUpdate.setSelected(isEnabled);
        switchLayoutUpdatePanel();
    }

/******************************************************************************/    

    public boolean getLayoutUpdateEnabled(){
        return cb_lu_enableLayoutUpdate.isSelected();
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateMethod(String key){
        comb_lu_method.setSelectedItem(key);
        switchVisibility();
    }

/******************************************************************************/    
    
    public String getLayoutUpdateMethod(){
        return (String) comb_lu_method.getSelectedItem();
    }

/******************************************************************************/    
    
    public void setLayoutUpdateDepth(int depth){
        tf_lu_depth.setText(String.valueOf(depth));
    }
    
/******************************************************************************/    
    
    public int getLayoutUpdateDepth(){
        return Integer.parseInt(tf_lu_depth.getText());
    }
    
/******************************************************************************/    

    public void setOptimizationAlgorithmEnabled(boolean enable){
        cb_useOptimizationAlg.setSelected(enable);
        switchVisibility();
    }
    
/******************************************************************************/    
    
    public boolean getOptimizationAlgorithmEnabled(){
        return cb_useOptimizationAlg.isSelected();
    }
    
/******************************************************************************/    
    
    public Properties getOptimizationConfiguration(){
        return optimizeAlgController.getConfiguration();
    }

/******************************************************************************/    

    public void setOptimizationConfiguration(Properties config){
        optimizeAlgController.setConfiguration(config);
    }
    
/******************************************************************************/    

    public void setLayoutUpdateOptimizationAlgorithmEnabled(boolean enable){
        cb_lu_useOptimizationAlg.setSelected(enable);
        switchVisibility();
    }
    
/******************************************************************************/    
    
    public boolean getLayoutUpdateOptimizationAlgorithmEnabled(){
        return cb_lu_useOptimizationAlg.isSelected();
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateOptimizationConfiguration(Properties config){
        lu_optimizeAlgController.setConfiguration(config);
    }
    
/******************************************************************************/    
    
    public Properties getLayoutUpdateOptimizationConfiguration(){
        return lu_optimizeAlgController.getConfiguration();
    }
    
/******************************************************************************/    

    public void setLayoutUpdateMethodPerimeterInitSize(double size){
        tf_lu_perimeter_initSize.setText(String.valueOf(size));
    }

/******************************************************************************/    

    public double getLayoutUpdateMethodPerimeterInitSize(){
        return Double.parseDouble(tf_lu_perimeter_initSize.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateMethodPerimeterSizeInc(double incValue){
        tf_lu_perimeter_sizeInc.setText(String.valueOf(incValue));
    }

/******************************************************************************/    

    public double getLayoutUpdateMethodPerimeterSizeInc(){
        return Double.parseDouble(tf_lu_perimeter_sizeInc.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateClusteringEnabled(boolean isEnabled){
        cb_lu_cluster_enable.setSelected(isEnabled);
        switchVisibility();
    }
    
/******************************************************************************/    

    public boolean getLayoutUpdateClusteringEnabled(){
        return cb_lu_cluster_enable.isSelected();
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateClusteringInitTemperature(double temp){
        tf_lu_cluster_initTemperature.setText(String.valueOf(temp));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateClusteringInitTemperature(){
        return Double.parseDouble(tf_lu_cluster_initTemperature.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateClusteringForceScalingFactor(double factor){
        tf_lu_cluster_forceScalingFactor.setText(String.valueOf(factor));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateClusteringForceScalingFactor(){
        return Double.parseDouble(tf_lu_cluster_forceScalingFactor.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateClusteringFactor(double factor){
        tf_lu_cluster_factor.setText(String.valueOf(factor));
    }
    
/******************************************************************************/    
    
    public double getLayoutUpdateClusteringFactor(){
        return Double.parseDouble(tf_lu_cluster_factor.getText());
    }
    
/******************************************************************************/    

    public static void main(String[] args){
        Properties[] configs = new Properties[1];
        for( int i = 0; i < configs.length; i++ )
            configs[i] = new Properties();
            
        configs[0].put(GEMLayoutController.KEY_CONFIG_NAME           ,"Default Values");
        configs[0].put(GEMLayoutController.KEY_INIT_TEMPERATURE      , "50.0"   );
        configs[0].put(GEMLayoutController.KEY_MIN_TEMPERATURE       ,  "3.0"   );
        configs[0].put(GEMLayoutController.KEY_MAX_TEMPERATURE       ,"256.0"   );
        configs[0].put(GEMLayoutController.KEY_PREF_EDGE_LENGTH      ,"100.0"   );
        configs[0].put(GEMLayoutController.KEY_GRAVITATION           ,  "0.0625");
        configs[0].put(GEMLayoutController.KEY_RANDOM_IMPULSE_RANGE  , "32.0"   );
        configs[0].put(GEMLayoutController.KEY_COMPUTE_PERMUTATION   , "true"   );
        configs[0].put(GEMLayoutController.KEY_END_CONDITION_AVERAGE , "true"   );
        configs[0].put(GEMLayoutController.KEY_AVOID_OVERLAPPING     , "false"   );
        configs[0].put(GEMLayoutController.KEY_OVERLAPPING_DETECTION_WIDTH, "40.0");
        configs[0].put(GEMLayoutController.KEY_OVERLAPPING_PREF_DISTANCE, "40.0");
        configs[0].put(GEMLayoutController.KEY_ALPHA_OSC             ,String.valueOf(Math.PI/2.0));
        configs[0].put(GEMLayoutController.KEY_ALPHA_ROT             ,String.valueOf(Math.PI/3.0));
        configs[0].put(GEMLayoutController.KEY_SIGMA_OSC             ,String.valueOf(1.0/3.0)); //je höher desto langsamer fällt temperatur
        configs[0].put(GEMLayoutController.KEY_SIGMA_ROT             ,String.valueOf(1.0/2.0)); //je kleiner desto kleiner temperaturänderung aufgrund rotation
        configs[0].put(GEMLayoutController.KEY_OPTIMIZE_ALGORITHM_ENABLED,"false");
//        configs[0].put(GEMLayoutController.KEY_OPTIMIZE_ALGORITHM_CONFIG,optimizeConfig.clone());
        
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      ,"10.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,"3.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_MAX_TEMPERATURE       ,"256.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH      ,"100.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_GRAVITATION           ,"0.0625");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE  ,"32.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,"true");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE ,"true");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING     ,"false");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH,"40.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE,  "40.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_OSC             ,String.valueOf(Math.toRadians(90.0)));
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_ROT             ,String.valueOf(Math.toRadians(60.0)));
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_OSC             ,String.valueOf(1.0/3.0));
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_ROT             ,String.valueOf(1.0/2.0));
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_ENABLED               ,"false");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_DEPTH                 ,"1");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD                ,GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY);
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED,"false");
//        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG,lu_optimizeConfig.clone());
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED              ,"false");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE     ,"15.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR ,"0.1");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR               ,"8.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE      ,"100.0");
        configs[0].put(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC       ,"20.0");
       
        GEMLayoutConfigurationDialog dialog = new GEMLayoutConfigurationDialog(new JFrame(),configs,null,null);
        dialog.setDefaultCloseOperation(GEMLayoutConfigurationDialog.EXIT_ON_CLOSE);
        dialog.action_LoadPreSets(0);
        dialog.setVisible(true);
        
    }
    
/******************************************************************************/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/******************************************************************************/
}
