package streamit.eclipse.debugger.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
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

			// only need to add breakpoint to entry of top-level pipeline init function
			IType streamType;
			for (int i = 0; i < types.length; i++) {
				streamType = types[i];
				String superclassName = streamType.getSuperclassName();
				
				if (superclassName.equals(IStreamItDebuggerConstants.FILTER_CLASS)) processFilter(streamType, fJavaFile, launchData);
				if (!superclassName.equals(IStreamItDebuggerConstants.STREAMIT_CLASS)) continue;
				
				IMethod[] methods = streamType.getMethods();
				String methodName;
				for (int j = 0; j < methods.length; j++) {
					methodName = methods[j].getElementName();
					if (!methodName.equals(IStreamItDebuggerConstants.INIT_METHOD)) continue;

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
						launchData.setInitBreakpoint(JDIDebugModel.createMethodBreakpoint(fJavaFile, typeName, methodName, methodSignature, true, false, false, -1, methodStart, methodEnd, 0, true, attributes));
					} else {
						breakpoint.setEntry(true);
						breakpoint.setExit(false);
						launchData.setInitBreakpoint(breakpoint); 
					}
					return;
				}
			}
		} catch (Exception e) {
		}
	}
	
	private IJavaMethodBreakpoint getMethodBreakpoint(String typeName, int methodStart, int methodEnd) throws CoreException {
		String breakpointMarkerType = IStreamItDebuggerConstants.ID_JAVA_METHOD_BREAKPOINT;
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] breakpoints = manager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			if (!(breakpoints[i] instanceof IJavaMethodBreakpoint)) continue;
			IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) breakpoints[i];
			if (!(breakpoint.getMarker().getType().equals(breakpointMarkerType))) continue;
			if (breakpoint.getTypeName().equals(typeName) && breakpoint.getCharStart() == methodStart && breakpoint.getCharEnd() == methodEnd)
				return breakpoint;
		}
		return null;
	}
	
	private void processFilter(IType streamType, IFile javaFile, LaunchData launchData) throws Exception {
		// add mapping between filter name and variables
		IField[] fields = streamType.getFields();
		String[] vars = new String[fields.length];
		for (int j = 0; j < fields.length; j++) vars[j] = fields[j].getElementName();
		launchData.addFilterVariables(streamType.getElementName(), vars);
	 }
}