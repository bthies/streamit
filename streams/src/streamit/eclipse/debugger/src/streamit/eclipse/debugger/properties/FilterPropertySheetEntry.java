package streamit.eclipse.debugger.properties;

import org.eclipse.swt.graphics.Image;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;
import streamit.eclipse.debugger.model.Filter;


/**
 * @author kkuo
 */
public class FilterPropertySheetEntry extends StreamStructurePropertySheetEntry {

	public FilterPropertySheetEntry(Filter f) {
		super(f);
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.STRUCTURE_TYPE, IStreamItGraphConstants.FILTER_STRUCTURE));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.INIT_EXECUTION_COUNT, f.getInitExecutionCount()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.STEADY_EXECUTION_COUNT, f.getSteadyExecutionCount()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.WORK_EXECUTIONS, f.getWorkExecutions()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getCategory()
	 */
	public String getCategory() {
		return IStreamItGraphConstants.FILTER_STRUCTURE;
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
