package streamit.eclipse.debugger.properties;

import java.util.Vector;

import org.eclipse.swt.graphics.Image;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;
import streamit.eclipse.debugger.model.FeedbackLoop;
import streamit.eclipse.debugger.model.Filter;
import streamit.eclipse.debugger.model.Pipeline;
import streamit.eclipse.debugger.model.SplitJoin;

/**
 * @author kkuo
 */
public class FeedbackLoopPropertySheetEntry extends StreamStructurePropertySheetEntry {
	
	public FeedbackLoopPropertySheetEntry(FeedbackLoop f) {
		super(f);

		Vector children = f.getChildStreams();
		Object child;
		for (int i = 0; i < children.size(); i++) {
			child = children.get(i);
			if (child instanceof Filter) {
				fChildEntries.add(new FilterPropertySheetEntry((Filter) child));
			} else if (child instanceof Pipeline) {
				fChildEntries.add(new PipelinePropertySheetEntry((Pipeline) child));
			} else if (child instanceof SplitJoin) {
				fChildEntries.add(new SplitJoinPropertySheetEntry((SplitJoin) child));
			} else if (child instanceof FeedbackLoop) {
				fChildEntries.add(new FeedbackLoopPropertySheetEntry((FeedbackLoop) child));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getCategory()
	 */
	public String getCategory() {
		return IStreamItGraphConstants.FEEDBACKLOOP_STRUCTURE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getDescription()
	 */
	public String getDescription() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getImage()
	 */
	public Image getImage() {
		return null;
	}
}