package streamit.eclipse.debugger.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
	private IFile fJavaFile, fPrimaryStrFile;
	private List methodBreakpoints, watchpoints;
	
	public PreDebuggingRunnable(IProject project, String absoluteJavaFileName, String javaFileName, IFile javaFile, IFile primaryStrFile) {
		fProject = project;
		fAbsoluteJavaFileName = absoluteJavaFileName;
		fJavaFileName = javaFileName;
		fJavaFile = javaFile;
		fPrimaryStrFile = primaryStrFile;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			// create launchData
			StrJavaMapper.getInstance().loadStrFile(fPrimaryStrFile, true);
			LaunchData launchData = StreamItDebugEventFilter.getInstance().beforeLaunch(fJavaFileName, fJavaFile);

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
				
				if (superclassName.equals(IStreamItCoreConstants.FILTER_CLASS)) processFilter(streamType, fJavaFile, launchData);
				if (!superclassName.equals(IStreamItCoreConstants.STREAMIT_CLASS)) continue;
				
				IMethod[] methods = streamType.getMethods();
				String methodName;
				for (int j = 0; j < methods.length; j++) {
					methodName = methods[j].getElementName();
					if (!methodName.equals(IStreamItCoreConstants.INIT_METHOD)) continue;

					// add method breakpoint to .java
					int methodStart = methods[j].getNameRange().getOffset();
					int methodEnd = methodStart + methods[j].getNameRange().getLength();
					String typeName = streamType.getFullyQualifiedName();
	
					// handle if method breakpoint already exists
					IJavaMethodBreakpoint breakpoint = StreamItDebugEventFilter.getInstance().getMethodBreakpoint(typeName, methodStart, methodEnd);
					if (breakpoint == null) {
						Map attributes = new HashMap(10);
						BreakpointUtils.addJavaBreakpointAttributes(attributes, methods[j]);
						String methodSignature = methods[j].getSignature();
						if (!streamType.isBinary()) methodSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(streamType, methodSignature);
						launchData.setInitBreakpoint(JDIDebugModel.createMethodBreakpoint(fJavaFile, typeName, methodName, methodSignature, true, true, false, -1, methodStart, methodEnd, 0, true, attributes));
					} else {
						breakpoint.setEntry(true);
						breakpoint.setExit(true);
						launchData.setInitBreakpoint(breakpoint); 
					}
					break;
				}
			}
			
			launchData.addStaticFilterBreakpoints();
		} catch (CoreException ce) {
		} catch (IOException ce) {
		}
	}
	
	private void processFilter(IType streamType, IFile javaFile, LaunchData launchData) throws JavaModelException, CoreException {
		// find and store work method information
		IMethod[] methods = streamType.getMethods();
		String methodName;
		for (int i = 0; i < methods.length; i++) {
			methodName = methods[i].getElementName();
			if (!methodName.equals(IStreamItCoreConstants.WORK_METHOD)) continue;

			launchData.addFilterWorkMethod(methods[i], streamType, methodName);
			break;
		}
	 }
}