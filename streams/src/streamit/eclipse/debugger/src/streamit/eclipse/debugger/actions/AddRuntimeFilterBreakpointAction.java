package streamit.eclipse.debugger.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageRegistry;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.core.LaunchData;
import streamit.eclipse.debugger.core.StreamItDebugEventFilter;
import streamit.eclipse.debugger.graph.FilterWidget;

/**
 * @author kkuo
 */
public class AddRuntimeFilterBreakpointAction extends Action {

	private String fFilterName, fFilterRuntimeId, fFilterStaticId;
	private LaunchData fLaunchData;
		
	/**
	 * 
	 */
	public AddRuntimeFilterBreakpointAction() {
		super(ActionMessages.getString(IStreamItActionConstants.FILTER_BREAKPOINT_ADD_BREAKPOINT)); //$NON-NLS-1$
		setDescription(ActionMessages.getString(IStreamItActionConstants.FILTER_BREAKPOINT_TOOL_TIP_TEXT)); //$NON-NLS-1$
		
		ImageRegistry ir = StreamItDebuggerPlugin.getDefault().getImageRegistry();
		setImageDescriptor(ir.getDescriptor(IStreamItDebuggerPluginConstants.BREAKPOINT_IMAGE));
	}
	
	/**
	 * Updates the enabled state of this action based
	 * on the selection
	 */
	public void update(FilterWidget f, LaunchData data) {
		setEnabled(true);
		fFilterName = f.getNameWithoutId();
		fFilterRuntimeId = f.getRuntimeId();
		fFilterStaticId = f.getStaticId();
		fLaunchData = data;

		boolean hasMethodBreakpoint = fLaunchData.hasFilterInstanceBreakpoint(fFilterName, fFilterRuntimeId, fFilterStaticId);
		if (hasMethodBreakpoint) setText(ActionMessages.getString(IStreamItActionConstants.FILTER_BREAKPOINT_REMOVE_BREAKPOINT));
		else setText(ActionMessages.getString(IStreamItActionConstants.FILTER_BREAKPOINT_ADD_BREAKPOINT));
	}

	public void disable() {
		fFilterName = null;
		setEnabled(false);
		setText(ActionMessages.getString(IStreamItActionConstants.FILTER_BREAKPOINT_ADD_BREAKPOINT));
	}
	
	/**
	 * @see IAction#run()
	 */
	public void run() {
		if (fFilterName == null) return;
		try {
			StreamItDebugEventFilter.getInstance().toggleFilterWorkMethodBreakpoint(fLaunchData, fFilterName, fFilterRuntimeId, fFilterStaticId);
		} catch (CoreException e) {
		}
	}	
}