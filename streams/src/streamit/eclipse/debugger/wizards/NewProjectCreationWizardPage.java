package streamit.eclipse.debugger.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.ClassPathDetector;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;


/**
 * @author kkuo
 */
public class NewProjectCreationWizardPage extends JavaCapabilityConfigurationPage {


	private WizardNewProjectCreationPage fMainPage;

	private IPath fCurrProjectLocation;
	protected IProject fCurrProject;
	
	protected boolean fCanRemoveContent;

	/**
	 * Constructor for NewProjectCreationWizardPage.
	 */
	public NewProjectCreationWizardPage(WizardNewProjectCreationPage mainPage) {
		super();
		
		setTitle(NewWizardMessages.getString("StreamItCapabilityConfigurationPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("StreamItCapabilityConfigurationPage.description")); //$NON-NLS-1$
		
		fMainPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
		fCanRemoveContent= false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			changeToNewProject();
		} else {
			removeProject();
		}
		super.setVisible(visible);
	}
	
	private void changeToNewProject() {
		IProject newProjectHandle= fMainPage.getProjectHandle();
		IPath newProjectLocation= fMainPage.getLocationPath();
		
		if (fMainPage.useDefaults()) {
			fCanRemoveContent= !newProjectLocation.append(fMainPage.getProjectName()).toFile().exists();
		} else {
			fCanRemoveContent= !newProjectLocation.toFile().exists();
		}
				
		final boolean initialize= !(newProjectHandle.equals(fCurrProject) && newProjectLocation.equals(fCurrProjectLocation));
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					updateProject(initialize, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 
			}
		};
	
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.error.desc");			 //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	protected void updateProject(boolean initialize, IProgressMonitor monitor) throws CoreException, InterruptedException {
		fCurrProject= fMainPage.getProjectHandle();
		fCurrProjectLocation= fMainPage.getLocationPath();
		boolean noProgressMonitor= !initialize && fCanRemoveContent;
		
		if (monitor == null || noProgressMonitor ) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.desc"), 2);				 //$NON-NLS-1$
			
			createProject(fCurrProject, fCurrProjectLocation, new SubProgressMonitor(monitor, 1));
			if (initialize) {
				IClasspathEntry[] entries= null;
				IPath outputLocation= null;
		
				if (fCurrProjectLocation.toFile().exists() && !Platform.getLocation().equals(fCurrProjectLocation)) {
					// detect classpath
					if (!fCurrProject.getFile(".classpath").exists()) { //$NON-NLS-1$
						// if .classpath exists noneed to look for files
						ClassPathDetector detector= new ClassPathDetector(fCurrProject);
						entries= detector.getClasspath();
						outputLocation= detector.getOutputLocation();
					}
				}				
				init(JavaCore.create(fCurrProject), outputLocation, entries, false);
			}
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Called from the wizard on finish.
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.createproject.desc"), 3);				 //$NON-NLS-1$
			if (fCurrProject == null) {
				updateProject(true, new SubProgressMonitor(monitor, 1));
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
			
			StreamItProjectNature sipn = new StreamItProjectNature();
			sipn.setProject(getJavaProject().getProject());
			sipn.configure();
		} finally {
			monitor.done();
			fCurrProject= null;
		}
	}

	private void removeProject() {
		if (fCurrProject == null || !fCurrProject.exists()) {
			return;
		}
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				boolean noProgressMonitor= Platform.getLocation().equals(fCurrProjectLocation);
				if (monitor == null || noProgressMonitor) {
					monitor= new NullProgressMonitor();
				}
				monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.removeproject.desc"), 3);				 //$NON-NLS-1$

				try {
					fCurrProject.delete(fCanRemoveContent, false, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
					fCurrProject= null;
					fCanRemoveContent= false;
				}
			}
		};
	
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("NewProjectCreationWizardPage.op_error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewProjectCreationWizardPage.op_error_remove.message");			 //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);		
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}		
			
	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		removeProject();
	}
}