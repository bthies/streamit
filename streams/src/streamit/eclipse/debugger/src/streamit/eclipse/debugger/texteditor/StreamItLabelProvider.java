package streamit.eclipse.debugger.texteditor;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public class StreamItLabelProvider extends LabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof StreamSegment) {
			ImageRegistry reg = StreamItDebuggerPlugin.getDefault().getImageRegistry();
			int type = ((StreamSegment) element).getType();
			switch(type) {
				case StreamSegment.FIELD_TYPE: return reg.get(IStreamItDebuggerPluginConstants.FIELD_IMAGE);
				case StreamSegment.METHOD_TYPE:	return reg.get(IStreamItDebuggerPluginConstants.METHOD_IMAGE);			
				case StreamSegment.FILTER_TYPE: return reg.get(IStreamItDebuggerPluginConstants.FILTER_IMAGE);
				case StreamSegment.PIPELINE_TYPE: return reg.get(IStreamItDebuggerPluginConstants.PIPELINE_IMAGE);
				case StreamSegment.SPLITJOIN_TYPE: return reg.get(IStreamItDebuggerPluginConstants.SPLITJOIN_IMAGE);
				case StreamSegment.FEEDBACKLOOP_TYPE: return reg.get(IStreamItDebuggerPluginConstants.FEEDBACKLOOP_IMAGE);
			}
		}
		return null;
	}
}