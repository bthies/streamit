package streamit.eclipse.debugger.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public class StreamItProjectNature implements IProjectNature {

	private IProject fProject;
	public static String NATURE_ID = StreamItDebuggerPlugin.getUniqueIdentifier() + ".streamitnature";

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription description = fProject.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = NATURE_ID;
		description.setNatureIds(newNatures);
		fProject.setDescription(description, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProjectDescription description = fProject.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length - 1];
		for (int i = 0; i < natures.length; i++) {
			if (!natures[i].equals(NATURE_ID))
				newNatures[i] = natures[i];
		}
		description.setNatureIds(newNatures);
		fProject.setDescription(description, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return fProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		fProject = project;
	}
}
