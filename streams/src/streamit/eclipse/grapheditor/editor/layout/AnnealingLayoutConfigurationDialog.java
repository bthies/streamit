/*
 * @(#)AnnealingLayoutConfigurationDialog.java 1.0 12.08.2003
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
import java.awt.Rectangle;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;


/******************************************************************************/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/******************************************************************************/
/**
 * @author winkler
 * @author sven luzar
 *
 */


public class AnnealingLayoutConfigurationDialog extends JDialog {

    private JTextField tf_initTemperature;
    private JTextField tf_minTemperature;
    private JTextField tf_minDistance;
    private JTextField tf_tempScaleFactor;
    private JTextField tf_maxRounds;
    private JTextField tf_triesPerCell;
    private JTextField tf_lambdaNodeDistribution;
    private JTextField tf_lambdaBorderline;
    private JTextField tf_lambdaEdgeLength;
    private JTextField tf_lambdaEdgeCrossing;
    private JTextField tf_lambdaEdgeDistribution;
    private JTextField tf_lambdaNodeDistance;
    private JTextField tf_boundsWidth;
    private JTextField tf_boundsHeight;
    
    private JCheckBox  cb_computePermutation;
    private JCheckBox  cb_uphillMovesAllowed;
    private JCheckBox  cb_useNodeDistribution;
    private JCheckBox  cb_useBorderline;
    private JCheckBox  cb_useEdgeLength;
    private JCheckBox  cb_useEdgeCrossing;
    private JCheckBox  cb_useEdgeDistribution;
    private JCheckBox  cb_useNodeDistance;
    
    private JButton    button_takeViewportSize;
    
    private JCheckBox  cb_enableLayoutUpdate;
    

    private JTextField tf_lu_initTemperature;
    private JTextField tf_lu_minTemperature;
    private JTextField tf_lu_minDistance;
    private JTextField tf_lu_tempScaleFactor;
    private JTextField tf_lu_maxRounds;
    private JTextField tf_lu_triesPerCell;
    private JTextField tf_lu_lambdaNodeDistribution;
    private JTextField tf_lu_lambdaBorderline;
    private JTextField tf_lu_lambdaEdgeLength;
    private JTextField tf_lu_lambdaEdgeCrossing;
    private JTextField tf_lu_lambdaEdgeDistribution;
    private JTextField tf_lu_lambdaNodeDistance;
    private JTextField tf_lu_boundsWidth;
    private JTextField tf_lu_boundsHeight;
    private JTextField tf_lu_method_neighborsDepth;
    private JTextField tf_lu_method_perimeterRadius;
    private JTextField tf_lu_method_perimeterRadiusInc;
    private JTextField tf_lu_clustering_factor;
    private JTextField tf_lu_clustering_moveScale;
    
    private JCheckBox  cb_lu_computePermutation;
    private JCheckBox  cb_lu_uphillMovesAllowed;
    private JCheckBox  cb_lu_useNodeDistribution;
    private JCheckBox  cb_lu_useBorderline;
    private JCheckBox  cb_lu_useEdgeLength;
    private JCheckBox  cb_lu_useEdgeCrossing;
    private JCheckBox  cb_lu_useEdgeDistribution;
    private JCheckBox  cb_lu_useNodeDistance;
    private JCheckBox  cb_lu_clustering_enable;
    
    private JButton    button_lu_takeViewportSize;


    private JComboBox  comb_loadPreSets;
    private JComboBox  comb_lu_Method;
    
    protected Properties[] preSetConfigs;

    private JPanel      panelSurface;    
    private JPanel      panelLUSurface;

	private JTabbedPane tp_main;
	
	JScrollPane panelSurfaceWrapper;	
	JScrollPane panelLUSurfaceWrapper;	


    /** configurations
     */
    private static final String CAPTION = "Simulated Annealing Layout Configuration";
    
    
    /** Boolean for the cancel operation variables
     */
    private boolean canceled = false;
    
    private boolean isOptimizer;
    
/******************************************************************************/    
    /**
     * Creates new form AnnealingLayoutConfigurationDialog
     */
    public AnnealingLayoutConfigurationDialog(Frame parent, Properties[] configs, boolean isOptimizationAlgorithm) {
        
        super(parent, true);
        
        isOptimizer = isOptimizationAlgorithm;
        
        preSetConfigs = configs;
        
        initComponents();
        action_LoadPreSets(0); //default values
        
        // size, title and location
        setTitle(CAPTION);
        setName(CAPTION);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int)((double)(screen.width  - this.getWidth() ) / 2.0);
        int y = (int)((double)(screen.height - this.getHeight()) / 2.0);
        setLocation(x, y);
    }
    
/******************************************************************************/        

    protected void action_LoadPreSets(int index){
        
        Properties config = preSetConfigs[index];      
          
            
        tf_initTemperature       .setText((String)config.get(AnnealingLayoutController.KEY_INIT_TEMPERATURE));
        tf_minTemperature        .setText((String)config.get(AnnealingLayoutController.KEY_MIN_TEMPERATURE));
        tf_minDistance           .setText((String)config.get(AnnealingLayoutController.KEY_MIN_DISTANCE));
        tf_tempScaleFactor       .setText((String)config.get(AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR));
        tf_maxRounds             .setText((String)config.get(AnnealingLayoutController.KEY_MAX_ROUNDS));
        tf_triesPerCell          .setText((String)config.get(AnnealingLayoutController.KEY_TRIES_PER_CELL));
        ArrayList lambda = (ArrayList) config.get(AnnealingLayoutController.KEY_LAMBDA);
        tf_lambdaNodeDistribution.setText(String.valueOf(((Double)lambda.get(0)).doubleValue()));
        tf_lambdaBorderline      .setText(String.valueOf(((Double)lambda.get(1)).doubleValue()));
        tf_lambdaEdgeLength      .setText(String.valueOf(((Double)lambda.get(2)).doubleValue()));
        tf_lambdaEdgeCrossing    .setText(String.valueOf(((Double)lambda.get(3)).doubleValue()));
        tf_lambdaEdgeDistribution.setText(String.valueOf(((Double)lambda.get(4)).doubleValue()));
        tf_lambdaNodeDistance    .setText(String.valueOf(((Double)lambda.get(5)).doubleValue()));
        
        Rectangle bounds = (Rectangle) config.get(AnnealingLayoutController.KEY_BOUNDS);
        tf_boundsWidth.setText(String.valueOf((int)bounds.getWidth()));
        tf_boundsHeight.setText(String.valueOf((int)bounds.getHeight()));
                
        cb_computePermutation.setSelected(isTrue((String)config.get(AnnealingLayoutController.KEY_COMPUTE_PERMUTATION)));
        cb_uphillMovesAllowed.setSelected(isTrue((String)config.get(AnnealingLayoutController.KEY_IS_UPHILL_MOVE_ALLOWED)));
        
        int costFunctionConfig = Integer.parseInt((String)config.get(AnnealingLayoutController.KEY_COST_FUNCTION_CONFIG),2);
        setCostFunctionConfiguration(costFunctionConfig);
        
        cb_enableLayoutUpdate.setSelected(isTrue((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_ENABLED)));
        
        tf_lu_initTemperature       .setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE));
        tf_lu_minTemperature        .setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE));
        tf_lu_minDistance           .setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_DISTANCE));
        tf_lu_tempScaleFactor       .setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR));
        tf_lu_maxRounds             .setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MAX_ROUNDS));
        tf_lu_triesPerCell          .setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_TRIES_PER_CELL));
        ArrayList lambdaLU = (ArrayList) config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_LAMBDA);
        tf_lu_lambdaNodeDistribution.setText(String.valueOf(((Double)lambdaLU.get(0)).doubleValue()));
        tf_lu_lambdaBorderline      .setText(String.valueOf(((Double)lambdaLU.get(1)).doubleValue()));
        tf_lu_lambdaEdgeLength      .setText(String.valueOf(((Double)lambdaLU.get(2)).doubleValue()));
        tf_lu_lambdaEdgeCrossing    .setText(String.valueOf(((Double)lambdaLU.get(3)).doubleValue()));
        tf_lu_lambdaEdgeDistribution.setText(String.valueOf(((Double)lambdaLU.get(4)).doubleValue()));
        tf_lu_lambdaNodeDistance    .setText(String.valueOf(((Double)lambdaLU.get(5)).doubleValue()));
        
        Rectangle boundsLU = (Rectangle) config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_BOUNDS);
        tf_lu_boundsWidth.setText(String.valueOf((int)boundsLU.getWidth()));
        tf_lu_boundsHeight.setText(String.valueOf((int)boundsLU.getHeight()));
                
        cb_lu_computePermutation.setSelected(isTrue((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION)));
        cb_lu_uphillMovesAllowed.setSelected(isTrue((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED)));
        
        int costFunctionConfigLU = Integer.parseInt((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG),2);
        setLayoutUpdateCostFunctionConfiguration(costFunctionConfigLU);
        
        tf_lu_method_neighborsDepth.setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH));
        tf_lu_method_perimeterRadius.setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS));
        tf_lu_method_perimeterRadiusInc.setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE));
        
        tf_lu_clustering_factor.setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR));
        tf_lu_clustering_moveScale.setText((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE));
        
        cb_lu_clustering_enable.setSelected(isTrue((String)config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED)));
        
        comb_lu_Method.setSelectedItem(config.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD));
        
        
        switchLayoutUpdatePanel();
        action_CheckBoxSwitch();
    }
        
