package streamit.eclipse.debugger.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.internal.debug.ui.actions.ManageMethodBreakpointActionDelegate;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;
import streamit.eclipse.debugger.graph.OptionData;
import streamit.eclipse.debugger.model.Filter;
import streamit.eclipse.debugger.model.Pipeline;
import streamit.eclipse.debugger.model.StreamStructure;
import streamit.eclipse.debugger.model.StreamStructureModelFactory;

/**
 * @author kkuo
 */
public class LaunchData {
	
	private IFile fJavaFile;

	// key = filter name (without id), entry = FilterWorkMethodInformation
	private HashMap fFilterWorkMethods, fOldFilterWorkMethods;
	
	// for setting input
	private IJavaMethodBreakpoint fInitBreakpoint;
	
	// for stepping
	private boolean fClientStepRequest; // if the client asks to step through code 
	private int fPrevStrLineNumber; 	// 1 based

	// for setting graph input
	private Pipeline fStreamItModel;
	private IVariable fModelInput;
	private OptionData fOptionData;
	
	// for static graphs
	private boolean fTerminated;

	protected LaunchData(IFile javaFile) {
		fJavaFile = javaFile;
		
		fFilterWorkMethods = new HashMap();
		fOldFilterWorkMethods = new HashMap();
		
		fInitBreakpoint = null;
		
		fClientStepRequest = false;
		fPrevStrLineNumber = -1;
		
		fStreamItModel = null;
		fModelInput = null;
		fOptionData = new OptionData(false, this);
		
		fTerminated = false;
	}
	
	public IFile getJavaFile() {
		return fJavaFile;
	}

	// Filter Work Method Breakpoint
	protected void addFilterWorkMethod(IMethod method, IType streamType, String methodName) throws JavaModelException {
		// store method breakpoint information
		int methodStart = method.getNameRange().getOffset();
		int methodEnd = methodStart + method.getNameRange().getLength();
		String typeName = streamType.getFullyQualifiedName();
		String methodSignature = method.getSignature();
		
		if (!streamType.isBinary()) methodSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(streamType, methodSignature);

		fFilterWorkMethods.put(streamType.getElementName(), new FilterWorkMethodInformation(method, methodStart, methodEnd, typeName, methodSignature, methodName));
	}
	
	protected FilterWorkMethodInformation getFilterWorkMethodInformation(String filterName) {
		return (FilterWorkMethodInformation) fFilterWorkMethods.get(filterName);
	}
	
	public boolean hasFilterInstanceBreakpoint(String filterName, String filterRuntimeId, String filterStaticId) {
		FilterWorkMethodInformation info = (FilterWorkMethodInformation) fFilterWorkMethods.get(filterName);
		if (info == null) return false;
		return info.hasId(filterRuntimeId, filterStaticId, fTerminated);
	}
	
	protected HashMap getStaticFilterInstanceBreakpoints() {
		return fFilterWorkMethods;
	}
	
	protected void addStaticFilterInstanceBreakpoints(HashMap filterWorkMethods) {
		fOldFilterWorkMethods = filterWorkMethods;
	}
	
	protected void addStaticFilterBreakpoints() throws CoreException {
		Iterator i = fOldFilterWorkMethods.keySet().iterator();
		String filterName;
		FilterWorkMethodInformation info;
		while (i.hasNext()) {
			filterName = (String) i.next();
			info = ((FilterWorkMethodInformation) fFilterWorkMethods.get(filterName));
			if (info == null) continue;
			info.addOldStaticIdsWithBreakpoints(((FilterWorkMethodInformation) fOldFilterWorkMethods.get(filterName)).getStaticIdsWithBreakpoints());
		}
		fOldFilterWorkMethods = null;
	}
	
	// Init Breakpoint
	protected void setInitBreakpoint(IJavaMethodBreakpoint breakpoint) throws CoreException {
		fInitBreakpoint = breakpoint;
	}
	
	protected boolean isInitBreakpoint(IBreakpoint b) {
		if (fInitBreakpoint == null) return false;
		return fInitBreakpoint.equals(b);
	}
	
	// Step Breakpoint
	public boolean isClientStepRequest() {
		return fClientStepRequest;
	}

	public void setClientStepRequest(boolean b) {
		fClientStepRequest = b;
	}

	public int getPrevStrLineNumber() {
		return fPrevStrLineNumber;
	}
	
	public void setPreviousLineNumber(int strLine) {
		fPrevStrLineNumber = strLine;
	}

	// StreamIt Model
	public Pipeline getStreamItModel() {
		return fStreamItModel;
	}

	public void makeStreamItModel(IVariable variable) {
		fModelInput = variable;
		updateStreamItModel();
	}

	public void updateStreamItModel() {
		try {
			fStreamItModel = StreamStructureModelFactory.getInstance().makeStream(fModelInput);
		} catch (DebugException e) {
		}
	}
	
	public OptionData getOptionData(boolean highlighting) {
		fOptionData.setHighlightSelect(highlighting);
		return fOptionData;
	}

	// for static graphs
	public void setTerminated(boolean b) {
		fTerminated = b;
		
		// move runtime filter breakpoints to static breakpoints
		Iterator i = fFilterWorkMethods.keySet().iterator();
		while (i.hasNext()) {
			((FilterWorkMethodInformation) fFilterWorkMethods.get(i.next())).moveRuntimeToStaticIds();
		}
	}
	
	public boolean isTerminated() {
		return fTerminated;
	}
	
	public String getStaticId(String filterName, String runtimeId) {
		if (fStreamItModel == null) return null;
		String staticId = IStreamItGraphConstants.ZERO_STRING;
		staticId = getStaticIdHelper(filterName + runtimeId, staticId, fStreamItModel.getChildStreams());
		if (staticId == null) return null;
		return staticId;
	}
	
	private String getStaticIdHelper(String filterNameWithRuntimeId, String staticId, Vector children) {
		String find;
		for (int i = 0; i < children.size(); i++) {
			StreamStructure element = (StreamStructure) children.get(i);
			if (element instanceof Filter) {
				if (element.getNameWithRuntimeId().equals(filterNameWithRuntimeId)) return staticId + i;
			} else {
				find = getStaticIdHelper(filterNameWithRuntimeId, staticId + i, element.getChildStreams());
				if (find != null) return find;
			}
		}
		return null;
	}
}