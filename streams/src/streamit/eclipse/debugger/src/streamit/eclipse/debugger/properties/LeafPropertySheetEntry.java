package streamit.eclipse.debugger.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetEntryListener;

/**
 * @author kkuo
 */
public class LeafPropertySheetEntry implements IPropertySheetEntry {
	
	private String fDisplayName;
	private String fValue;
	
	public LeafPropertySheetEntry(String name, String value) {
		fDisplayName = name;
		fValue = value;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getDisplayName()
	 */
	public String getDisplayName() {
		return fDisplayName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getValueAsString()
	 */
	public String getValueAsString() {
		return fValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#hasChildEntries()
	 */
	public boolean hasChildEntries() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#getChildEntries()
	 */
	public IPropertySheetEntry[] getChildEntries() {
		return null;
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