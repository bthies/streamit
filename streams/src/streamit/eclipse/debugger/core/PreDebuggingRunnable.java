package streamit.eclipse.debugger.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.actions.ManageMethodBreakpointActionDelegate;

/**
 * @author kkuo
 */
public class PreDebuggingRunnable implements Runnable {

	private IProject fProject;
	private String fAbsoluteJavaFileName;
	private String fJavaFileName;
	private IFile fJavaFile;
	private List methodBreakpoints, watchpoints;
	
	public PreDebuggingRunnable(IProject project, String absoluteJavaFileName, String javaFileName, IFile javaFile) {
		fProject = project;
		fAbsoluteJavaFileName = absoluteJavaFileName;
		fJavaFileName = javaFileName;
		fJavaFile = javaFile;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			fJavaFile.refreshLocal(IResource.DEPTH_ONE, null);
			
			LaunchData launchData = StreamItDebugEventSetListener.getInstance().beforeLaunch(fAbsoluteJavaFileName, fJavaFileName, fJavaFile);

			// get all types
			ICompilationUnit unit = (ICompilationUnit) JavaCore.create(fJavaFile);
			synchronized (unit) {
				unit.reconcile();
			}
			IType[] types = unit.getAllTypes();

			// add method breakpoint to all init and work functions
			setPoints(IStreamItDebuggerConstants.ID_JAVA_METHOD_BREAKPOINT, IStreamItDebuggerConstants.ID_JAVA_WATCHPOINT);
			boolean setChannelWatchpoint = true;
			for (int i = 0; i < types.length; i++) {
				String superclassName = types[i].getSuperclassName();

				if (superclassName.equals(IStreamItDebuggerConstants.FILTER_CLASS)) {
					if (setChannelWatchpoint) {
						processChannel(types[i]);
						setChannelWatchpoint = false;
					}
					processFilter(types[i], fJavaFile, launchData);
				} else if (superclassName.equals(IStreamItDebuggerConstants.PIPELINE_CLASS)) {
					processPipeline(types[i], launchData, false);
				} else if (superclassName.equals(IStreamItDebuggerConstants.STREAMIT_CLASS))  {
					processPipeline(types[i], launchData, true);
				} else if (superclassName.equals(IStreamItDebuggerConstants.SPLITJOIN_CLASS)) {
					processSplitJoin(types[i], launchData);
				} else if (superclassName.equals(IStreamItDebuggerConstants.FEEDBACKLOOP_CLASS)) {
					processFeedbackLoop(types[i], launchData);
				}
			}
		} catch (Exception e) {
		}
	}
	
	private void processChannel(IType streamType) throws Exception {
		// add modification only watchpoint to totalItemsPushed, totalItemsPopped, queue; also streamElements
		// open hierarchy to get streamit.Channel class
		ITypeHierarchy hier = streamType.newSupertypeHierarchy(null);
		IType type = hier.getSuperclass(streamType); // filter
		type = hier.getSuperclass(type); // stream
		type = hier.getSuperclass(type); // operator
		type = hier.getSuperclass(type); // DestroyedClass
		hier = type.newTypeHierarchy(null);
		IType[] ts = hier.getSubtypes(type);
		for (int j = 0; j < ts.length; j++) {
			if (ts[j].getElementName().equals(IStreamItDebuggerConstants.CHANNEL)) {
				type = ts[j];
				break;
			}
		}
								
		String typeName = type.getFullyQualifiedName();
		// add watchpoint for totalItemsPushed, totalItemsPopped, queue
		addWatchpoint(typeName, type, IStreamItDebuggerConstants.TOTALITEMSPUSHED_FIELD);
		addWatchpoint(typeName, type, IStreamItDebuggerConstants.TOTALITEMSPOPPED_FIELD);
		addWatchpoint(typeName, type, IStreamItDebuggerConstants.QUEUE_FIELD);
	}
	
	private void addWatchpoint(String typeName, IType type, String searchField) throws CoreException {
		IField field = type.getField(searchField); 
		String fieldName = field.getElementName();
		if (!isWatchpoint(typeName, fieldName)) {
			Map attributes = new HashMap(10);
			BreakpointUtils.addJavaBreakpointAttributes(attributes, field);
			JDIDebugModel.createWatchpoint(fProject, typeName, fieldName, -1, -1, -1, -1, true, attributes).setAccess(false);
		}
	}

	private void processFilter(IType streamType, IFile javaFile, LaunchData launchData) throws Exception {
		// add mapping between filter name and variables
		IField[] fields = streamType.getFields();
		String[] vars = new String[fields.length];
		for (int j = 0; j < fields.length; j++) {
			vars[j] = fields[j].getElementName();
		}
		launchData.addFilterVariables(streamType.getElementName(), vars);
							
		processStream(streamType, launchData, false);
	}
			
	private void setPoints(String breakpointMarkerType, String watchpointMarkerType) throws CoreException {
		methodBreakpoints = new Vector();
		watchpoints = new Vector();

		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] breakpoints = manager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			if (breakpoints[i] instanceof IJavaMethodBreakpoint) {
				IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) breakpoints[i];
				if (breakpoint.getMarker().getType().equals(breakpointMarkerType)) methodBreakpoints.add(breakpoint);
			} else if (breakpoints[i] instanceof IJavaWatchpoint) {
				IJavaWatchpoint breakpoint = (IJavaWatchpoint) breakpoints[i];
				if (breakpoint.getMarker().getType().equals(watchpointMarkerType)) watchpoints.add(breakpoint);
			}
		}
	}
			
	private IJavaMethodBreakpoint getMethodBreakpoint(String typeName, int methodStart, int methodEnd) throws CoreException {
		for (int i = 0; i < methodBreakpoints.size(); i++) {
			IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) methodBreakpoints.get(i);
			if (breakpoint.getTypeName().equals(typeName)
				&& breakpoint.getCharStart() == methodStart && breakpoint.getCharEnd() == methodEnd) {
					return breakpoint;
			}
		}
		return null;
	}
			
	private boolean isWatchpoint(String typeName, String fieldName) throws CoreException {
		for (int i = 0; i < watchpoints.size(); i++) {
			IJavaWatchpoint breakpoint = (IJavaWatchpoint) watchpoints.get(i);
			if (breakpoint.getTypeName().equals(typeName) && breakpoint.getFieldName().equals(fieldName)) return true;
		}
		return false;
	}
	
	private void processPipeline(IType streamType, LaunchData launchData, boolean topLevelPipeline) throws Exception {
		launchData.addPipelineName(streamType.getElementName());
		processStream(streamType, launchData, topLevelPipeline);
	}

	private void processSplitJoin(IType streamType, LaunchData launchData) throws Exception {
		launchData.addFeedbackLoopName(streamType.getElementName());
		processStream(streamType, launchData, false);
	}
	
	private void processFeedbackLoop(IType streamType, LaunchData launchData) throws Exception {
		launchData.addSplitjoinName(streamType.getElementName());
		processStream(streamType, launchData, false);
	}
		
	private void processStream(IType streamType, LaunchData launchData, boolean topLevelPipeline) throws Exception {
		// add breakpoints to all functions
		IMethod[] methods = streamType.getMethods();
		String methodName;
		boolean entry, exit;
		int methodType;
		for (int j = 0; j < methods.length; j++) {
			methodName = methods[j].getElementName();
								
			if (methodName.equals(IStreamItDebuggerConstants.INIT_METHOD)) { // exit only, unless topLevelPipeline
				entry = topLevelPipeline; //false;
				exit = true;
				methodType = IStreamItDebuggerConstants.INIT_BREAKPOINTS;
			} else if (methodName.equals(IStreamItDebuggerConstants.WORK_METHOD)) { // entry & exit
				entry = true;
				exit = true;
				methodType = IStreamItDebuggerConstants.WORK_BREAKPOINTS;
			} else if (methodName.equals(IStreamItDebuggerConstants.PREWORK_METHOD)) { // entry only
				entry = true;
				exit = false;
				methodType = IStreamItDebuggerConstants.PREWORK_BREAKPOINTS;
			} else if (methodName.equals(IStreamItDebuggerConstants.MAIN_METHOD)) {				
				continue;
			} else { 															// entry & exit
				entry = true;
				exit = true;
				methodType = IStreamItDebuggerConstants.OTHER_BREAKPOINTS;
			}

			// add method breakpoint to .java
			int methodStart = methods[j].getNameRange().getOffset();
			int methodEnd = methodStart + methods[j].getNameRange().getLength();
			String typeName = streamType.getFullyQualifiedName();
	
			// handle if method breakpoint already exists
			IJavaMethodBreakpoint breakpoint = getMethodBreakpoint(typeName, methodStart, methodEnd);
			if (breakpoint == null) {
				Map attributes = new HashMap(10);
				BreakpointUtils.addJavaBreakpointAttributes(attributes, methods[j]);
				methodName = methods[j].getElementName();
				String methodSignature = methods[j].getSignature();
				if (!streamType.isBinary()) methodSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(streamType, methodSignature);
				launchData.addBreakpoint(methodType, true, JDIDebugModel.createMethodBreakpoint(fJavaFile, typeName, methodName, methodSignature, entry, exit, false, -1, methodStart, methodEnd, 0, true, attributes));
			} else {
				launchData.addBreakpoint(methodType, !breakpoint.isEntry(), breakpoint);
				breakpoint.setEntry(entry);
				breakpoint.setExit(exit);
			}
		}
	}
}
