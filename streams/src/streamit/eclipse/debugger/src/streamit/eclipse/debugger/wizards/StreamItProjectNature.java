package streamit.eclipse.debugger.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

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
		if (fProject.hasNature(NATURE_ID)) return;
			
		IProjectDescription description = fProject.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = NATURE_ID;
		description.setNatureIds(newNatures);
		fProject.setDescription(description, null);
		
		// set streamit.jar
		IJavaProject javaProject = JavaCore.create(fProject);
		if (!javaProject.exists()) return;

		IPath entryPath = JavaCore.getClasspathVariable(IStreamItWizardsConstants.ECLIPSE_CLASSPATH_VARIABLE);
		if (entryPath == null) return;
		IPath jarPath = entryPath.append(IStreamItWizardsConstants.PLUGINS_DIRECTORY).addTrailingSeparator().append(IStreamItWizardsConstants.STREAMIT_DIRECTORY).addTrailingSeparator().append(IStreamItWizardsConstants.STREAMIT_JAR_FILENAME);
		if (!jarPath.isValidPath(jarPath.toString())) return;

		IClasspathEntry[] classpath = javaProject.getRawClasspath();
		IClasspathEntry[] newClasspath = new IClasspathEntry[classpath.length + 1];
		System.arraycopy(classpath, 0, newClasspath, 0, classpath.length);
		newClasspath[classpath.length] = JavaCore.newLibraryEntry(jarPath, null, null, false);
		javaProject.setRawClasspath(newClasspath, null);
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
