package streamit.eclipse.debugger.properties;

import java.util.Vector;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetEntryListener;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;
import streamit.eclipse.debugger.model.StreamStructure;

/**
 * @author kkuo
 */
public class StreamStructurePropertySheetEntry implements IPropertySheetEntry {

	protected Vector fChildEntries; // vector of IPropertySheetEntry
	private String fNameWithId;

	public StreamStructurePropertySheetEntry(StreamStructure s) {
		fNameWithId = s.getNameWithRuntimeId();
		fChildEntries = new Vector();
		
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.INPUT_TYPE, s.getInputType()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.OUTPUT_TYPE, s.getOutputType()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.PUSH_RATE, s.getPushRate()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.POP_RATE, s.getPopRate()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.PEEK_RATE, s.getPeekRate()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.POPPED, s.getPopped()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.PEEKED, s.getMaxPeeked()));
		fChildEntries.add(new LeafPropertySheetEntry(IStreamItGraphConstants.PUSHED, s.getPushed()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getDisplayName()
	 */
	public String getDisplayName() {
		return fNameWithId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getValueAsString()
	 */
	public String getValueAsString() {
		return IStreamItGraphConstants.EMPTY_STRING;
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
		IPropertySheetEntry[] entries = new IPropertySheetEntry[fChildEntries.size()];
		
		for (int i = 0; i < entries.length; i++) {
			entries[i] = (IPropertySheetEntry) fChildEntries.get(i);
		}
		
		return entries;
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#addPropertySheetEntryListener(org.eclipse.ui.views.properties.IPropertySheetEntryListener)
	 */
	public void addPropertySheetEntryListener(IPropertySheetEntryListener listener) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#applyEditorValue()
	 */
	public void applyEditorValue() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getEditor(org.eclipse.swt.widgets.Composite)
	 */
	public CellEditor getEditor(Composite parent) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getErrorText()
	 */
	public String getErrorText() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getFilters()
	 */
	public String[] getFilters() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getHelpContextIds()
	 */
	public Object getHelpContextIds() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#removePropertySheetEntryListener(org.eclipse.ui.views.properties.IPropertySheetEntryListener)
	 */
	public void removePropertySheetEntryListener(IPropertySheetEntryListener listener) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#resetPropertyValue()
	 */
	public void resetPropertyValue() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#setValues(java.lang.Object[])
	 */
	public void setValues(Object[] values) {
	}
}
