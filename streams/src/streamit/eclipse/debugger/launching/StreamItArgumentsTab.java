package streamit.eclipse.debugger.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.internal.debug.ui.IJavaDebugHelpContextIds;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.JavaDebugImages;
import org.eclipse.jdt.internal.debug.ui.launcher.JavaLaunchConfigurationTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock;
import org.eclipse.jdt.internal.debug.ui.launcher.WorkingDirectoryBlock;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * A launch configuration tab that displays and edits program arguments,
 * VM arguments, and working directory launch configuration attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed.
 * </p>
 * @author kkuo
 */
public class StreamItArgumentsTab extends JavaLaunchConfigurationTab {

	// CHANGED kkuo
	// Secondary classes widgets
	protected Label fSecondaryClassesLabel;
	protected Text fSecondaryClassesText;

	// Program arguments widgets
	protected Label fPrgmArgumentsLabel;
	protected Text fPrgmArgumentsText;

	// VM arguments widgets
	protected VMArgumentsBlock fVMArgumentsBlock;
	
	// Working directory
	protected WorkingDirectoryBlock fWorkingDirectoryBlock;
		
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
		
	public StreamItArgumentsTab() {		
		fVMArgumentsBlock = createVMArgsBlock();
		fWorkingDirectoryBlock = createWorkingDirBlock();
	}
	
	protected VMArgumentsBlock createVMArgsBlock() {
		return new VMArgumentsBlock();
	}
	
	protected WorkingDirectoryBlock createWorkingDirBlock() {
		return new WorkingDirectoryBlock();
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setFont(font);
		setControl(comp);
		setHelpContextId();
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		
		GridData gd;
		
		createVerticalSpacer(comp, 1);
		
		// CHANGED kkuo		
		fSecondaryClassesLabel = new Label(comp, SWT.NONE);
		fSecondaryClassesLabel.setText(LaunchingMessages.getString("StreamItArgumentsTab.&Secondary_classes__5")); //$NON-NLS-1$
		fSecondaryClassesLabel.setFont(font);
		
		fSecondaryClassesText = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 40;
		gd.widthHint = 100;
		fSecondaryClassesText.setLayoutData(gd);
		fSecondaryClassesText.setFont(font);
		fSecondaryClassesText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		// END CHANGED kkuo		
					
		fPrgmArgumentsLabel = new Label(comp, SWT.NONE);
		fPrgmArgumentsLabel.setText(LauncherMessages.getString("JavaArgumentsTab.&Program_arguments__5")); //$NON-NLS-1$
		fPrgmArgumentsLabel.setFont(font);
		
		fPrgmArgumentsText = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 40;
		gd.widthHint = 100;
		fPrgmArgumentsText.setLayoutData(gd);
		fPrgmArgumentsText.setFont(font);
		fPrgmArgumentsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fVMArgumentsBlock.createControl(comp);
				
		createVerticalSpacer(comp, 1);
						
		fWorkingDirectoryBlock.createControl(comp);		
	}
	
	/**
	 * Set the help context id for this launch config tab.  Subclasses may
	 * override this method.
	 */
	protected void setHelpContextId() {
		WorkbenchHelp.setHelp(getControl(), IJavaDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_ARGUMENTS_TAB);		
	}
			
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		return fWorkingDirectoryBlock.isValid(config);
	}

	/**
	 * Defaults are empty.
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		// CHANGED kkuo
		config.setAttribute(IStreamItLaunchingConstants.ATTR_SECONDARY_CLASSES, (String)null);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "-i 10");
		fVMArgumentsBlock.setDefaults(config);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String)null);
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			// CHANGED kkuo
			fSecondaryClassesText.setText(configuration.getAttribute(IStreamItLaunchingConstants.ATTR_SECONDARY_CLASSES, EMPTY_STRING));
			fPrgmArgumentsText.setText(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "-i 10")); //$NON-NLS-1$
		
			fVMArgumentsBlock.initializeFrom(configuration);
			fWorkingDirectoryBlock.initializeFrom(configuration);
		} catch (CoreException e) {
			setErrorMessage(LauncherMessages.getString("JavaArgumentsTab.Exception_occurred_reading_configuration___15") + e.getStatus().getMessage()); //$NON-NLS-1$
			JDIDebugUIPlugin.log(e);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// CHANGED kkuo
		configuration.setAttribute(IStreamItLaunchingConstants.ATTR_SECONDARY_CLASSES, getAttributeValueFrom(fSecondaryClassesText));
		
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getAttributeValueFrom(fPrgmArgumentsText));
		fVMArgumentsBlock.performApply(configuration);
		fWorkingDirectoryBlock.performApply(configuration);
	}

	/**
	 * Retuns the string in the text widget, or <code>null</code> if empty.
	 * 
	 * @return text or <code>null</code>
	 */
	protected String getAttributeValueFrom(Text text) {
		String content = text.getText().trim();
		if (content.length() > 0) {
			return content;
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LauncherMessages.getString("JavaArgumentsTab.&Arguments_16"); //$NON-NLS-1$
	}	
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setLaunchConfigurationDialog(ILaunchConfigurationDialog)
	 */
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		super.setLaunchConfigurationDialog(dialog);
		fWorkingDirectoryBlock.setLaunchConfigurationDialog(dialog);
		fVMArgumentsBlock.setLaunchConfigurationDialog(dialog);
	}	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getErrorMessage()
	 */
	public String getErrorMessage() {
		String m = super.getErrorMessage();
		if (m == null) {
			return fWorkingDirectoryBlock.getErrorMessage();
		}
		return m;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getMessage()
	 */
	public String getMessage() {
		String m = super.getMessage();
		if (m == null) {
			return fWorkingDirectoryBlock.getMessage();
		}
		return m;
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return JavaDebugImages.get(JavaDebugImages.IMG_VIEW_ARGUMENTS_TAB);
	}
}