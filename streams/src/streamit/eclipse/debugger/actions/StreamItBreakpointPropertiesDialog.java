package streamit.eclipse.debugger.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.JavaBreakpointPreferencePage;
import org.eclipse.jdt.internal.debug.ui.actions.JavaBreakpointPreferenceStore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

/**
 * @author kkuo
 */
public class StreamItBreakpointPropertiesDialog	extends Dialog implements IPreferencePageContainer {
	//extends JavaBreakpointPropertiesDialog {
	/**
	 * Layout for the page container.
	 *
	 * @see JavaBreakpointPropertiesDialog#createPageContainer(Composite, int)
	 */
	private class PageLayout extends Layout {
		public void layout(Composite composite, boolean force) {
			Rectangle rect = composite.getClientArea();
			Control [] children = composite.getChildren();
			for (int i= 0; i < children.length; i++) {
				children[i].setSize(rect.width, rect.height);
			}
		}
		public Point computeSize(Composite composite, int wHint, int hHint, boolean force) {
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
				return new Point(wHint, hHint);
			int x= fMinimumPageSize.x;
			int y= fMinimumPageSize.y;
			
			Control[] children= composite.getChildren();
			for (int i= 0; i < children.length; i++) {
				Point size= children[i].computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
				x= Math.max(x, size.x);
				y= Math.max(y, size.y);
			}				
			if (wHint != SWT.DEFAULT) x = wHint;
			if (hHint != SWT.DEFAULT) y = hHint;
			return new Point(x, y);
		}	
	}

	private Composite fTitleArea;
	private Label fTitleImage;
	private CLabel fMessageLabel;
	
	private String fMessage;
	private Color fNormalMsgAreaBackground;
	private Image fErrorMsgImage;

	
	private JavaBreakpointPreferencePage fPage;
	
	private Button fOkButton;
	
	/**
	 * Must declare our own images as the JFaceResource images will not be created unless
	 * a property/preference dialog has been shown
	 */
	protected static final String PREF_DLG_TITLE_IMG = "breakpoint_preference_dialog_title_image";//$NON-NLS-1$
	protected static final String PREF_DLG_IMG_TITLE_ERROR = "breakpoint_preference_dialog_title_error_image";//$NON-NLS-1$
	static {
		ImageRegistry reg = JDIDebugUIPlugin.getDefault().getImageRegistry();
		reg.put(PREF_DLG_TITLE_IMG, ImageDescriptor.createFromFile(PreferenceDialog.class, "images/pref_dialog_title.gif"));//$NON-NLS-1$
		reg.put(PREF_DLG_IMG_TITLE_ERROR, ImageDescriptor.createFromFile(Dialog.class, "images/message_error.gif"));//$NON-NLS-1$
	}
	
	/**
	 * The Composite in which a page is shown.
	 */
	private Composite fPageContainer;

	/**
	 * The minimum page size; 200 by 200 by default.
	 *
	 * @see #setMinimumPageSize(Point)
	 */
	private Point fMinimumPageSize = new Point(200,200);
	
	/**
	 * The breakpoint that this dialog is operating on
	 */
	private IJavaBreakpoint fBreakpoint;
		
	/**
	 * The "fake" preference store used to interface between
	 * the breakpoint and the breakpoint preference page.
	 */
	private JavaBreakpointPreferenceStore fJavaBreakpointPreferenceStore;
	
