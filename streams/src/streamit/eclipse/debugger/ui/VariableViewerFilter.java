package streamit.eclipse.debugger.ui;

import java.util.Vector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import streamit.eclipse.debugger.core.StreamItDebugEventSetListener;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;

/**
 * @author kkuo
 */
public class VariableViewerFilter extends ViewerFilter {

	private String[] fFilterVariables = null;
	private Vector filteredElements = null;
	private String fAbsoluteJavaFileName = null;
	
	
	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		
		// only handle if StreamIt Launch
		if (parent instanceof IJavaVariable) {
			IJavaVariable parentVar = (IJavaVariable) parent;
	
			try {
				ILaunchConfiguration config = isStreamItLaunch(parentVar);
				if (config == null) return elements;

				String projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
				String mainClassName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
				String javaFileName = mainClassName + '.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION;
				fAbsoluteJavaFileName = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getFile(javaFileName).getLocation().toOSString();

				fFilterVariables = StreamItDebugEventSetListener.getFilterVariables(fAbsoluteJavaFileName, parentVar.getReferenceTypeName());
			} catch (CoreException e) {
			}	
		} else if (parent instanceof IJavaStackFrame) {
			IJavaStackFrame parentFrame = (IJavaStackFrame) parent;
			if (isStreamItLaunch(parentFrame) == null) return elements;
			
		}
		
		// filter JDIStackFrame, JDIThisVariable parent
		filteredElements = new Vector();
		for (int i = 0; i < elements.length; i++) {
			if (select(viewer, parent, elements[i])) {
				filteredElements.add(elements[i]);
			}
		}
		return filteredElements.toArray();		
	}
	
	private ILaunchConfiguration isStreamItLaunch(IDebugElement element) {
		ILaunchConfiguration config = element.getLaunch().getLaunchConfiguration();
		if (config.getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return null;
		return config;
	}
	
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// parentElement is usually JDIStackFrame, JDIThisVariable
		// child element is usually JDIThisVariable, JDIFieldVariable

		try {
			if (parentElement instanceof IJavaVariable && element instanceof IJavaFieldVariable) {
				String fieldName = ((IJavaFieldVariable) element).getName();
				
				if (fFilterVariables != null) {
					return findField(fieldName);
				} else {
					if (fieldName.equals("streamElements") || fieldName.equals("childrenStreams")) {
						// find header
						IVariable[] vars = ((IJavaFieldVariable) element).getValue().getVariables();
						for (int i = 0; i < vars.length; i++) {
							if (vars[i].getName().equals("header")) {
								vars = vars[i].getValue().getVariables();
								break;
							}
						}
						
						// find elements
						for (int i = 0; i < vars.length; i++) {
							if (vars[i].getName().equals("next")) { 
								accumulate(vars[i].getValue().getVariables());
								break;
							}
						}
					} else if (fieldName.equals("body") || fieldName.equals("loop")) {
						return true;
					} else {
						String filterName = ((IJavaVariable) parentElement).getValue().getReferenceTypeName();
						fFilterVariables = StreamItDebugEventSetListener.getFilterVariables(fAbsoluteJavaFileName, filterName);
						return findField(fieldName);				
					}
				}				
			} else if (parentElement instanceof IJavaStackFrame && element instanceof IJavaVariable) {
				String name = ((IJavaVariable) element).getName();
				if (name.equals("this") || name.equals("program")) return true;
			}

		} catch (DebugException e) {
		}
		return false;	
	}
	
	private boolean findField(String fieldName) {
		if (fFilterVariables == null) return false;
		for (int i = 0; i < fFilterVariables.length; i++) 
			if (fFilterVariables[i].equals(fieldName)) return true;
		return false;
	}
	
	private void accumulate(IVariable[] vars) throws DebugException {
		IVariable element = null;
		IValue next = null;
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals("element")) element = vars[i];
			else if (vars[i].getName().equals("next")) next = vars[i].getValue();
		}
		
		if (!element.getValue().getValueString().equals("null")) {
			filteredElements.add(element);
			accumulate(next.getVariables());
		}
	}
}