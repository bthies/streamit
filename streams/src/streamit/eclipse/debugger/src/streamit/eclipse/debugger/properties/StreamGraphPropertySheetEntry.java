package streamit.eclipse.debugger.properties;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySheetEntry;

import streamit.eclipse.debugger.model.Pipeline;

/**
 * @author kkuo
 */
public class StreamGraphPropertySheetEntry extends LeafPropertySheetEntry {
	private IPropertySheetEntry[] fChildEntry;

	public StreamGraphPropertySheetEntry(String name, String value, Pipeline topLevelPipeline) {
		super(name, value);
		fChildEntry = new IPropertySheetEntry[1];
		fChildEntry[0] = new PipelinePropertySheetEntry(topLevelPipeline);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#hasChildEntries()
	 */
	public boolean hasChildEntries() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getChildEntries()
	 */
	public IPropertySheetEntry[] getChildEntries() {
		return fChildEntry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getCategory()
	 */
	public String getCategory() {
		return null;
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