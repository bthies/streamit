package streamit.eclipse.debugger.launching;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaSourceLookupTab;

/**
 * @author kkuo
 */
public class LocalStreamItApplicationTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			//new JavaMainTab(),
			new StreamItMainTab(),
			new StreamItArgumentsTab(),
			new JavaJRETab(),
			new JavaClasspathTab(),
			new JavaSourceLookupTab(),
			new CommonTab()
		};
		setTabs(tabs);
	}
}