/******************************************************************************/        

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
    /** Called by pressing the cancel button
     *
     */
    private void action_cancel() {
        setVisible(false);
        dispose();
        canceled = true;
    }
    
/******************************************************************************/    
    /** Called by pressing the ok button
     *
     */
    protected void action_ok() {
           
        boolean isOK = true;
        ArrayList errList = new ArrayList();

        isOK &= assertDouble(tf_initTemperature.getText(),       AnnealingLayoutController.KEY_INIT_TEMPERATURE,errList);
        isOK &= assertDouble(tf_minTemperature.getText(),        AnnealingLayoutController.KEY_MIN_TEMPERATURE,errList);
        isOK &= assertDouble(tf_minDistance.getText(),           AnnealingLayoutController.KEY_MIN_DISTANCE,errList);
        isOK &= assertDouble(tf_tempScaleFactor.getText(),       AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR,errList);
        isOK &= assertInteger(tf_maxRounds.getText(),             AnnealingLayoutController.KEY_MAX_ROUNDS,errList);
        isOK &= assertInteger(tf_triesPerCell.getText(),         AnnealingLayoutController.KEY_TRIES_PER_CELL,errList);
        isOK &= assertDouble(tf_lambdaNodeDistribution.getText(),"Node Distribution",errList);
        isOK &= assertDouble(tf_lambdaBorderline.getText(),      "Borderline",errList);
        isOK &= assertDouble(tf_lambdaEdgeLength.getText(),      "Edgelength",errList);
        isOK &= assertDouble(tf_lambdaEdgeCrossing.getText(),    "Edgecrossing",errList);
        isOK &= assertDouble(tf_lambdaEdgeDistribution.getText(),"Node-Edge Distribution",errList);
        isOK &= assertDouble(tf_lambdaNodeDistance.getText(),    "Node Overlapping",errList);
        isOK &= assertInteger(tf_boundsWidth.getText(),           "max. width",errList);
        isOK &= assertInteger(tf_boundsHeight.getText(),          "max. height",errList);

        isOK &= assertDouble(tf_lu_initTemperature.getText(),       AnnealingLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE,errList);
        isOK &= assertDouble(tf_lu_minTemperature.getText(),        AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE,errList);
        isOK &= assertDouble(tf_lu_minDistance.getText(),           AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_DISTANCE,errList);
        isOK &= assertDouble(tf_lu_tempScaleFactor.getText(),       AnnealingLayoutController.KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR,errList);
        isOK &= assertInteger(tf_lu_maxRounds.getText(),             AnnealingLayoutController.KEY_LAYOUT_UPDATE_MAX_ROUNDS,errList);
        isOK &= assertInteger(tf_lu_triesPerCell.getText(),         AnnealingLayoutController.KEY_LAYOUT_UPDATE_TRIES_PER_CELL,errList);
        isOK &= assertDouble(tf_lu_lambdaNodeDistribution.getText(),"Layout Update Node Distribution",errList);
        isOK &= assertDouble(tf_lu_lambdaBorderline.getText(),      "Layout Update Borderline",errList);
        isOK &= assertDouble(tf_lu_lambdaEdgeLength.getText(),      "Layout Update Edgelength",errList);
        isOK &= assertDouble(tf_lu_lambdaEdgeCrossing.getText(),    "Layout Update Edgecrossing",errList);
        isOK &= assertDouble(tf_lu_lambdaEdgeDistribution.getText(),"Layout Update Node-Edge Distribution",errList);
        isOK &= assertDouble(tf_lu_lambdaNodeDistance.getText(),    "Layout Update Node Overlapping",errList);
        isOK &= assertInteger(tf_lu_boundsWidth.getText(),           "Layout Update max. width",errList);
        isOK &= assertInteger(tf_lu_boundsHeight.getText(),          "Layout Update max. height",errList);
        isOK &= assertDouble(tf_lu_clustering_factor.getText(), AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,errList);
        isOK &= assertDouble(tf_lu_clustering_moveScale.getText(),AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE,errList);

        if( isOK ){        
            isOK &= assertDoublePositiveSign(tf_initTemperature.getText(),false,AnnealingLayoutController.KEY_INIT_TEMPERATURE,errList);
            isOK &= assertDoublePositiveSign(tf_initTemperature.getText(),false,AnnealingLayoutController.KEY_INIT_TEMPERATURE,errList);
            isOK &= assertDoublePositiveSign(tf_minTemperature.getText(),false,AnnealingLayoutController.KEY_MIN_TEMPERATURE,errList);
            isOK &= assertDoublePositiveSign(tf_minDistance.getText(),false,AnnealingLayoutController.KEY_MIN_DISTANCE,errList);
            isOK &= assertRange(tf_tempScaleFactor.getText(),0.0,1.0,false,false,AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR,errList);
            isOK &= assertIntegerPositiveSign(tf_maxRounds.getText(),false,AnnealingLayoutController.KEY_MAX_ROUNDS,errList);
            isOK &= assertRange(tf_triesPerCell.getText(),8,99,true,true,AnnealingLayoutController.KEY_TRIES_PER_CELL,errList);
            isOK &= assertIntegerPositiveSign(tf_boundsWidth.getText(),false,"max. width",errList);
            isOK &= assertIntegerPositiveSign(tf_boundsWidth.getText(),false,"max. height",errList);
            isOK &= assertDoublePositiveSign(tf_lambdaNodeDistribution.getText(),false,"Node Distribution",errList);
            isOK &= assertDoublePositiveSign(tf_lambdaBorderline.getText()      ,false,"Borderline",errList);
            isOK &= assertDoublePositiveSign(tf_lambdaEdgeLength.getText()      ,false,"Edgelength",errList);
            isOK &= assertDoublePositiveSign(tf_lambdaEdgeCrossing.getText()    ,false,"Edgecrossing",errList);
            isOK &= assertDoublePositiveSign(tf_lambdaEdgeDistribution.getText(),false,"Node-Edge Distribution",errList);
            isOK &= assertDoublePositiveSign(tf_lambdaNodeDistance.getText()    ,false,"Node Overlapping",errList);
            
            isOK &= assertDoublePositiveSign(tf_lu_initTemperature.getText(),false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE,errList);
            isOK &= assertDoublePositiveSign(tf_lu_initTemperature.getText(),false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE,errList);
            isOK &= assertDoublePositiveSign(tf_lu_minTemperature.getText(),false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE,errList);
            isOK &= assertDoublePositiveSign(tf_lu_minDistance.getText(),false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_DISTANCE,errList);
            isOK &= assertRange(tf_lu_tempScaleFactor.getText(),0.0,1.0,false,false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR,errList);
            isOK &= assertIntegerPositiveSign(tf_lu_maxRounds.getText(),false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_MAX_ROUNDS,errList);
            isOK &= assertRange(tf_lu_triesPerCell.getText(),8,99,true,true,AnnealingLayoutController.KEY_LAYOUT_UPDATE_TRIES_PER_CELL,errList);
            isOK &= assertIntegerPositiveSign(tf_lu_boundsWidth.getText(),false,"Layout Update max. width",errList);
            isOK &= assertIntegerPositiveSign(tf_lu_boundsWidth.getText(),false,"Layout Update max. height",errList);
            isOK &= assertDoublePositiveSign(tf_lu_lambdaNodeDistribution.getText(),false,"Layout Update Node Distribution",errList);
            isOK &= assertDoublePositiveSign(tf_lu_lambdaBorderline.getText()      ,false,"Layout Update Borderline",errList);
            isOK &= assertDoublePositiveSign(tf_lu_lambdaEdgeLength.getText()      ,false,"Layout Update Edgelength",errList);
            isOK &= assertDoublePositiveSign(tf_lu_lambdaEdgeCrossing.getText()    ,false,"Layout Update Edgecrossing",errList);
            isOK &= assertDoublePositiveSign(tf_lu_lambdaEdgeDistribution.getText(),false,"Layout Update Node-Edge Distribution",errList);
            isOK &= assertDoublePositiveSign(tf_lu_lambdaNodeDistance.getText()    ,false,"Layout Update Node Overlapping",errList);
            
            isOK &= assertDoublePositiveSign(tf_lu_clustering_factor.getText(),false,AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,errList);
            isOK &= assertRange(tf_lu_clustering_moveScale.getText(),0.0,1.0,false,true,AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE,errList);
        }
        
        isOK &= checkAdditionalFields(errList);


        if( isOK ){
            setVisible(false);
            dispose();
            canceled = false;
        }
        else {
            String errorMsg = new String();
            for( int i = 0; i < errList.size(); i++ ){
                errorMsg += (String)errList.get(i);
                if( i != errList.size() - 1 )
                    errorMsg += "\n";
            }
                
            JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
/******************************************************************************/    
    
    protected boolean checkAdditionalFields(ArrayList errList){
        return true;    
    }
        
/******************************************************************************/    
    /**
     * Returns true if the dialog has been canceled.
     */
    public boolean isCanceled() {
        return canceled;
    }
        
/******************************************************************************/    
    /** Initialize the Swing Components
     */    
    private void initComponents() {
        panelSurface = new JPanel(new BorderLayout(5,10));
        panelSurface.setBackground(new Color(205,207,215));
        getContentPane().setLayout(new BorderLayout(5,10));
		tp_main = new JTabbedPane();
        getContentPane().add(tp_main, BorderLayout.CENTER);
        //getContentPane().add(panelSurface,BorderLayout.CENTER);
        
        /*
        if( !isOptimizer )
            panelSurface.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "Values for main run"));
        */
    
        JPanel panelLabels  = new JPanel(new GridLayout(8,1, 0,5));
        JPanel panelFields  = new JPanel(new GridLayout(8,1, 0,5));
        JPanel panelVars    = new JPanel(new BorderLayout(5,10));
        JPanel panelLambda  = new JPanel(new GridBagLayout());
        JPanel panelButtons = new JPanel(new GridLayout(1,3,10,0));
        JPanel panelBounds  = new JPanel(new GridBagLayout());
        JPanel panelCenter  = new JPanel(new BorderLayout(5,10));
        
        panelLabels .setOpaque(false);
        panelFields .setOpaque(false);
        panelVars   .setOpaque(false);
        panelLambda .setOpaque(false);
        panelButtons.setOpaque(false);
        panelBounds .setOpaque(false);
        panelCenter .setOpaque(false);
        
        
        getContentPane().add(panelButtons,BorderLayout.SOUTH);
        
        panelVars.add(panelLabels,BorderLayout.WEST);
        panelVars.add(panelFields,BorderLayout.CENTER);
        
        panelCenter.add(panelLambda,BorderLayout.CENTER);
        panelCenter.add(panelBounds,BorderLayout.SOUTH);
        
        panelSurface.add(panelVars   ,BorderLayout.NORTH);
        panelSurface.add(panelCenter ,BorderLayout.WEST);
        panelSurface.add(new JPanel(),BorderLayout.SOUTH);
                
        panelLabels.add( new JLabel("start temperature :"));
        panelLabels.add( new JLabel("min. temperature :"));
        panelLabels.add( new JLabel("min. distance :"));
        panelLabels.add( new JLabel("temperature scaling factor :"));
        panelLabels.add( new JLabel("max. rounds :"));
        panelLabels.add( new JLabel("tries per cell :"));
        panelLabels.add( new JLabel("are uphill-moves allowed :"));
        panelLabels.add( new JLabel("compute permutations :"));
        
        
        tf_initTemperature        = new JTextField();
        tf_minTemperature         = new JTextField();
        tf_minDistance            = new JTextField();
        tf_tempScaleFactor        = new JTextField();
        tf_maxRounds              = new JTextField();
        tf_triesPerCell           = new JTextField();
        
        tf_lambdaNodeDistribution = new JTextField();
        tf_lambdaBorderline       = new JTextField();
        tf_lambdaEdgeLength       = new JTextField();
        tf_lambdaEdgeCrossing     = new JTextField();
        tf_lambdaEdgeDistribution = new JTextField();
        tf_lambdaNodeDistance     = new JTextField();
        
        cb_computePermutation  = new JCheckBox();
        cb_uphillMovesAllowed  = new JCheckBox();
        cb_useNodeDistribution = new JCheckBox();
        cb_useBorderline       = new JCheckBox();
        cb_useEdgeLength       = new JCheckBox();
        cb_useEdgeCrossing     = new JCheckBox();
        cb_useEdgeDistribution = new JCheckBox();
        cb_useNodeDistance     = new JCheckBox();
        
        cb_computePermutation .setOpaque(false);
        cb_uphillMovesAllowed .setOpaque(false);
        cb_useNodeDistribution.setOpaque(false);
        cb_useBorderline      .setOpaque(false);
        cb_useEdgeLength      .setOpaque(false);
        cb_useEdgeCrossing    .setOpaque(false);
        cb_useEdgeDistribution.setOpaque(false);
        cb_useNodeDistance    .setOpaque(false);
        
        registerCheckBoxAction(cb_useNodeDistribution);
        registerCheckBoxAction(cb_useBorderline);
        registerCheckBoxAction(cb_useEdgeLength);
        registerCheckBoxAction(cb_useEdgeCrossing);
        registerCheckBoxAction(cb_useEdgeDistribution);
        registerCheckBoxAction(cb_useNodeDistance);
    
        panelFields.add(tf_initTemperature);
        panelFields.add(tf_minTemperature);
        panelFields.add(tf_minDistance);
        panelFields.add(tf_tempScaleFactor);
        panelFields.add(tf_maxRounds);
        panelFields.add(tf_triesPerCell);
        
        panelFields.add(cb_computePermutation);
        panelFields.add(cb_uphillMovesAllowed);
        

        panelLambda.add(new JLabel("Costfunction Nodedistribution :"),
                                    new GridBagConstraints(0, 0, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(new JLabel("Costfunction Borderline :"),
                                    new GridBagConstraints(0, 1, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(new JLabel("Costfunction Edgelength :"),
                                    new GridBagConstraints(0, 2, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(new JLabel("Costfunction Edgecrossing :"),
                                    new GridBagConstraints(0, 3, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(new JLabel("Costfunction Edge Distribution :"),
                                    new GridBagConstraints(0, 4, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(new JLabel("Costfunction Node Overlapping :"),
                                    new GridBagConstraints(0, 5, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );
                                
        panelLambda.add(cb_useNodeDistribution,
                                    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLambda.add(cb_useBorderline,
                                    new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLambda.add(cb_useEdgeLength,
                                    new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLambda.add(cb_useEdgeCrossing,
                                    new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLambda.add(cb_useEdgeDistribution,
                                    new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );
        panelLambda.add(cb_useNodeDistance,
                                    new GridBagConstraints(1, 5, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );
        panelLambda.add(tf_lambdaNodeDistribution,
                                    new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(tf_lambdaBorderline,
                                    new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(tf_lambdaEdgeLength,
                                    new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(tf_lambdaEdgeCrossing,
                                    new GridBagConstraints(2, 3, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLambda.add(tf_lambdaEdgeDistribution,
                                    new GridBagConstraints(2, 4, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );
        panelLambda.add(tf_lambdaNodeDistance,
                                    new GridBagConstraints(2, 5, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );
                                                        
        JLabel[]  label_AdditionalCF = getAdditionalCostFunctionLabels();
        JTextField[] tf_AdditionalCF = getAdditionalCostFunctionTextFields();
        JCheckBox[]  cb_AdditionalCF = getAdditionalCostFunctionCheckBoxes();
        
        for( int i = 0; i < label_AdditionalCF.length; i++ )
            panelLambda.add(label_AdditionalCF[i],
                new GridBagConstraints(0, 
                                AnnealingLayoutController.COUT_COSTFUNCTION + i, 
                                1, 1, 0.5, 0.5, GridBagConstraints.WEST,
                                GridBagConstraints.NONE, new Insets(0,0,0,0), 
                                0,0) );
        
        for( int i = 0; i < cb_AdditionalCF.length; i++ )
            panelLambda.add(cb_AdditionalCF[i],
                new GridBagConstraints(1, 
                                AnnealingLayoutController.COUT_COSTFUNCTION + i, 
                                1, 1, 0.5, 0.5, GridBagConstraints.CENTER,
                                GridBagConstraints.NONE, new Insets(0,20,0,0), 
                                0,0) );
                                
        for( int i = 0; i < tf_AdditionalCF.length; i++ )
            panelLambda.add(tf_AdditionalCF[i],
                new GridBagConstraints(2, 
                                AnnealingLayoutController.COUT_COSTFUNCTION + i, 
                                1, 1, 0.5, 0.5, GridBagConstraints.EAST,
                                GridBagConstraints.HORIZONTAL, 
                                new Insets(0,0,0,0), 0,0) );
                                                        
                                                        
        button_takeViewportSize = new JButton("take viewport size");
        button_takeViewportSize.setOpaque(false);
        
        JLabel labelBoundsWidth  = new JLabel("max. width :");
        JLabel labelBoundsHeight = new JLabel("max. height :");
        
        tf_boundsWidth  = new JTextField();
        tf_boundsHeight = new JTextField();
                                                        
        panelBounds.add(button_takeViewportSize,
                                    new GridBagConstraints(0, 0, 1, 2, 0.5, 1.0, 
                                                  GridBagConstraints.SOUTHEAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(25,20,15,30), 
                                                  0,0) );
        
                                                        
        panelBounds.add(labelBoundsWidth,
                                    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.SOUTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(15,0,5,0),
                                                  0,0) );
                                                        
        panelBounds.add(labelBoundsHeight,                                                        
                                    new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.NORTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0),
                                                  0,0) );
                                                      
        panelBounds.add(tf_boundsWidth,
                                    new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.SOUTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(15,0,5,0),
                                                  0,0) );
                                                        
        panelBounds.add(tf_boundsHeight,
                                    new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.NORTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0),
                                                  0,0) );

        /*####################################################################*/
        
        
        panelLUSurface = new JPanel(new BorderLayout(5,10));
        //panelLUSurface.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Values for Layout updates"));
        panelLUSurface.setBackground(new Color(220,205,205));

        JPanel panelLULabels  = new JPanel(new GridLayout(8,1, 0,5));
        JPanel panelLUFields  = new JPanel(new GridLayout(8,1, 0,5));
        JPanel panelLUVars    = new JPanel(new BorderLayout(5,10));
        JPanel panelLULambda  = new JPanel(new GridBagLayout());
        JPanel panelLUBounds  = new JPanel(new GridBagLayout());
        JPanel panelLUCenter  = new JPanel(new BorderLayout(5,10));
        
        JPanel panelLUMethod  = new JPanel(new BorderLayout(5,10));
        
        panelLULabels.setOpaque(false);
        panelLUFields.setOpaque(false);
        panelLUVars  .setOpaque(false);
        panelLULambda.setOpaque(false);
        panelLUBounds.setOpaque(false);
        panelLUCenter.setOpaque(false);
        panelLUMethod.setOpaque(false);
        
        panelLUVars.add(panelLULabels,BorderLayout.WEST);
        panelLUVars.add(panelLUFields,BorderLayout.CENTER);
        
        panelLUCenter.add(panelLULambda,BorderLayout.WEST);
        panelLUCenter.add(panelLUBounds,BorderLayout.SOUTH);
        
        panelLUSurface.add(panelLUVars   ,BorderLayout.NORTH);
        panelLUSurface.add(panelLUCenter ,BorderLayout.CENTER);
        panelLUSurface.add(panelLUMethod ,BorderLayout.SOUTH);
                
        panelLULabels.add( new JLabel("start temperature :"));
        panelLULabels.add( new JLabel("min. temperature :"));
        panelLULabels.add( new JLabel("min. distance :"));
        panelLULabels.add( new JLabel("temperature scaling factor :"));
        panelLULabels.add( new JLabel("max. rounds :"));
        panelLULabels.add( new JLabel("tries per cell :"));
        panelLULabels.add( new JLabel("are uphill-moves allowed :"));
        panelLULabels.add( new JLabel("compute permutations :"));
        
        
        tf_lu_initTemperature        = new JTextField();
        tf_lu_minTemperature         = new JTextField();
        tf_lu_minDistance            = new JTextField();
        tf_lu_tempScaleFactor        = new JTextField();
        tf_lu_maxRounds              = new JTextField();
        tf_lu_triesPerCell           = new JTextField();
        
        tf_lu_lambdaNodeDistribution = new JTextField();
        tf_lu_lambdaBorderline       = new JTextField();
        tf_lu_lambdaEdgeLength       = new JTextField();
        tf_lu_lambdaEdgeCrossing     = new JTextField();
        tf_lu_lambdaEdgeDistribution = new JTextField();
        tf_lu_lambdaNodeDistance     = new JTextField();
        
        cb_lu_computePermutation  = new JCheckBox();
        cb_lu_uphillMovesAllowed  = new JCheckBox();
        cb_lu_useNodeDistribution = new JCheckBox();
        cb_lu_useBorderline       = new JCheckBox();
        cb_lu_useEdgeLength       = new JCheckBox();
        cb_lu_useEdgeCrossing     = new JCheckBox();
        cb_lu_useEdgeDistribution = new JCheckBox();
        cb_lu_useNodeDistance     = new JCheckBox();
        
        cb_lu_computePermutation .setOpaque(false);
        cb_lu_uphillMovesAllowed .setOpaque(false);
        cb_lu_useNodeDistribution.setOpaque(false);
        cb_lu_useBorderline      .setOpaque(false);
        cb_lu_useEdgeLength      .setOpaque(false);
        cb_lu_useEdgeCrossing    .setOpaque(false);
        cb_lu_useEdgeDistribution.setOpaque(false);
        cb_lu_useNodeDistance    .setOpaque(false);
        
        registerCheckBoxAction(cb_lu_useNodeDistribution);
        registerCheckBoxAction(cb_lu_useBorderline);
        registerCheckBoxAction(cb_lu_useEdgeLength);
        registerCheckBoxAction(cb_lu_useEdgeCrossing);
        registerCheckBoxAction(cb_lu_useEdgeDistribution);
        registerCheckBoxAction(cb_lu_useNodeDistance);
        
        
    
        panelLUFields.add(tf_lu_initTemperature);
        panelLUFields.add(tf_lu_minTemperature);
        panelLUFields.add(tf_lu_minDistance);
        panelLUFields.add(tf_lu_tempScaleFactor);
        panelLUFields.add(tf_lu_maxRounds);
        panelLUFields.add(tf_lu_triesPerCell);
        
        panelLUFields.add(cb_lu_computePermutation);
        panelLUFields.add(cb_lu_uphillMovesAllowed);
        

        panelLULambda.add(new JLabel("Costfunction Nodedistribution :"),
                                    new GridBagConstraints(0, 0, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(new JLabel("Costfunction Borderline :"),
                                    new GridBagConstraints(0, 1, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(new JLabel("Costfunction Edgelength :"),
                                    new GridBagConstraints(0, 2, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(new JLabel("Costfunction Edgecrossing :"),
                                    new GridBagConstraints(0, 3, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(new JLabel("Costfunction Edge Distribution :"),
                                    new GridBagConstraints(0, 4, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,-20), 
                                                  0,0) );

        panelLULambda.add(new JLabel("Costfunction Node Overlapping :"),
                                    new GridBagConstraints(0, 5, 1, 1, 0.5, 0.5, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,0,0,-20), 
                                                  0,0) );
        panelLULambda.add(cb_lu_useNodeDistribution,
                                    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLULambda.add(cb_lu_useBorderline,
                                    new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLULambda.add(cb_lu_useEdgeLength,
                                    new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLULambda.add(cb_lu_useEdgeCrossing,
                                    new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );

        panelLULambda.add(cb_lu_useEdgeDistribution,
                                    new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );
        panelLULambda.add(cb_lu_useNodeDistance,
                                    new GridBagConstraints(1, 5, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.CENTER,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,20,0,0), 
                                                  0,0) );
        panelLULambda.add(tf_lu_lambdaNodeDistribution,
                                    new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(tf_lu_lambdaBorderline,
                                    new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(tf_lu_lambdaEdgeLength,
                                    new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(tf_lu_lambdaEdgeCrossing,
                                    new GridBagConstraints(2, 3, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );

        panelLULambda.add(tf_lu_lambdaEdgeDistribution,
                                    new GridBagConstraints(2, 4, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );
        panelLULambda.add(tf_lu_lambdaNodeDistance,
                                    new GridBagConstraints(2, 5, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.EAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0), 
                                                  0,0) );
                                                        
        JLabel[]  label_lu_AdditionalCF = getAdditionalLUCostFunctionLabels();
        JTextField[] tf_lu_AdditionalCF = getAdditionalLUCostFunctionTextFields();
        JCheckBox[]  cb_lu_AdditionalCF = getAdditionalLUCostFunctionCheckBoxes();
        
        for( int i = 0; i < label_lu_AdditionalCF.length; i++ )
            panelLULambda.add(label_lu_AdditionalCF[i],
                new GridBagConstraints(0, 
                                AnnealingLayoutController.COUT_COSTFUNCTION + i, 
                                1, 1, 0.5, 0.5, GridBagConstraints.WEST,
                                GridBagConstraints.NONE, new Insets(0,0,0,0),
                                0,0) );
        
        for( int i = 0; i < cb_lu_AdditionalCF.length; i++ )
            panelLULambda.add(cb_lu_AdditionalCF[i],
                new GridBagConstraints(1, 
                                AnnealingLayoutController.COUT_COSTFUNCTION + i, 
                                1, 1, 0.5, 0.5, GridBagConstraints.CENTER,
                                GridBagConstraints.NONE, new Insets(0,20,0,0), 
                                0,0) );
                                
        for( int i = 0; i < tf_lu_AdditionalCF.length; i++ )
            panelLULambda.add(tf_lu_AdditionalCF[i],
                new GridBagConstraints(2, 
                                AnnealingLayoutController.COUT_COSTFUNCTION + i, 
                                1, 1, 0.5, 0.5, GridBagConstraints.EAST,
                                GridBagConstraints.HORIZONTAL, 
                                new Insets(0,0,0,0), 0,0) );
                                                        
                                                        
                                                        
        button_lu_takeViewportSize = new JButton("take viewport size");
        button_lu_takeViewportSize.setOpaque(false);
        
        JLabel labelLUBoundsWidth  = new JLabel("max. width :");
        JLabel labelLUBoundsHeight = new JLabel("max. height :");
        
        tf_lu_boundsWidth  = new JTextField();
        tf_lu_boundsHeight = new JTextField();
                                                        
        panelLUBounds.add(button_lu_takeViewportSize,
                                    new GridBagConstraints(0, 0, 1, 2, 0.5, 1.0, 
                                                  GridBagConstraints.SOUTHEAST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(25,20,15,30), 
                                                  0,0) );
        
                                                        
        panelLUBounds.add(labelLUBoundsWidth,
                                    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.SOUTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(15,0,5,0),
                                                  0,0) );
                                                        
        panelLUBounds.add(labelLUBoundsHeight,                                                        
                                    new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.NORTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0),
                                                  0,0) );
                                                      
        panelLUBounds.add(tf_lu_boundsWidth,
                                    new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.SOUTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(15,0,5,0),
                                                  0,0) );
                                                        
        panelLUBounds.add(tf_lu_boundsHeight,
                                    new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, 
                                                  GridBagConstraints.NORTH,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0),
                                                  0,0) );
                                                        
        tf_lu_method_neighborsDepth = new JTextField();
        tf_lu_method_perimeterRadius = new JTextField();
        tf_lu_method_perimeterRadiusInc = new JTextField();

        comb_lu_Method = new JComboBox();
        comb_lu_Method.addItem(
            AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY);
        comb_lu_Method.addItem(
            AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER);
        comb_lu_Method.setBackground(panelLUSurface.getBackground());
        comb_lu_Method.setOpaque(false);
        comb_lu_Method.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                action_CheckBoxSwitch();
            }
        });
        
        JPanel panelLUMethodLabels = new JPanel(new GridLayout(4,1,10,5));
        JPanel panelLUMethodFields = new JPanel(new GridLayout(4,1,10,5));
        
        panelLUMethodLabels.setOpaque(false);
        panelLUMethodFields.setOpaque(false);
        
        panelLUMethod.setBorder(
            BorderFactory.createTitledBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    "Layout Update Method"));
                    
        panelLUMethod.add(panelLUMethodLabels,BorderLayout.WEST);
        panelLUMethod.add(panelLUMethodFields,BorderLayout.EAST);
        
        panelLUMethodLabels.add(new JLabel("Method for Layout Updates :"));
        panelLUMethodLabels.add(new JLabel("Edge Distance to Neighbors :"));
        panelLUMethodLabels.add(new JLabel("Optical Distance to Neighbors :"));
        panelLUMethodLabels.add(new JLabel("Increase of optical Distance :"));
        
        panelLUMethodFields.add(comb_lu_Method);
        panelLUMethodFields.add(tf_lu_method_neighborsDepth);
        panelLUMethodFields.add(tf_lu_method_perimeterRadius);
        panelLUMethodFields.add(tf_lu_method_perimeterRadiusInc);
        
        JPanel panelLUMethodClustering = new JPanel(new GridLayout(3,2,10,5));
        JPanel panelLUMethodClusteringSwitch = new JPanel(new GridLayout(1,2,10,5));
        JPanel panelLUMethodClusteringEmpty = new JPanel();
        
        panelLUMethodClustering.setOpaque(false);
        panelLUMethodClusteringSwitch.setOpaque(false);
        panelLUMethodClusteringEmpty.setOpaque(false);
        
        tf_lu_clustering_factor    = new JTextField();
        cb_lu_clustering_enable    = new JCheckBox();
        tf_lu_clustering_moveScale = new JTextField();
        cb_lu_clustering_enable.setOpaque(false);
        registerCheckBoxAction(cb_lu_clustering_enable);
        
        panelLUMethodClusteringSwitch.add(new JLabel("on/off"));
        panelLUMethodClusteringSwitch.add(cb_lu_clustering_enable);
        
        panelLUMethodClustering.setBorder(
            BorderFactory.createTitledBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    "Clustering"));
        panelLUMethodClustering.add(panelLUMethodClusteringSwitch);
        panelLUMethodClustering.add(panelLUMethodClusteringEmpty);
        panelLUMethodClustering.add(new JLabel("Clustering factor :"));
        panelLUMethodClustering.add(tf_lu_clustering_factor);
        panelLUMethodClustering.add(new JLabel(
                                            "Cluster move scale factor :"));
        panelLUMethodClustering.add(tf_lu_clustering_moveScale);
        panelLUMethod.add(panelLUMethodClustering,BorderLayout.SOUTH);
        


        /*####################################################################*/

        cb_enableLayoutUpdate = new JCheckBox();
        if( !isOptimizer ){
            JPanel panelGlobal = new JPanel(new GridBagLayout());
            getContentPane().add(panelGlobal,BorderLayout.NORTH);
            
            cb_enableLayoutUpdate.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e ){
                    switchLayoutUpdatePanel();
                }
            });
            panelGlobal.add(new JLabel("Run permanent:"),
                           new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.NONE,
                                                  new Insets(0,5,0,30),
                                                  0,0) );
            panelGlobal.add(cb_enableLayoutUpdate,
                           new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 
                                                  GridBagConstraints.WEST,
                                                  GridBagConstraints.HORIZONTAL,
                                                  new Insets(0,0,0,0),
                                                  0,0) );
        }
        
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                closeDialog();
            }
        });
        
        JButton buttonFinished = new JButton("OK");
        JButton buttonCancel   = new JButton("Cancel");
        
        comb_loadPreSets = new JComboBox();
        
        for( int i = 0; i < preSetConfigs.length; i++ )
            comb_loadPreSets.addItem(preSetConfigs[i].get(
                                    AnnealingLayoutController.KEY_CONFIG_NAME));
        
        
        panelButtons.add(buttonFinished);
        panelButtons.add(comb_loadPreSets);
        panelButtons.add(buttonCancel);

        buttonFinished.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                action_ok();
            }
        });
        
        comb_loadPreSets.addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e){
                action_LoadPreSets(((JComboBox)e.getSource()).
                                                            getSelectedIndex());
            }
            
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                action_cancel();
            }
        });
        
        
		panelSurfaceWrapper = new JScrollPane(panelSurface);
		Dimension innerSize = new Dimension(550, 350);
		panelSurfaceWrapper .setPreferredSize(innerSize);
		panelLUSurfaceWrapper= new JScrollPane(panelLUSurface);
		panelLUSurfaceWrapper.setPreferredSize(innerSize);
        
		tp_main.addTab("Values for main run", panelSurfaceWrapper);
        
        getRootPane().setDefaultButton(buttonFinished);

        setToolTipTexts();
                
        pack();
    }
    
/******************************************************************************/    

    private void registerCheckBoxAction(JCheckBox cb){
        cb.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                action_CheckBoxSwitch();
            }
        });
    }

/******************************************************************************/    

    protected JLabel[] getAdditionalCostFunctionLabels(){
        return new JLabel[0];
    }

/******************************************************************************/    

    protected JCheckBox[] getAdditionalCostFunctionCheckBoxes(){
        return new JCheckBox[0];
    }

/******************************************************************************/    

    protected JTextField[] getAdditionalCostFunctionTextFields(){
        return new JTextField[0];
    }

/******************************************************************************/    

    protected JLabel[] getAdditionalLUCostFunctionLabels(){
        return new JLabel[0];
    }

/******************************************************************************/    

    protected JCheckBox[] getAdditionalLUCostFunctionCheckBoxes(){
        return new JCheckBox[0];
    }

/******************************************************************************/    

    protected JTextField[] getAdditionalLUCostFunctionTextFields(){
        return new JTextField[0];
    }

/******************************************************************************/    

    private void switchLayoutUpdatePanel(){
    	/*
        if( cb_enableLayoutUpdate.isSelected() && !isOptimizer){
        	
            getContentPane().remove(panelSurface);
            getContentPane().add(panelSurface,BorderLayout.WEST);
            getContentPane().add(panelLUSurface,BorderLayout.EAST);
        }
        else {
        	
            getContentPane().remove(panelSurface);
            getContentPane().remove(panelLUSurface);
            getContentPane().add(panelSurface,BorderLayout.CENTER);
            
        }
        */
        
		if( cb_enableLayoutUpdate.isSelected() && !isOptimizer){
			tp_main.addTab("Values for Layout updates", panelLUSurfaceWrapper);
		}
		else {
			tp_main.remove(panelLUSurfaceWrapper);
		}
        
        pack();
    }

/******************************************************************************/    
    /** Closes the dialog 
     * @see #action_cancel
     * */
    private void closeDialog() {
        action_cancel();
    }
            
/******************************************************************************/    

    protected void setToolTipTexts(){
        tf_initTemperature             .setToolTipText("starting value for temperature");
        tf_minTemperature              .setToolTipText("if temperature reaches this value, the algorithm finishes");
        tf_minDistance                 .setToolTipText("minimal distance, vertices should have to each other before causing more costs");
        tf_tempScaleFactor             .setToolTipText("factor the temperature is multiplied every round");
        tf_maxRounds                   .setToolTipText("maximum number of rounds, if not finished by temperature before");
        tf_triesPerCell                .setToolTipText("number of segments around a vertex. The vertex will be placed on each to find a new position. Costs are calculated for every segment, so don't choose your value too high");
        tf_lambdaNodeDistribution      .setToolTipText("Factor, the costs of the node distribution function are multiplied. Low distances between Vertices make heigher costs");
        tf_lambdaBorderline            .setToolTipText("Factor, the costs of the borderline function are multiplied. Border can be configured with \"max. width\" and \"max. height\" below. Vertices near or beyond cause higher costs");
        tf_lambdaEdgeLength            .setToolTipText("Factor, the costs of the edge length function are multiplied. Long edges cause higher costs");
        tf_lambdaEdgeCrossing          .setToolTipText("Factor, the costs of the edge crossing function are multiplied. For every crossing Edge the costs increase");
        tf_lambdaEdgeDistribution      .setToolTipText("Factor, the costs of the edge distribution function are multiplied. This function evaluates the distances between edges and vertices. A low distance increases the costs");
        tf_lambdaNodeDistance          .setToolTipText("Factor, the costs of the node distance function are multiplied. Nodes to close to each other or overlapping increases the costs");
        tf_boundsWidth                 .setToolTipText("width of the field the graph should be layouted on");
        tf_boundsHeight                .setToolTipText("height of the field the graph should be layouted on");
    
        cb_computePermutation          .setToolTipText("define if the vertices are computed every round with the same order or a random order (permutation)");
        cb_uphillMovesAllowed          .setToolTipText("defines if moves of vertices are allowed, which increases the costs slightly");
        cb_useNodeDistribution         .setToolTipText("switches the costfunction for node distribution on/off");
        cb_useBorderline               .setToolTipText("switches the costfunction for borderline on/off");
        cb_useEdgeLength               .setToolTipText("switches the costfunction for edge length on/off");
        cb_useEdgeCrossing             .setToolTipText("switches the costfunction for edge crossings on/off");
        cb_useEdgeDistribution         .setToolTipText("switches the costfunction for edge distribution on/off");
        cb_useNodeDistance             .setToolTipText("switches the costfunction for node distance on/off");
    
        button_takeViewportSize        .setToolTipText("takes the size of the viewport for the graph (FEATURE NOT IMPLEMENTED YET!!!)");
    
        cb_enableLayoutUpdate          .setToolTipText("enables the algorithm to run, when vertices are removed/added");
    
        tf_lu_initTemperature          .setToolTipText(tf_initTemperature.getToolTipText());
        tf_lu_minTemperature           .setToolTipText(tf_minTemperature.getToolTipText());
        tf_lu_minDistance              .setToolTipText(tf_minDistance.getToolTipText());
        tf_lu_tempScaleFactor          .setToolTipText(tf_tempScaleFactor.getToolTipText());
        tf_lu_maxRounds                .setToolTipText(tf_maxRounds.getToolTipText());
        tf_lu_triesPerCell             .setToolTipText(tf_triesPerCell.getToolTipText());
        tf_lu_lambdaNodeDistribution   .setToolTipText(tf_lambdaNodeDistribution.getToolTipText());
        tf_lu_lambdaBorderline         .setToolTipText(tf_lambdaBorderline.getToolTipText());
        tf_lu_lambdaEdgeLength         .setToolTipText(tf_lambdaEdgeLength.getToolTipText());
        tf_lu_lambdaEdgeCrossing       .setToolTipText(tf_lambdaEdgeCrossing.getToolTipText());
        tf_lu_lambdaEdgeDistribution   .setToolTipText(tf_lambdaEdgeDistribution.getToolTipText());
        tf_lu_lambdaNodeDistance       .setToolTipText(tf_lambdaNodeDistance.getToolTipText());
        tf_lu_boundsWidth              .setToolTipText(tf_boundsWidth.getToolTipText());
        tf_lu_boundsHeight             .setToolTipText(tf_boundsHeight.getToolTipText());
        tf_lu_method_neighborsDepth    .setToolTipText("");
        tf_lu_method_perimeterRadius   .setToolTipText("");
        tf_lu_method_perimeterRadiusInc.setToolTipText("");
    
        cb_lu_computePermutation       .setToolTipText(cb_computePermutation.getToolTipText());
        cb_lu_uphillMovesAllowed       .setToolTipText(cb_uphillMovesAllowed.getToolTipText());
        cb_lu_useNodeDistribution      .setToolTipText(cb_useNodeDistribution.getToolTipText());
        cb_lu_useBorderline            .setToolTipText(cb_useBorderline.getToolTipText());
        cb_lu_useEdgeLength            .setToolTipText(cb_useEdgeLength.getToolTipText());
        cb_lu_useEdgeCrossing          .setToolTipText(cb_useEdgeCrossing.getToolTipText());
        cb_lu_useEdgeDistribution      .setToolTipText(cb_useEdgeDistribution.getToolTipText());
        cb_lu_useNodeDistance          .setToolTipText(cb_useNodeDistance.getToolTipText());
    
        button_lu_takeViewportSize     .setToolTipText(button_takeViewportSize.getToolTipText());
        
        comb_lu_Method                 .setToolTipText("choose a method for Layout Updating. This method determines, which of the Neighbors around the inserted Vertices are also new layouted in order to make place for the Layout of the inserted Vertices.");
        tf_lu_method_neighborsDepth    .setToolTipText("Neighbors are once again layouted if their distance to the inserted vertices is lower or equal this value");
        tf_lu_method_perimeterRadius   .setToolTipText("Neighbors within this radius around the initial position of inserted vertices are once again layouted in order to make place for the Layout of the inserted Vertices");
        tf_lu_method_perimeterRadiusInc.setToolTipText("If more than one Vertice is inserted and has the same initial Position as another the perimeter radius is increase by this value");

        tf_lu_clustering_factor        .setToolTipText("Factor the creation of clusters. A lower value leads to more clusters, with smaller size, a higher value to fewer, bigger clusters");
        tf_lu_clustering_moveScale     .setToolTipText("Scales the movement of clusters. Possible values are between 0.0 and 1.0. A small value ensures, that clusters move slow and over short distances.");
    }

/******************************************************************************/    

    protected void action_CheckBoxSwitch(){
        tf_lambdaNodeDistribution.setEnabled(cb_useNodeDistribution.isSelected());
        tf_lambdaBorderline      .setEnabled(cb_useBorderline      .isSelected());
        button_takeViewportSize  .setEnabled(cb_useBorderline      .isSelected());
        tf_boundsWidth           .setEnabled(cb_useBorderline      .isSelected());
        tf_boundsHeight          .setEnabled(cb_useBorderline      .isSelected());
        tf_lambdaEdgeLength      .setEnabled(cb_useEdgeLength      .isSelected());
        tf_lambdaEdgeCrossing    .setEnabled(cb_useEdgeCrossing    .isSelected());
        tf_lambdaEdgeDistribution.setEnabled(cb_useEdgeDistribution.isSelected());
        tf_lambdaNodeDistance    .setEnabled(cb_useNodeDistance    .isSelected());
        
        tf_lu_lambdaNodeDistribution.setEnabled(cb_lu_useNodeDistribution.isSelected());
        tf_lu_lambdaBorderline      .setEnabled(cb_lu_useBorderline      .isSelected());
        button_lu_takeViewportSize  .setEnabled(cb_lu_useBorderline      .isSelected());
        tf_lu_boundsWidth           .setEnabled(cb_lu_useBorderline      .isSelected());
        tf_lu_boundsHeight          .setEnabled(cb_lu_useBorderline      .isSelected());
        tf_lu_lambdaEdgeLength      .setEnabled(cb_lu_useEdgeLength      .isSelected());
        tf_lu_lambdaEdgeCrossing    .setEnabled(cb_lu_useEdgeCrossing    .isSelected());
        tf_lu_lambdaEdgeDistribution.setEnabled(cb_lu_useEdgeDistribution.isSelected());
        tf_lu_lambdaNodeDistance    .setEnabled(cb_lu_useNodeDistance    .isSelected());
        
        tf_lu_clustering_factor.setEnabled(cb_lu_clustering_enable.isSelected());
        tf_lu_clustering_moveScale.setEnabled(cb_lu_clustering_enable.isSelected());
        
        String selectedLUMethod = (String) comb_lu_Method.getSelectedItem();
        if( AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY.
            equals(selectedLUMethod) ){
                tf_lu_method_neighborsDepth.setEnabled(true);
                tf_lu_method_perimeterRadius.setEnabled(false);
                tf_lu_method_perimeterRadiusInc.setEnabled(false);
        }
        else if( AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER.
            equals(selectedLUMethod) ){
            tf_lu_method_neighborsDepth.setEnabled(true);
            tf_lu_method_perimeterRadius.setEnabled(true);
            tf_lu_method_perimeterRadiusInc.setEnabled(true);
        }   
    }

/******************************************************************************/    

    protected boolean assertDouble(String text, String name, ArrayList errList){
        String errMsg = name+" is NOT a double value!";
        try {
            Double.parseDouble(text);
        }
        catch( NumberFormatException e ){
//            JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
            errList.add(errMsg);
            return false;
        }
        return true;
    }
        
/******************************************************************************/    

    protected boolean assertInteger(String text, String name, ArrayList errList){
        String errMsg = name+" is NOT a integer value!";
        try {
            Integer.parseInt(text);
        }
        catch( NumberFormatException e ){
//            JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
            errList.add(errMsg);
            return false;
        }
        return true;
    }

/******************************************************************************/    

    protected boolean assertRange(String value, double minValue, double maxValue, boolean includeMinVal, boolean includeMaxVal, String name, ArrayList errList){
        double val = Double.parseDouble(value);
        boolean isOK = true;
        if( includeMinVal ){
            isOK &= val >= minValue;
        }
        else {
            isOK &= val >  minValue;
        }
        if( isOK ){
            if( includeMaxVal ){
                isOK &= val <= maxValue;
            }
            else {
                isOK &= val <  maxValue;
            }
        }
        if( isOK ) {
            return true;
        }
        else {
            String errMsg = name+" is out the interval ";
            errMsg += includeMinVal ? "[" : "]";
            errMsg += minValue+";"+maxValue;
            errMsg += includeMaxVal ? "]" : "[";
//            JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
            errList.add(errMsg);
            return false;
        }
    }

/******************************************************************************/    

    protected boolean assertRange(String value, int minValue, int maxValue, boolean includeMinVal, boolean includeMaxVal, String name, ArrayList errList){
        double val = Integer.parseInt(value);
        boolean isOK = true;
        if( includeMinVal ){
            isOK &= val >= minValue;
        }
        else {
            isOK &= val >  minValue;
        }
        if( isOK ){
            if( includeMaxVal ){
                isOK &= val <= maxValue;
            }
            else {
                isOK &= val <  maxValue;
            }
        }
        if( isOK ) {
            return true;
        }
        else {
            String errMsg = name+" is out the interval ";
            errMsg += includeMinVal ? "[" : "]";
            errMsg += minValue+";"+maxValue;
            errMsg += includeMaxVal ? "]" : "[";
//            JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
            errList.add(errMsg);
            return false;
        }
    }

/******************************************************************************/    

    protected boolean assertDoublePositiveSign(String value, boolean includeZero, String name, ArrayList errList){
        double val = Double.parseDouble(value);
        boolean isOK = true;
        if( includeZero ){
            isOK = val >= 0.0;
        }
        else {
            isOK = val >  0.0;
        }
        if( !isOK ){
            String errMsg = name;
            if( includeZero ){
                errMsg += " has to be equal or bigger than 0.0";
            }
            else {
                errMsg += " has to be bigger than 0.0";
            }
//            JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
            errList.add(errMsg);
            return false;
        }
        return isOK;
    }

/******************************************************************************/    

    protected boolean assertIntegerPositiveSign(String value, boolean includeZero, String name, ArrayList errList){
        int val = Integer.parseInt(value);
        boolean isOK = true;
        if( includeZero ){
            isOK = val >= 0;
        }
        else {
            isOK = val >  0;
        }
        if( !isOK ){
            String errMsg = name;
            if( includeZero ){
                errMsg += " has to be equal or bigger than 0";
            }
            else {
                errMsg += " has to be bigger than 0";
            }
//            JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.INFORMATION_MESSAGE);
            errList.add(errList);
            return false;
        }
        return isOK;
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

    public double getMinDistance(){
        return Double.parseDouble(tf_minDistance.getText());
    }

/******************************************************************************/    

    public void setMinDistance(double distance){
        tf_minDistance.setText(String.valueOf(distance));
    }

/******************************************************************************/    

    public double getTemperatureScaleFactor(){
        return Double.parseDouble(tf_tempScaleFactor.getText());
    }
    
/******************************************************************************/    
    
    public void setTemperatureScaleFactor(double factor){
        tf_tempScaleFactor.setText(String.valueOf(factor));
    }
    
/******************************************************************************/    

    public int getMaxRounds(){
        return Integer.parseInt(tf_maxRounds.getText());
    }
    
/******************************************************************************/    
    
    public void setMaxRounds(int n){
        tf_maxRounds.setText(String.valueOf(n));
    }
    
/******************************************************************************/    
    
    public int getTriesPerCell(){
        return Integer.parseInt(tf_triesPerCell.getText());
    }

/******************************************************************************/    

    public void setTriesPerCell(int tries){
        tf_triesPerCell.setText(String.valueOf(tries));
    }
    
/******************************************************************************/    
    
    public int getCostFunctionConfiguration(){
        int config = 0;
        if( cb_useNodeDistance.isSelected() ){
            config |= 32;
        }
        if( cb_useNodeDistribution.isSelected() ){
            config |= 16;
        }
        if( cb_useBorderline.isSelected() ){
            config |=  8;
        }
        if( cb_useEdgeLength.isSelected() ){
            config |=  4;
        }
        if( cb_useEdgeCrossing.isSelected() ){
            config |=  2;
        }
        if( cb_useEdgeDistribution.isSelected() ){
            config |=  1;
        }
        config |= getAdditionalCostFunctionConfiguration();
        return config;
    }
    
/******************************************************************************/    

    protected int getAdditionalCostFunctionConfiguration(){
        return 0;
    }

/******************************************************************************/    
    public void setCostFunctionConfiguration(int config){
        cb_useNodeDistance    .setSelected((config & 32) != 0 );
        cb_useNodeDistribution.setSelected((config & 16) != 0 );        
        cb_useBorderline      .setSelected((config &  8) != 0 );
        cb_useEdgeLength      .setSelected((config &  4) != 0 );
        cb_useEdgeCrossing    .setSelected((config &  2) != 0 );
        cb_useEdgeDistribution.setSelected((config &  1) != 0 );
        setAdditionalCostFunctionConfiguration(config);
    }
    
/******************************************************************************/    
    
    protected void setAdditionalCostFunctionConfiguration(int config){
    }

/******************************************************************************/    
    
    public ArrayList getLambda(){
        ArrayList lambda = new ArrayList();
        lambda.add(new Double(tf_lambdaNodeDistribution.getText()));
        lambda.add(new Double(tf_lambdaBorderline.getText()));
        lambda.add(new Double(tf_lambdaEdgeLength.getText()));
        lambda.add(new Double(tf_lambdaEdgeCrossing.getText()));
        lambda.add(new Double(tf_lambdaEdgeDistribution.getText()));
        lambda.add(new Double(tf_lambdaNodeDistance.getText()));
        lambda = getAdditionalLambda(lambda);
        return lambda;
    }
    
/******************************************************************************/    
    
    protected ArrayList getAdditionalLambda(ArrayList lambda){
        return lambda;
    }
    
/******************************************************************************/    
    
    public void setLambda(ArrayList lambda){
        tf_lambdaNodeDistribution.setText(String.valueOf(((Double)lambda.get(0)).doubleValue()));
        tf_lambdaBorderline      .setText(String.valueOf(((Double)lambda.get(1)).doubleValue()));
        tf_lambdaEdgeLength      .setText(String.valueOf(((Double)lambda.get(2)).doubleValue()));
        tf_lambdaEdgeCrossing    .setText(String.valueOf(((Double)lambda.get(3)).doubleValue()));
        tf_lambdaEdgeDistribution.setText(String.valueOf(((Double)lambda.get(4)).doubleValue()));
        tf_lambdaNodeDistance    .setText(String.valueOf(((Double)lambda.get(5)).doubleValue()));
        setAdditionalLambda(lambda);
    }
    
/******************************************************************************/    

    protected void setAdditionalLambda(ArrayList lambda){
    }
    
/******************************************************************************/    
    
    public boolean getUphillMovesAllowed(){
        return cb_uphillMovesAllowed.isSelected();
    }
    
/******************************************************************************/    
    
    public void setUphillMovesAllowed(boolean allow){
        cb_uphillMovesAllowed.setSelected(allow);
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

    public Rectangle getResultBounds(){
        int x = 0;
        int y = 0;
        int w = Integer.parseInt(tf_boundsWidth.getText());
        int h = Integer.parseInt(tf_boundsHeight.getText());
        
        return new Rectangle(x,y,w,h);
    }
    
/******************************************************************************/    

    public void setResultBounds(Rectangle r){
        tf_boundsWidth.setText(String.valueOf((int)r.getWidth()));
        tf_boundsHeight.setText(String.valueOf((int)r.getHeight()));
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

    public double getLayoutUpdateMinDistance(){
        return Double.parseDouble(tf_lu_minDistance.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateMinDistance(double distance){
        tf_lu_minDistance.setText(String.valueOf(distance));
    }

/******************************************************************************/    

    public double getLayoutUpdateTemperatureScaleFactor(){
        return Double.parseDouble(tf_lu_tempScaleFactor.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateTemperatureScaleFactor(double factor){
        tf_lu_tempScaleFactor.setText(String.valueOf(factor));
    }
    
/******************************************************************************/    

    public int getLayoutUpdateMaxRounds(){
        return Integer.parseInt(tf_lu_maxRounds.getText());
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateMaxRounds(int n){
        tf_lu_maxRounds.setText(String.valueOf(n));
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateTriesPerCell(int tries){
        tf_lu_triesPerCell.setText(String.valueOf(tries));
    }

/******************************************************************************/    

    public int getLayoutUpdateTriesPerCell(){
        return Integer.parseInt(tf_lu_triesPerCell.getText());
    }
    
/******************************************************************************/    
    
    public int getLayoutUpdateCostFunctionConfiguration(){
        int config = 0;
        if( cb_lu_useNodeDistance.isSelected() ){
            config |= 32;
        }
        if( cb_lu_useNodeDistribution.isSelected() ){
            config |= 16;
        }
        if( cb_lu_useBorderline.isSelected() ){
            config |=  8;
        }
        if( cb_lu_useEdgeLength.isSelected() ){
            config |=  4;
        }
        if( cb_lu_useEdgeCrossing.isSelected() ){
            config |=  2;
        }
        if( cb_lu_useEdgeDistribution.isSelected() ){
            config |=  1;
        }
        config |= getLayoutUpdateAdditionalCostFunctionConfiguration();
        return config;
    }
    
/******************************************************************************/    
    
    protected int getLayoutUpdateAdditionalCostFunctionConfiguration(){
        return 0;
    }

/******************************************************************************/    
    
    public void setLayoutUpdateCostFunctionConfiguration(int config){
        cb_lu_useNodeDistance    .setSelected((config & 32) != 0 );
        cb_lu_useNodeDistribution.setSelected((config & 16) != 0 );        
        cb_lu_useBorderline      .setSelected((config &  8) != 0 );
        cb_lu_useEdgeLength      .setSelected((config &  4) != 0 );
        cb_lu_useEdgeCrossing    .setSelected((config &  2) != 0 );
        cb_lu_useEdgeDistribution.setSelected((config &  1) != 0 );
        setLayoutUpdateAdditionalCostFunctionConfiguration(config);
    }
    
/******************************************************************************/    
    
    protected void setLayoutUpdateAdditionalCostFunctionConfiguration(int config){
    }

/******************************************************************************/    
    
    public ArrayList getLayoutUpdateLambda(){
        ArrayList lambda = new ArrayList();
        lambda.add(new Double(tf_lu_lambdaNodeDistribution.getText()));
        lambda.add(new Double(tf_lu_lambdaBorderline.getText()));
        lambda.add(new Double(tf_lu_lambdaEdgeLength.getText()));
        lambda.add(new Double(tf_lu_lambdaEdgeCrossing.getText()));
        lambda.add(new Double(tf_lu_lambdaEdgeDistribution.getText()));
        lambda.add(new Double(tf_lu_lambdaNodeDistance.getText()));
        lambda = getLayoutUpdateAdditionalLambda(lambda);
        return lambda;
    }
    
/******************************************************************************/    
    
    protected ArrayList getLayoutUpdateAdditionalLambda(ArrayList lambda){
        return lambda;
    }

/******************************************************************************/    
    
    public void setLayoutUpdateLambda(ArrayList lambda){
        tf_lu_lambdaNodeDistribution.setText(String.valueOf(((Double)lambda.get(0)).doubleValue()));
        tf_lu_lambdaBorderline      .setText(String.valueOf(((Double)lambda.get(1)).doubleValue()));
        tf_lu_lambdaEdgeLength      .setText(String.valueOf(((Double)lambda.get(2)).doubleValue()));
        tf_lu_lambdaEdgeCrossing    .setText(String.valueOf(((Double)lambda.get(3)).doubleValue()));
        tf_lu_lambdaEdgeDistribution.setText(String.valueOf(((Double)lambda.get(4)).doubleValue()));
        tf_lu_lambdaNodeDistance    .setText(String.valueOf(((Double)lambda.get(5)).doubleValue()));
        setLayoutUpdateAdditionalLambda(lambda);
            
    }
    
/******************************************************************************/    
    
    protected void setLayoutUpdateAdditionalLambda(ArrayList lambda){
    }

/******************************************************************************/    
    
    public boolean getLayoutUpdateUphillMovesAllowed(){
        return cb_lu_uphillMovesAllowed.isSelected();
    }
    
/******************************************************************************/    
    
    public void setLayoutUpdateUphillMovesAllowed(boolean allow){
        cb_lu_uphillMovesAllowed.setSelected(allow);
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

    public Rectangle getLayoutUpdateResultBounds(){
        int x = 0;
        int y = 0;
        int w = Integer.parseInt(tf_lu_boundsWidth.getText());
        int h = Integer.parseInt(tf_lu_boundsHeight.getText());
        
        return new Rectangle(x,y,w,h);
    }
    
/******************************************************************************/    

    public void setLayoutUpdateResultBounds(Rectangle r){
        tf_lu_boundsWidth.setText(String.valueOf((int)r.getWidth()));
        tf_lu_boundsHeight.setText(String.valueOf((int)r.getHeight()));
    }

/******************************************************************************/    

    public void setLayoutUpdateEnabled(boolean enable){
        cb_enableLayoutUpdate.setSelected(enable);
        switchLayoutUpdatePanel();
    }

/******************************************************************************/    

    public boolean getLayoutUpdateEnabled(){
        return cb_enableLayoutUpdate.isSelected();
    }

/******************************************************************************/    

    public void setLayoutUpdateMethod(String method){
        comb_lu_Method.setSelectedItem(method);
    }

/******************************************************************************/    

    public String getLayoutUpdateMethod(){
        return (String) comb_lu_Method.getSelectedItem();
    }

/******************************************************************************/    

    public void setLayoutUpdateMethodNeighborsDepth(int depth){
        tf_lu_method_neighborsDepth.setText(String.valueOf(depth));
    }

/******************************************************************************/    

    public int getLayoutUpdateMethodNeighborsDepth(){
        return Integer.parseInt(tf_lu_method_neighborsDepth.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateMethodPerimeterRadius(double radius){
        tf_lu_method_perimeterRadius.setText(String.valueOf(radius));
    }

/******************************************************************************/    

    public double getLayoutUpdateMethodPerimeterRadius(){
        return Double.parseDouble(tf_lu_method_perimeterRadius.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateMethodPerimeterRadiusIncrease(double radiusInc){
        tf_lu_method_perimeterRadiusInc.setText(String.valueOf(radiusInc));
    }

/******************************************************************************/    

    public double getLayoutUpdateMethodPerimeterRadiusIncrease(){
        return Double.parseDouble(tf_lu_method_perimeterRadiusInc.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateClusteringEnabled(boolean enable){
        cb_lu_clustering_enable.setSelected(enable);
        
    }

/******************************************************************************/    

    public boolean getLayoutUpdateClusteringEnabled(){
        return cb_lu_clustering_enable.isSelected();
    }

/******************************************************************************/    

    public void setLayoutUpdateClusteringFactor(double factor){
        tf_lu_clustering_factor.setText(String.valueOf(factor));
    }

/******************************************************************************/    

    public double getLayoutUpdateClusteringFactor(){
        return Double.parseDouble(tf_lu_clustering_factor.getText());
    }

/******************************************************************************/    

    public void setLayoutUpdateClusteringMoveScaleFactor(double factor){
        tf_lu_clustering_moveScale.setText(String.valueOf(factor));
    }

/******************************************************************************/    

    public double getLayoutUpdateClusteringMoveScaleFactor(){
        return Double.parseDouble(tf_lu_clustering_moveScale.getText());
    }

/******************************************************************************/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/******************************************************************************/

    /**
     * For debugging purpose
     * @author winkler
     */
    public static void main(String[] args){
        Properties[] config = new Properties[1];
        
        config[0] = new Properties();
        config[0].put(AnnealingLayoutController.KEY_CONFIG_NAME           ,"Default Values");
        config[0].put(AnnealingLayoutController.KEY_INIT_TEMPERATURE      , "500.0");
        config[0].put(AnnealingLayoutController.KEY_MIN_TEMPERATURE       ,   "2.0");
        config[0].put(AnnealingLayoutController.KEY_MIN_DISTANCE          ,  "50.0");
        config[0].put(AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR     ,   "0.97");
        config[0].put(AnnealingLayoutController.KEY_COMPUTE_PERMUTATION   ,"true");
        config[0].put(AnnealingLayoutController.KEY_IS_UPHILL_MOVE_ALLOWED,"true");
        config[0].put(AnnealingLayoutController.KEY_MAX_ROUNDS            ,  "10000");
        config[0].put(AnnealingLayoutController.KEY_TRIES_PER_CELL        ,  "8");
        config[0].put(AnnealingLayoutController.KEY_COST_FUNCTION_CONFIG  ,"111110");
        config[0].put(AnnealingLayoutController.KEY_BOUNDS                ,   "0.0");
        
        ArrayList lambda1 = new ArrayList();
        lambda1.add(new Double(1000.0));
        lambda1.add(new Double(100000.0));
        lambda1.add(new Double(0.02));
        lambda1.add(new Double(2000.0));
        lambda1.add(new Double(150.0));
        lambda1.add(new Double(1000000.0));
        config[0].put(AnnealingLayoutController.KEY_LAMBDA                ,lambda1);
        
        Rectangle bounds1 = new Rectangle(0,0,1000,700);
        config[0].put(AnnealingLayoutController.KEY_BOUNDS                ,bounds1);


        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_ENABLED               ,"true");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      , "200.0");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,   "2.0");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_DISTANCE          ,  "50.0");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR     ,   "0.97");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,"true");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED,"true");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MAX_ROUNDS            ,  "10000");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_TRIES_PER_CELL        ,  "8");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG  ,"111111");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD                ,AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER);
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH,"1");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS,"200.0");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE,"40.0");
        
        ArrayList lambdaLU1 = new ArrayList();
        lambdaLU1.add(new Double(1000.0));
        lambdaLU1.add(new Double(100000.0));
        lambdaLU1.add(new Double(0.02));
        lambdaLU1.add(new Double(2000.0));
        lambdaLU1.add(new Double(150.0));
        lambdaLU1.add(new Double(1000000.0));
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_LAMBDA                ,lambdaLU1);
        
        Rectangle boundsLU1 = new Rectangle(0,0,1000,700);
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_BOUNDS                ,boundsLU1);
        
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED,"true");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,"8.0");
        config[0].put(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE,"0.2");
        
        AnnealingLayoutConfigurationDialog dialog = 
                      new AnnealingLayoutConfigurationDialog(null,config,false);
        dialog.validate();
        dialog.pack();
        dialog.setVisible(true);
    }


/******************************************************************************/    

}
