
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * This dialog is shown when a new GEStreamNode is created
 * so that the user can choose the type and the properties that
 * he wants to change. 
 * @author jcarlos
 */
public class StreamTypeDialog extends JDialog 
{
    /** 
     * True if canceled
     */
    private boolean canceled;

	private JPanel pnlApplyTo;
	private JButton cmdConfigure;
	private JPanel pnlStreamNodeControllers;
	private JScrollPane scrollStreamNodeControllers;
	private ButtonGroup cmdGrpApplyTo;
	private JList streamNodeControllers;
	private JPanel pnlButtons;
	private JButton cmdCancel;
	private JPanel pnlMain;
	private JLabel lblApplyTo;
	private JButton cmdFinished;
        
	private String title = "Stream Node Type";
	
	private Properties configuration;
	
	private GEStreamNodeController _controller;
	
	
	/**
	 * The current GPDocument that corresponds to the current StreamTypeDialog 
	 * that was create. 
	 */
	private GPDocument document;

    /**
     *  Constructor. Creates new form StreamTypeDialog
     * @param parent The dialog that is the parent of the StreamTypeDialog.
     */
    public StreamTypeDialog(Dialog parent) 
    {
        super(parent, true);
        init();
    }

	/**
	 * Constructor. Creates new form StreamTypeDialog
	 * @param parent The frame that is the parent of the StreamTypeDialog.
	 */    
    public StreamTypeDialog(Frame parent) 
    {
		super(parent, true);
        init();
    }

	/**
	 * Constructor.
	 * @param parent The Frame that is the parent of the StreamTypeDialog.
	 * @param document The current GPDocument that corresponds to the <this>.
	 */
	public StreamTypeDialog(Frame parent, GPDocument document)
	{
		super(parent, true);
		this.document = document;
		configuration = null;
		_controller = null;
		init();
	}
	
    /** 
     * Initializes the dialog
     */
    protected void init() {
            // netbeans
            initComponents();

            // fill the list
            fillList();

            // select the first one
            try {
				streamNodeControllers.setSelectedIndex(0);
            } catch (Exception e) {
            }
    }

    /** Fills the List with the GEStreamNodeControllers
     *  from the GEStreamNodeControllerRegistry.
     *  
     *  @see GEStreamNodeControllerRegistry
     */
    protected void fillList() 
    {
        try 
        {
        	
    		DefaultListModel model = new DefaultListModel();                 
			Iterator all = GEStreamNodeControllerRegistry.registeredControllers();
          	
          	/*
			String [] streamTypes = GEType.ALL_STREAM_TYPES;
			for (int i=0; i < streamTypes.length; i++)
			{
				model.addElement((String)streamTypes[i]);
			}
			*/

            while (all.hasNext()) 
            {
                    GEStreamNodeController controller = (GEStreamNodeController) all.next();
                    model.addElement(controller);
            }

                
			streamNodeControllers.setModel(model);
        } catch (Exception e) {
                e.printStackTrace();
        }
    }

    /** 
     * Called if the user cancels the dialog
     */
    protected void cancel() {
            canceled = true;
            setVisible(false);
            dispose();
    }

    /** 
     * Called if the user finishes the dialog
     */
    protected void finish() {
            canceled = false;
            setVisible(false);
            dispose();
    }

    /**
     * Returns true if the dialog has been canceled.
     */
    public boolean isCanceled() {
            return canceled;
    }

