package streamit.eclipse.debugger.ui;

import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import streamit.eclipse.debugger.launching.StreamItLocalApplicationLaunchConfigurationDelegate;

/**
 * @author kkuo
 */
public class VariablesViewerFilter extends ViewerFilter {

	private Vector filteredElements = null;
	private IFile fJavaFile = null;
		
	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		
		// only handle if StreamIt Launch
		if (parent instanceof IJavaVariable) {
			if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(((IJavaVariable) parent).getLaunch())) return elements;
		} else if (parent instanceof IJavaStackFrame) {
			if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(((IJavaStackFrame) parent).getLaunch())) return elements;			
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

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// parentElement is usually JDIStackFrame, JDIThisVariable
		// child element is usually JDIThisVariable, JDIFieldVariable

		try {
			if (parentElement instanceof IJavaVariable && element instanceof IJavaFieldVariable) {
				IJavaFieldVariable var = (IJavaFieldVariable) element;
				String fieldName = var.getName();
				String parentName = var.getDeclaringType().getName();
				if (fieldName.equals(IStreamItUIConstants.STREAM_ELEMENTS_FIELD) && parentName.equals(IStreamItUIConstants.STREAM_CLASS)) {
					select(element);
				} else if (fieldName.equals(IStreamItUIConstants.CHILDREN_STREAMS_FIELD) && parentName.equals(IStreamItUIConstants.SPLITJOIN_CLASS)) {
					select(element);
				} else if ((fieldName.equals(IStreamItUIConstants.BODY_FIELD) || fieldName.equals(IStreamItUIConstants.LOOP_FIELD)) && parentName.equals(IStreamItUIConstants.FEEDBACKLOOP_CLASS)) {
					return true;
				} else {
					return findField(parentName);
				}
			} else if (parentElement instanceof IJavaStackFrame && element instanceof IJavaVariable) {
				String name = ((IJavaVariable) element).getName();
				if (!name.equals(IStreamItUIConstants.ALLFILTERS_FIELD) 
					&& !name.equals(IStreamItUIConstants.ALL_SINKS_FIELD)
					&& !name.equals(IStreamItUIConstants.DESTROYED_CLASS_FIELD)
					&& !name.equals(IStreamItUIConstants.FULL_CHANNELS_FIELD)
					&& !name.equals(IStreamItUIConstants.MESSAGE_STUB_FIELD)
					&& !name.equals(IStreamItUIConstants.TOTAL_BUFFER_FIELD)
					&& !name.equals(IStreamItUIConstants.ARGS_FIELD)) return true;
			}
		} catch (DebugException e) {
		}
		return false;
	}

	private void select(Object element) throws DebugException {
		// find header
		IVariable[] vars = ((IJavaFieldVariable) element).getValue().getVariables();
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals(IStreamItUIConstants.HEADER_FIELD)) {
				vars = vars[i].getValue().getVariables();
				break;
			}
		}
						
		// find elements
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals(IStreamItUIConstants.NEXT_FIELD)) {
				accumulate(vars[i].getValue().getVariables());
				break;
			}
		}
	}
	
	private void accumulate(IVariable[] vars) throws DebugException {
		IVariable element = null;
		IValue next = null;
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals(IStreamItUIConstants.ELEMENT_FIELD)) element = vars[i];
			else if (vars[i].getName().equals(IStreamItUIConstants.NEXT_FIELD)) next = vars[i].getValue();
		}
		
		if (!element.getValue().getValueString().equals(IStreamItUIConstants.NULL_VALUE)) {
			filteredElements.add(element);
			accumulate(next.getVariables());
		}
	}

	private boolean findField(String parentName) {
		if (parentName.equals(IStreamItUIConstants.FILTER_CLASS) ||
			parentName.equals(IStreamItUIConstants.DESTROYED_CLASS_CLASS) ||
			parentName.equals(IStreamItUIConstants.OPERATOR_CLASS) ||
			parentName.equals(IStreamItUIConstants.STREAM_CLASS)) {
			return false;		
		} else {
			return true;
		}
	}	
}