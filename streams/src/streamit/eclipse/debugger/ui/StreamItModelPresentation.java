package streamit.eclipse.debugger.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.DebugUIMessages;
import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;
import org.eclipse.swt.graphics.Image;

import streamit.eclipse.debugger.core.IStreamItDebuggerConstants;
import streamit.eclipse.debugger.core.StreamItDebugEventSetListener;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;

/**
 * @author kkuo
 */
public class StreamItModelPresentation extends JDIModelPresentation {

	IDebugModelPresentation fModel = DebugUITools.newDebugModelPresentation();
	
	public StreamItModelPresentation() {
		super();
	}

	/**
	 * @see IDebugModelPresentation#getImage(Object)
	 */
	public Image getImage(Object element) {
		return fModel.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		fModel.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return fModel.isLabelProperty(element, property);
	}
	
	// From org.eclipse.jdt.internal.debug.ui.JDIModelPresentation
	/**
	 * @see IDebugModelPresentation#getText(Object)
	 */
	public String getText(Object element) {
		String javaText = fModel.getText(element);
		
		try {
			if (element instanceof Launch) {
				Launch l = (Launch) element;  
				if (l.getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) != -1) {
					// HelloWorld6streamit.eclipse.debugger.launching.localStreamItApplication [Java Application]
					// replace with HelloWorld6 [StreamIt Application]
					return javaText.substring(0, javaText.indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION)) + " [" +  
						DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IStreamItLaunchingConstants.ID_STR_APPLICATION).getName()
						+ ']';				
				}
			} else if (element instanceof IJavaThread) {
				// Thread [main] (Suspended (breakpoint at line 7 in IntSource))

				// only handle if StreamIt
				if (((IJavaThread) element).getLaunch().getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) 
					return javaText;

				boolean showQualified = isShowQualifiedNames();
				StringBuffer label = new StringBuffer();
				label.append(getThreadText((IJavaThread) element, showQualified));
				if (((IJavaThread) element).isOutOfSynch()) {
					label.append(DebugUIMessages.getString("JDIModelPresentation._(out_of_synch)_1")); //$NON-NLS-1$
				} else if (((IJavaThread) element).mayBeOutOfSynch()) {
					label.append(DebugUIMessages.getString("JDIModelPresentation._(may_be_out_of_synch)_2")); //$NON-NLS-1$
				}
				
				if (element instanceof ITerminate) {
					if (((ITerminate) element).isTerminated()) {
						label.insert(0, DebugUIMessages.getString("JDIModelPresentation.<terminated>_2")); //$NON-NLS-1$
						return label.toString();
					}
				}
				if (element instanceof IDisconnect) {
					if (((IDisconnect) element).isDisconnected()) {
						label.insert(0, DebugUIMessages.getString("JDIModelPresentation.<disconnected>_4")); //$NON-NLS-1$
						return label.toString();
					}
				}
				return label.toString();
			} else if (element instanceof IJavaStackFrame) {
				// only handle if StreamIt
				if (((IJavaStackFrame) element).getLaunch().getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) 
					return javaText;

				StringBuffer label = new StringBuffer(getStackFrameText((IStackFrame) element));
				if (((IJavaStackFrame) element).isOutOfSynch()) return javaText;
				return label.toString();
			}
		} catch (CoreException e) {
		}
		
		return javaText;
	}
	
	/**
	 * Build the text for an IJavaThread.
	 */
	protected String getThreadText(IJavaThread thread, boolean qualified) throws CoreException {
		if (thread.isTerminated()) {
			if (thread.isSystemThread()) {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[({0}]_(Terminated)_7"), thread.getName()); //$NON-NLS-1$
			} else {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[({0}]_(Terminated)_8"), thread.getName()); //$NON-NLS-1$
			}
		}
		if (thread.isStepping()) {
			if (thread.isSystemThread()) {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Stepping)_9"), thread.getName()); //$NON-NLS-1$
			} else {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Stepping)_10"), thread.getName()); //$NON-NLS-1$
			}
		}
		if (thread.isPerformingEvaluation()) {
			if (thread.isSystemThread()) {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Evaluating)_9"), thread.getName()); //$NON-NLS-1$
			} else {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Evaluating)_10"), thread.getName()); //$NON-NLS-1$
			}
		}
		if (!thread.isSuspended() || (thread instanceof JDIThread && ((JDIThread)thread).isSuspendedQuiet())) {
			if (thread.isSystemThread()) {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Running)_11"), thread.getName()); //$NON-NLS-1$
			} else {
				return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Running)_12"), thread.getName()); //$NON-NLS-1$
			}
		}
		IBreakpoint[] breakpoints= thread.getBreakpoints();
		if (breakpoints.length > 0) {
			IJavaBreakpoint breakpoint= (IJavaBreakpoint)breakpoints[0];
			for (int i= 0, numBreakpoints= breakpoints.length; i < numBreakpoints; i++) {
				if (BreakpointUtils.isProblemBreakpoint(breakpoints[i])) {
					// If a compilation error breakpoint exists, display it instead of the first breakpoint
					breakpoint= (IJavaBreakpoint)breakpoints[i];
					break;
				}
			}
			String typeName= getMarkerTypeName(breakpoint, qualified);
			if (breakpoint instanceof IJavaExceptionBreakpoint) {
				String exName = ((IJavaExceptionBreakpoint)breakpoint).getExceptionTypeName();
				if (exName == null) {
					exName = typeName;
				} else if (!qualified) {
					int index = exName.lastIndexOf('.');
					exName = exName.substring(index + 1);
				} 
				if (thread.isSystemThread()) {
					return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(exception_{1}))_13"), new String[] {thread.getName(), exName}); //$NON-NLS-1$
				} else {
					return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(exception_{1}))_14"), new String[] {thread.getName(), exName}); //$NON-NLS-1$
				}
			}
			if (breakpoint instanceof IJavaWatchpoint) {
				IJavaWatchpoint wp = (IJavaWatchpoint)breakpoint;
				String fieldName = wp.getFieldName(); //$NON-NLS-1$
				if (wp.isAccessSuspend(thread.getDebugTarget())) {
					if (thread.isSystemThread()) {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(access_of_field_{1}_in_{2}))_16"), new String[] {thread.getName(), fieldName, typeName}); //$NON-NLS-1$
					} else {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(access_of_field_{1}_in_{2}))_17"), new String[] {thread.getName(), fieldName, typeName}); //$NON-NLS-1$
					}
				} else {
					// modification
					if (thread.isSystemThread()) {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(modification_of_field_{1}_in_{2}))_18"), new String[] {thread.getName(), fieldName, typeName}); //$NON-NLS-1$
					} else {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(modification_of_field_{1}_in_{2}))_19"), new String[] {thread.getName(), fieldName, typeName}); //$NON-NLS-1$
					}
				}
			}
			if (breakpoint instanceof IJavaMethodBreakpoint) {
				IJavaMethodBreakpoint me= (IJavaMethodBreakpoint)breakpoint;
				String methodName= me.getMethodName();
				if (me.isEntrySuspend(thread.getDebugTarget())) {
					if (thread.isSystemThread()) {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(entry_into_method_{1}_in_{2}))_21"), new String[] {thread.getName(), methodName, typeName}); //$NON-NLS-1$
					} else {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(entry_into_method_{1}_in_{2}))_22"), new String[] {thread.getName(), methodName, typeName}); //$NON-NLS-1$
					}
				} else {
					if (thread.isSystemThread()) {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(exit_of_method_{1}_in_{2}))_21"), new String[] {thread.getName(), methodName, typeName}); //$NON-NLS-1$
					} else {
						return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(exit_of_method_{1}_in_{2}))_22"), new String[] {thread.getName(), methodName, typeName}); //$NON-NLS-1$
					}					
				}
			}
			if (breakpoint instanceof IJavaLineBreakpoint) {
				IJavaLineBreakpoint jlbp = (IJavaLineBreakpoint)breakpoint;
				int lineNumber= jlbp.getLineNumber();
				if (lineNumber > -1) {
					// map java line number to str line number
					
					// get cause of launch
					ISourceLocator locator = thread.getLaunch().getSourceLocator();
					Object o = locator.getSourceElement(thread.getTopStackFrame());

					// only handle if from .java
					if (o instanceof ICompilationUnit) {
						ICompilationUnit unit = (ICompilationUnit) o;
						IFile javaFile = (IFile) unit.getResource();
						lineNumber = StreamItDebugEventSetListener.getLineNumber(javaFile, lineNumber);

						if (thread.isSystemThread()) {
							if (BreakpointUtils.isRunToLineBreakpoint(jlbp)) {
								return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(run_to_line_{1}_in_{2}))_23"), new String[] {thread.getName(), String.valueOf(lineNumber), typeName}); //$NON-NLS-1$
							} else {
								return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended_(breakpoint_at_line_{1}_in_{2}))_24"), new String[] {thread.getName(), String.valueOf(lineNumber), typeName}); //$NON-NLS-1$
							}
						} else {
							if (BreakpointUtils.isRunToLineBreakpoint(jlbp)) {
								return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(run_to_line_{1}_in_{2}))_25"), new String[] {thread.getName(), String.valueOf(lineNumber), typeName}); //$NON-NLS-1$
							} else {
								return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended_(breakpoint_at_line_{1}_in_{2}))_26"), new String[] {thread.getName(), String.valueOf(lineNumber), typeName}); //$NON-NLS-1$
							}
						}
					}
				}
			}
		}

		// Otherwise, it's just suspended
		if (thread.isSystemThread()) {
			return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.System_Thread_[{0}]_(Suspended)_27"), thread.getName()); //$NON-NLS-1$
		} else {
			return getFormattedString(DebugUIMessages.getString("JDIModelPresentation.Thread_[{0}]_(Suspended)_28"), thread.getName()); //$NON-NLS-1$
		}
	}
	
	protected String getStackFrameText(IStackFrame stackFrame) throws DebugException {
		IJavaStackFrame frame= (IJavaStackFrame) stackFrame.getAdapter(IJavaStackFrame.class);
		if (frame != null) {
			StringBuffer label= new StringBuffer();
			
			String dec= DebugUIMessages.getString("JDIModelPresentation<unknown_declaring_type>_4"); //$NON-NLS-1$
			try {
				dec= frame.getDeclaringTypeName();
			} catch (DebugException exception) {
			}
			if (frame.isObsolete()) {
				label.append(DebugUIMessages.getString("JDIModelPresentation.<obsolete_method_in__1")); //$NON-NLS-1$
				label.append(dec);
				label.append('>');
				return label.toString();
			}

			// receiver name
			String rec= DebugUIMessages.getString("JDIModelPresentation<unknown_receiving_type>_5"); //$NON-NLS-1$
			try {
				rec= frame.getReceivingTypeName();
			} catch (DebugException exception) {
			}
			label.append(getQualifiedName(rec));

			// append declaring type name if different
			if (!dec.equals(rec)) {
				label.append('(');
				label.append(getQualifiedName(dec));
				label.append(')');
			}

			// append a dot separator and method name
			label.append('.');
			try {
				// only allow init, work, main
				String name = frame.getMethodName();
				if (name.equals(IStreamItDebuggerConstants.INIT_METHOD) ||
					name.equals(IStreamItDebuggerConstants.WORK_METHOD) ||
					name.equals(IStreamItDebuggerConstants.PREWORK_METHOD)) {
					label.append(name);
				} else {
					label.deleteCharAt(label.length() - 1);
				}
			} catch (DebugException exception) {
				label.append(DebugUIMessages.getString("JDIModelPresentation<unknown_method_name>_6")); //$NON-NLS-1$
			}

			label.append("()"); //$NON-NLS-1$

			try {
				int lineNumber= frame.getLineNumber();
				label.append(' ');
				label.append(DebugUIMessages.getString("JDIModelPresentation.line__76")); //$NON-NLS-1$
				label.append(' ');
				if (lineNumber >= 0) {
					// map java line number to str line number
					
					// get cause of launch
					ISourceLocator locator = frame.getLaunch().getSourceLocator();
					Object o = locator.getSourceElement(frame);

					// only handle if from .java
					if (!(o instanceof ICompilationUnit)) {
						label.append(DebugUIMessages.getString("JDIModelPresentation.not_available")); //$NON-NLS-1$
						if (frame.isNative()) {
							label.append(' ');
							label.append(DebugUIMessages.getString("JDIModelPresentation.native_method")); //$NON-NLS-1$
						}
					} else {
						ICompilationUnit unit = (ICompilationUnit) o;
						IFile javaFile = (IFile) unit.getResource();
						lineNumber = StreamItDebugEventSetListener.getLineNumber(javaFile, lineNumber);
						if (lineNumber > 0) label.append(lineNumber);
						else {
							int end = label.length() - 1;
							int start = end - 1 - DebugUIMessages.getString("JDIModelPresentation.line__76").length();
							label.delete(start, end);
						}
					}
				} else {
					label.append(DebugUIMessages.getString("JDIModelPresentation.not_available")); //$NON-NLS-1$
					if (frame.isNative()) {
						label.append(' ');
						label.append(DebugUIMessages.getString("JDIModelPresentation.native_method")); //$NON-NLS-1$
					}
				}
			} catch (DebugException exception) {
				label.append(DebugUIMessages.getString("JDIModelPresentation_<unknown_line_number>_8")); //$NON-NLS-1$
			}
			
			if (!frame.wereLocalsAvailable()) {
				label.append(' ');
				label.append(DebugUIMessages.getString("JDIModelPresentation.local_variables_unavailable")); //$NON-NLS-1$
			}
			
			return label.toString();

		}
		return null;
	}
}