    /** 
     *  Will call if the user clicks on the configuration button.
     *  if the GEStreamNode controller is configurable the method
     *  calls the configure method at the controller.
     *
     */
    protected void configure() 
    {
		try 
		{
			_controller = (GEStreamNodeController) streamNodeControllers.getSelectedValue();
            if (!_controller.isConfigurable()) 
            {
            	String message = Translator.getString("Error.ThisLayoutCannotBeConfigured"); 
                JOptionPane.showMessageDialog(this, message, null, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            _controller.configure(this.document);
            configuration = _controller.getConfiguration();
        } catch (Exception e) 
        {
                e.printStackTrace();
        }
	}

    /** 
     *  Returns the selected  GEStreamNodeController or null if
     *  no GEStreamNodeController was selected.
     *
     */
    public GEStreamNodeController getSelectedLayoutController() {
        try 
        {
        	return (GEStreamNodeController) streamNodeControllers.getSelectedValue();
        } 
        catch (Exception e) 
        {
        	return null;
        }
    }
   
   /**
    * Returns the properties of the node that was just added (either the ones
    * modified by the user or the deafault configuration values for a new node). 
    *
    */
   public Properties getConfiguration()
   {
   		return this.configuration;
   }
   

    /** 
     * Initializes the GUI
     */
    protected void initComponents() 
    { 
        cmdGrpApplyTo = new javax.swing.ButtonGroup();
        
        pnlMain = new JPanel();
        pnlApplyTo = new JPanel();
        lblApplyTo = new JLabel();

        pnlButtons = new JPanel();
        cmdConfigure = new JButton();
        cmdCancel = new JButton();
        cmdFinished = new JButton();
        
		pnlStreamNodeControllers = new JPanel();
		scrollStreamNodeControllers = new JScrollPane();
		streamNodeControllers = new JList();

        setTitle(this.title);       
        setName(this.title);      
        addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent evt) {
                            closeDialog(evt);
                    }
            });

            pnlMain.setLayout(new java.awt.BorderLayout());

            lblApplyTo.setText(Translator.getString("Apply to"));   //#Changed
            lblApplyTo.setName("ApplyTo");  //#Frozen
            pnlApplyTo.add(lblApplyTo);




            pnlMain.add(pnlApplyTo, java.awt.BorderLayout.NORTH);

            cmdConfigure.setText(Translator.getString("Configure"));        //#Changed
            cmdConfigure.setName("Configure");      //#Frozen
            cmdConfigure.setFocusPainted(false);
            cmdConfigure.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                            configureButtonActionPerformed(evt);
                    }
            });

            pnlButtons.add(cmdConfigure);

            cmdFinished.setText(Translator.getString("OK"));        //#Changed
            cmdFinished.setName("OK");      //#Frozen
            cmdFinished.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                            finishedButtonActionPerformed(evt);
                    }
            });

            pnlButtons.add(cmdFinished);
            getRootPane().setDefaultButton(cmdFinished);

            cmdCancel.setText(Translator.getString("Cancel"));      //#Changed
            cmdCancel.setName("Cancel");    //#Frozen
            cmdCancel.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                            cancelButtonActionPerformed(evt);
                    }
            });
            pnlButtons.add(cmdCancel);

            pnlMain.add(pnlButtons, java.awt.BorderLayout.SOUTH);
            getContentPane().add(pnlMain, java.awt.BorderLayout.SOUTH);
			pnlStreamNodeControllers.setLayout(new java.awt.BorderLayout());
			scrollStreamNodeControllers.setViewportView(streamNodeControllers);
			pnlStreamNodeControllers.add(scrollStreamNodeControllers, java.awt.BorderLayout.CENTER);
            getContentPane().add(pnlStreamNodeControllers, java.awt.BorderLayout.CENTER);

            pack();
            java.awt.Dimension screenSize =
                    java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            //setSize(new java.awt.Dimension(302, 312));
            pack();
            setLocation(
                    (screenSize.width - this.getWidth()) / 2,
                    (screenSize.height - this.getHeight()) / 2);
    } 

    
    /** calls the configure method
     *  @see #configure
     */
    protected void configureButtonActionPerformed(java.awt.event.ActionEvent evt) 
    { 
            configure();
    } 
    
    /** calls the finish method
     *  @see #finish
     */
    protected void finishedButtonActionPerformed(ActionEvent evt) 
    {
    	if (configuration == null)
    	{
			_controller = (GEStreamNodeController) streamNodeControllers.getSelectedValue();
    	 	configuration = _controller.getDefaultConfiguration();
    	}
        finish();
    } 
    
    /** calls the cancel method
     *  @see #cancel
     */
    protected void cancelButtonActionPerformed(ActionEvent evt) 
    {  
            cancel();
    } 
    
    /** calls the cancel method
     *  @see #cancel
     */
    protected void closeDialog (WindowEvent evt) 
    { 
            cancel();
    } 

    /**
     * TEST
     */
    public static void main(String args[]) {
            new StreamTypeDialog(new javax.swing.JFrame()).setVisible(true);
            System.exit(0);
    }
}