	public StreamItBreakpointPropertiesDialog(Shell parentShell, IJavaBreakpoint breakpoint) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		setBreakpoint(breakpoint);
		//fJavaBreakpointPreferenceStore= new JavaBreakpointPreferenceStore();
	}

	/**
	 * @see Dialog#okPressed()
	 */
	protected void okPressed() {
		final List changedProperties= new ArrayList(5);
		getPreferenceStore().addPropertyChangeListener( new IPropertyChangeListener() {
			/**
			 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent event) {
				changedProperties.add(event.getProperty());
			}
		});
		fPage.performOk();
		setBreakpointProperties(changedProperties);
		super.okPressed();
	}
	
	/**
	 * All of the properties that the user has changed via the dialog
	 * are written through to the breakpoint.
	 */
	protected void setBreakpointProperties(final List changedProperties) {
		IWorkspaceRunnable wr= new IWorkspaceRunnable() {
			
		public void run(IProgressMonitor monitor) throws CoreException {
				
				/*
		boolean newEnabled= false;
		IJavaBreakpoint breakpoint= getBreakpoint();
		Iterator changed= changedProperties.iterator();
		while (changed.hasNext()) {
			String property = (String) changed.next();
			switch (property.charAt(0)) {
				case 'A': //access
					IJavaWatchpoint jWatchpoint= (IJavaWatchpoint)breakpoint;
					jWatchpoint.setAccess(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.ACCESS));
					break;
				case 'C': //caught or condition
					switch (property.charAt(property.length() - 1)) {
						case 'T': // caught
							IJavaExceptionBreakpoint jeBreakpoint= (IJavaExceptionBreakpoint)breakpoint;
							jeBreakpoint.setCaught(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.CAUGHT));
							break;
						case 'N': // condition
							IJavaLineBreakpoint lineBreakpoint= (IJavaLineBreakpoint)breakpoint;
							lineBreakpoint.setCondition(getPreferenceStore().getString(JavaBreakpointPreferenceStore.CONDITION));
							break;
						case 'D': // condition_enabled
							lineBreakpoint= (IJavaLineBreakpoint)breakpoint;
							lineBreakpoint.setConditionEnabled(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.CONDITION_ENABLED));
							break;
						case 'E': // condition suspend policy
							lineBreakpoint= (IJavaLineBreakpoint)breakpoint;
							lineBreakpoint.setConditionSuspendOnTrue(getPreferenceStore().getString(JavaBreakpointPreferenceStore.CONDITION_SUSPEND_ON_TRUE) == JavaBreakpointPreferenceStore.CONDITION_SUSPEND_ON_TRUE);
							break;
					}
					break;
				case 'E'://enabled
					newEnabled= true;
					break;
				case 'H':
					if (property.charAt(property.length() - 1) == 'T') {
						//hitcount
						breakpoint.setHitCount(getPreferenceStore().getInt(JavaBreakpointPreferenceStore.HIT_COUNT));
					} else {
						if (getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.HIT_COUNT_ENABLED)) {
							 if (!changedProperties.contains(JavaBreakpointPreferenceStore.HIT_COUNT)) {
								//enabled the hit count but did not change the hit count value
								breakpoint.setHitCount(getPreferenceStore().getInt(JavaBreakpointPreferenceStore.HIT_COUNT));
							}
						} else {
							//disable the hitCount
							breakpoint.setHitCount(0);	
						}
					}
					break;
				case 'M':
					char lastChar= property.charAt(property.length() - 1);
					if (lastChar == 'T') {
						//exit
						IJavaMethodBreakpoint jmBreakpoint= (IJavaMethodBreakpoint)breakpoint;
						jmBreakpoint.setExit(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.METHOD_EXIT));
					} else if (lastChar == 'Y') {
						//entry
						IJavaMethodBreakpoint jmBreakpoint= (IJavaMethodBreakpoint)breakpoint;
						jmBreakpoint.setEntry(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.METHOD_ENTRY));
					} else {
						jWatchpoint= (IJavaWatchpoint)breakpoint;
						jWatchpoint.setModification(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.MODIFICATION));
					}
					break;
				case 'S':
					String value= getPreferenceStore().getString(JavaBreakpointPreferenceStore.SUSPEND_POLICY);
					if (value.equals(JavaBreakpointPreferencePage.VM_SUSPEND_POLICY)) {
						breakpoint.setSuspendPolicy(IJavaBreakpoint.SUSPEND_VM);
					} else {
						breakpoint.setSuspendPolicy(IJavaBreakpoint.SUSPEND_THREAD);
					}
					break;
				case 'U':
					IJavaExceptionBreakpoint jeBreakpoint= (IJavaExceptionBreakpoint)breakpoint;
					jeBreakpoint.setUncaught(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.UNCAUGHT));
					break;
			}
			
		}
		if (newEnabled) {
			//some of the other attributes auto enable the breakpoint
			//ensure that the breakpoint is set as the user specified
			breakpoint.setEnabled(getPreferenceStore().getBoolean(JavaBreakpointPreferenceStore.ENABLED));
		}*/
		}};
		
		try {
			ResourcesPlugin.getWorkspace().run(wr, null);
		} catch (CoreException ce) {
			JDIDebugUIPlugin.log(ce);
		}	
		
	}
	/**
	 * Sets the title for this dialog.
	 * @param title the title.
	 */
	public void setTitle(String title) {
		Shell shell= getShell();
		if ((shell != null) && !shell.isDisposed()) {
			shell.setText(title);
		}
	}
	
	/**
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Font font = parent.getFont();
		
		Composite composite = (Composite)super.createDialogArea(parent);
		((GridLayout) composite.getLayout()).numColumns = 1;
		composite.setFont(font);
		
		// Build the title area and separator line
		Composite titleComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		titleComposite.setLayout(layout);
		titleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		titleComposite.setFont(font);
		
		createTitleArea(titleComposite);
	
		Label titleBarSeparator = new Label(titleComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		titleBarSeparator.setLayoutData(gd);
	
		// Build the Page container
		fPageContainer = createPageContainer(composite);
		fPageContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		//fPage= new JavaBreakpointPreferencePage(getBreakpoint());
		//fPage.setContainer(this);
		//fPage.createControl(fPageContainer);
			
		// Build the separator line
		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		separator.setLayoutData(gd);
	
		applyDialogFont(composite);
		return composite;
	}
	
	/**
	 * Creates the dialog's title area.
	 *
	 * @param parent the SWT parent for the title area composite
	 * @return the created title area composite
	 */
	private Composite createTitleArea(Composite parent) {
		Font font = parent.getFont();
		
		// Create the title area which will contain
		// a title, message, and image.
		fTitleArea = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.numColumns = 2;
		
		// Get the colors for the title area
		Display display = parent.getDisplay();
		Color bg = JFaceColors.getBannerBackground(display);
		Color fg = JFaceColors.getBannerForeground(display);
		
		GridData layoutData = new GridData(GridData.FILL_BOTH);
		fTitleArea.setLayout(layout);
		fTitleArea.setLayoutData(layoutData);
		fTitleArea.setFont(font);
		fTitleArea.setBackground(bg);
	
		// Message label
		fMessageLabel = new CLabel(fTitleArea, SWT.LEFT);
		fMessageLabel.setBackground(bg);
		fMessageLabel.setForeground(fg);
		fMessageLabel.setText(" ");//$NON-NLS-1$
		fMessageLabel.setFont(JFaceResources.getBannerFont());
		
		final IPropertyChangeListener fontListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(JFaceResources.BANNER_FONT.equals(event.getProperty()) ||
					JFaceResources.DIALOG_FONT.equals(event.getProperty())) {
					updateMessage();
				}
			}
		};
		
		fMessageLabel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				JFaceResources.getFontRegistry().removeListener(fontListener);
			}
		});
		
		JFaceResources.getFontRegistry().addListener(fontListener);
				
		GridData gd = new GridData(GridData.FILL_BOTH);
		fMessageLabel.setLayoutData(gd);
	
		// Title image
		fTitleImage = new Label(fTitleArea, SWT.LEFT);
		fTitleImage.setBackground(bg);
		fTitleImage.setImage(JDIDebugUIPlugin.getDefault().getImageRegistry().get(PREF_DLG_TITLE_IMG));
		gd = new GridData(); 
		gd.horizontalAlignment = GridData.END;
		fTitleImage.setLayoutData(gd);
	
		return fTitleArea;
	}

	/**
	 * Creates the inner page container.
	 */
	private Composite createPageContainer(Composite parent) {
		Composite result = new Composite(parent, SWT.NULL);
		result.setLayout(new PageLayout());
		result.setFont(parent.getFont());
		return result;
	}

	/**
	 * Sets the minimum page size.
	 *
	 * @param size the page size encoded as
	 *   <code>new Point(width,height)</code>
	 * @see #setMinimumPageSize(int,int)
	 */
	public void setMinimumPageSize(Point size) {
		fMinimumPageSize.x = size.x;
		fMinimumPageSize.y = size.y;
	}
	
	/**
	 * Display the given error message. The currently displayed message
	 * is saved and will be redisplayed when the error message is set
	 * to <code>null</code>.
	 *
	 * @param errorMessage the errorMessage to display or <code>null</code>
	 */
	public void setErrorMessage(String errorMessage) {
		if (errorMessage == null) {
			if (fMessageLabel.getImage() != null) {
				// we were previously showing an error
				fMessageLabel.setBackground(fNormalMsgAreaBackground);
				fMessageLabel.setImage(null);
				fTitleImage.setImage(JDIDebugUIPlugin.getDefault().getImageRegistry().get(PREF_DLG_TITLE_IMG));
				fTitleArea.layout(true);
			}
	
			// show the message
			setMessage(fMessage);
	
		} else {
			fMessageLabel.setText(errorMessage);
			if (fMessageLabel.getImage() == null) {
				// we were not previously showing an error
							
				// lazy initialize the error background color and image
				if (fErrorMsgImage == null) {
					fErrorMsgImage = JDIDebugUIPlugin.getDefault().getImageRegistry().get(PREF_DLG_IMG_TITLE_ERROR);
				}
	
				// show the error	
				fNormalMsgAreaBackground = fMessageLabel.getBackground();
				fMessageLabel.setBackground(JFaceColors.getErrorBackground(fMessageLabel.getDisplay()));
				fMessageLabel.setImage(fErrorMsgImage);
				fTitleImage.setImage(null);
				fTitleArea.layout(true);
			}
		}
	}
	/**
	 * Set the message text. If the message line currently displays an error,
	 * the message is stored and will be shown after a call to clearErrorMessage
	 */
	public void setMessage(String newMessage) {
		fMessage = newMessage;
		if (fMessage == null) {
			fMessage = "";//$NON-NLS-1$
		}
		if (fMessageLabel.getImage() == null) {
			// we are not showing an error
			fMessageLabel.setText(fMessage);
		}
	}
	
	/**
	 * @see IPreferencePageContainer#updateMessage()
	 */
	public void updateMessage() {
		String pageMessage = fPage.getMessage();
		String pageErrorMessage = fPage.getErrorMessage();

		// Adjust the font
		if (pageMessage == null && pageErrorMessage == null)
			fMessageLabel.setFont(JFaceResources.getBannerFont());
		else
			fMessageLabel.setFont(JFaceResources.getDialogFont());

		// Set the message and error message	
		if (pageMessage == null) {
			setMessage(fPage.getTitle());
		} else {
			setMessage(pageMessage);
		}
		setErrorMessage(pageErrorMessage);
	}
	
	protected IJavaBreakpoint getBreakpoint() {
		return fBreakpoint;
	}

	protected void setBreakpoint(IJavaBreakpoint breakpoint) {
		fBreakpoint = breakpoint;
	}
	
	/**
	 * @see IPreferencePageContainer#getPreferenceStore()
	 */
	public IPreferenceStore getPreferenceStore() {
		return fJavaBreakpointPreferenceStore;
	}

	/**
	 * @see IPreferencePageContainer#updateButtons()
	 */
	public void updateButtons() {
		if (fOkButton != null) {
			fOkButton.setEnabled(fPage.isValid());
		}
	}

	/**
	 * @see IPreferencePageContainer#updateTitle()
	 */
	public void updateTitle() {
		setTitle(fPage.getTitle());
	}
	
	/**
	 * @see Dialog#createButtonsForButtonBar(Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fOkButton= createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
}