package streamit.eclipse.debugger.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * @author kkuo
 */
public class NewProjectCreationWizard extends NewElementWizard implements IExecutableExtension {

	private NewProjectCreationWizardPage fStreamItPage;
	private WizardNewProjectCreationPage fMainPage;
	private IConfigurationElement fConfigElement;
	
	public NewProjectCreationWizard() {
		super();
	
		JavaCapabilityConfigurationPage j;	
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString(IStreamItWizardsConstants.NEW_PROJECT_CREATION_WIZARD_TITLE));
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage(IStreamItWizardsConstants.NEW_PROJECT_CREATION_WIZARD);
		fMainPage.setTitle(NewWizardMessages.getString(IStreamItWizardsConstants.NEW_PROJECT_CREATION_WIZARD_MAINPAGE_TITLE));
		fMainPage.setDescription(NewWizardMessages.getString(IStreamItWizardsConstants.NEW_PROJECT_CREATION_WIZARD_MAINPAGE_DESCRIPTION));
		addPage(fMainPage);
		fStreamItPage= new NewProjectCreationWizardPage(fMainPage);
		addPage(fStreamItPage);
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fStreamItPage.performFinish(monitor); // use the full progress monitor
		BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
		selectAndReveal(fStreamItPage.getJavaProject().getProject());
	}
	
	protected void handleFinishException(Shell shell, InvocationTargetException e) {
		String title= NewWizardMessages.getString(IStreamItWizardsConstants.NEW_PROJECT_CREATION_WIZARD_ERROR_TITLE);
		String message= NewWizardMessages.getString(IStreamItWizardsConstants.NEW_PROJECT_CREATION_WIZARD_ERROR_MESSAGE);
		ExceptionHandler.handle(e, getShell(), title, message);
	}

	/*
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}
	
	/* (non-Javadoc)
	 * @see IWizard#performCancel()
	 */
	public boolean performCancel() {
		fStreamItPage.performCancel();
		return super.performCancel();
	}
}