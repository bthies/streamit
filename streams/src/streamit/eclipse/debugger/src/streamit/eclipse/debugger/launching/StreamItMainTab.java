package streamit.eclipse.debugger.launching;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.ui.IJavaDebugHelpContextIds;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.JavaLaunchConfigurationTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodFinder;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * @author kkuo
 */
public class StreamItMainTab extends JavaLaunchConfigurationTab {
		
	// Project UI widgets
	protected Label fProjLabel;
	protected Text fProjText;
	protected Button fProjButton;

	// Main class UI widgets
	protected Label fMainLabel;
	protected Text fMainText;
	protected Button fSearchButton;
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		WorkbenchHelp.setHelp(getControl(), IJavaDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		
		GridData gd;
		
		createVerticalSpacer(comp, 1);
		
		Composite projComp = new Composite(comp, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 2;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);
		projComp.setFont(font);
		
		fProjLabel = new Label(projComp, SWT.NONE);
		fProjLabel.setText(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_PROJECT)); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);
		fProjLabel.setFont(font);
		
		fProjText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.setFont(font);
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fProjButton = createPushButton(projComp, LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_BROWSE), null); //$NON-NLS-1$
		fProjButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
		
		createVerticalSpacer(comp, 1);

		Composite mainComp = new Composite(comp, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComp.setLayoutData(gd);
		mainComp.setFont(font);
		
		// TODO change what Main Tab says
		fMainLabel = new Label(mainComp, SWT.NONE);
		fMainLabel.setText(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_MAIN_CLASSES)); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fMainLabel.setLayoutData(gd);
		fMainLabel.setFont(font);

		fMainText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fMainText.setLayoutData(gd);
		fMainText.setFont(font);
		fMainText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fSearchButton = createPushButton(mainComp,LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_SEARCH), null); //$NON-NLS-1$
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
			}
		});

	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		updateProjectFromConfig(config);
		updateMainTypeFromConfig(config);
	}
	
	protected void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName = IStreamItLaunchingConstants.EMPTY_STRING; //$NON-NLS-1$
		try {
			projectName = StreamItLocalApplicationLaunchConfigurationDelegate.getProjectName(config);	
		} catch (CoreException ce) {
			JDIDebugUIPlugin.log(ce);
		}
		fProjText.setText(projectName);
	}
	
	protected void updateMainTypeFromConfig(ILaunchConfiguration config) {
		String mainTypeName = IStreamItLaunchingConstants.EMPTY_STRING; //$NON-NLS-1$
		try {
			mainTypeName = StreamItLocalApplicationLaunchConfigurationDelegate.getMainClassName(config);
		} catch (CoreException ce) {
			JDIDebugUIPlugin.log(ce);	
		}	
		fMainText.setText(mainTypeName);	
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)fProjText.getText());
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)fMainText.getText());
		
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, (String)null);
		config.setAttribute(IStreamItLaunchingConstants.ATTR_INCLUDE_EXTERNAL_JARS, (String)null);
	}
			
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}

	/**
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected() {
		
		IJavaProject javaProject = getJavaProject();
		IJavaSearchScope searchScope = null;
		if ((javaProject == null) || !javaProject.exists()) {
			searchScope = SearchEngine.createWorkspaceScope();
		} else {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, false);
		}		
		
		int constraints = IJavaElementSearchConstants.CONSIDER_BINARIES;
		
		Shell shell = getShell();
		SelectionDialog dialog = JavaUI.createMainTypeDialog(shell, 
															 getLaunchConfigurationDialog(), 
															 searchScope, 
															 constraints, 
															 false, 
															 fMainText.getText()); 
		dialog.setTitle(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_CHOOSE_MAIN_TYPE)); //$NON-NLS-1$
		dialog.setMessage(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_CHOOSE_MAIN_TYPE_LAUNCH)); //$NON-NLS-1$
		if (dialog.open() == SelectionDialog.CANCEL) {
			return;
		}
		
		Object[] results = dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return;
		}		
		IType type = (IType)results[0];
		if (type != null) {
			fMainText.setText(type.getFullyQualifiedName());
			javaProject = type.getJavaProject();
			fProjText.setText(javaProject.getElementName());
		}
	}
		
	/**
	 * Show a dialog that lets the user select a project.  This in turn provides
	 * context for the main type, allowing the user to key a main type name, or
	 * constraining the search for main types to the specified project.
	 */
	protected void handleProjectButtonSelected() {
		IJavaProject project = chooseJavaProject();
		if (project == null) {
			return;
		}
		
		String projectName = project.getElementName();
		fProjText.setText(projectName);		
	}
	
	/**
	 * Realize a Java Project selection dialog and return the first selected project,
	 * or null if there was none.
	 */
	protected IJavaProject chooseJavaProject() {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(getWorkspaceRoot()).getJavaProjects();
		} catch (JavaModelException e) {
			JDIDebugUIPlugin.log(e);
			projects= new IJavaProject[0];
		}
		
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_PROJECT_SELECTION)); //$NON-NLS-1$
		dialog.setMessage(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_PROJECT_TO_CONSTRAIN)); //$NON-NLS-1$
		dialog.setElements(projects);
		
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(new Object[] { javaProject });
		}
		if (dialog.open() == ElementListSelectionDialog.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
	
	/**
	 * Return the IJavaProject corresponding to the project name in the project name
	 * text field, or null if the text does not match a project name.
	 */
	protected IJavaProject getJavaProject() {
		String projectName = fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return getJavaModel().getJavaProject(projectName);		
	}
	
	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	/**
	 * Convenience method to get access to the java model.
	 */
	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		
		setErrorMessage(null);
		setMessage(null);
		
		String name = fProjText.getText().trim();
		if (name.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists()) {
				setErrorMessage(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_PROJECT_DOES_NOT_EXIST)); //$NON-NLS-1$
				return false;
			}
		}

		name = fMainText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage(LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_MAIN_TYPE_NOT_SPECIFIED)); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement = getContext();
		if (javaElement != null) {
			initializeJavaProject(javaElement, config);
		} else {
			// We set empty attributes for project & main type so that when one config is
			// compared to another, the existence of empty attributes doesn't cause an
			// incorrect result (the performApply() method can result in empty values
			// for these attributes being set on a config if there is nothing in the
			// corresponding text boxes)
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, IStreamItLaunchingConstants.EMPTY_STRING); //$NON-NLS-1$
		}
		initializeMainTypeAndName(javaElement, config);
	}

	/**
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	protected void initializeMainTypeAndName(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		String name= null;
		if (javaElement instanceof IMember) {
			IMember member = (IMember)javaElement;
			if (member.isBinary()) {
				javaElement = member.getClassFile();
			} else {
				javaElement = member.getCompilationUnit();
			}
		}
		if (javaElement instanceof ICompilationUnit || javaElement instanceof IClassFile) {
			try {
				IType[] types = MainMethodFinder.findTargets(new BusyIndicatorRunnableContext(), new Object[] {javaElement});
				if (types != null && (types.length > 0)) {
					// Simply grab the first main type found in the searched element
					name = types[0].getFullyQualifiedName();
				}
			} catch (InterruptedException ie) {
			} catch (InvocationTargetException ite) {
			}
		}
		if (name == null) {
			name= IStreamItLaunchingConstants.EMPTY_STRING; //$NON-NLS-1$
		}
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, name);
		if (name.length() > 0) {
			int index = name.lastIndexOf(IStreamItLaunchingConstants.PERIOD_CHAR);
			if (index > 0) {
				name = name.substring(index + 1);
			}		
			name = getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_MAIN_TAB_MAIN); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
	}

}