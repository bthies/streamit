/*******************************************************************************
 * StreamIt Plugin adapted from Example Readme Tool
 * modifier - Kimberly Kuo
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package texteditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * This class implements a sample preference page that is 
 * added to the preference dialog based on the registration.
 */
public class StreamItPreferencePage extends PreferencePage 
    implements IWorkbenchPreferencePage {

    /* Private Members */
    private List lCategory;
    
	private List lMember;
    private Button pbDeleteMember;
    private Text tfAddMember;
    private Button pbAddMember;

	private Set fgKeywords;	
	private Set fgStrKeywords;
	private Set fgTypes;
	private Set fgConstants;
	private Set fgStrCommon;

	private SelectionListener createSelListener(int code) {
		SelectionListener sl;
		// category list
		switch (code) {
			case 0:
				sl = new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent event) {
						//Handle a default selection.
					}	
					public void widgetSelected(SelectionEvent event) {
						// make lMember reflect selection in lCategory
						int selected = lCategory.getSelectionIndex();
						lMember.removeAll();
						Iterator i;
						switch (selected) {
							case 0: i = fgKeywords.iterator(); break;
							case 1: i = fgStrKeywords.iterator(); break;
							case 2: i = fgTypes.iterator(); break;
							case 3: i = fgConstants.iterator(); break;
							default: i = fgStrCommon.iterator(); break;
						}
						while (i.hasNext())
							lMember.add((String) i.next());
						lMember.deselectAll();
						lMember.setSelection(0);
						lCategory.deselectAll();
						lCategory.setSelection(selected);
					}
				};
				break;
			// delete member
			case 1:
				sl = new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent event) {
						//Handle a default selection.
					}
					public void widgetSelected(SelectionEvent event) {
						String[] sel = lMember.getSelection();	
						lMember.remove(lMember.getSelectionIndices());
						lMember.deselectAll();
						lMember.setSelection(0);
						
						int selected = lCategory.getSelectionIndex();
						switch (selected) {
							case 0: 				
								for (int i = 0; i < sel.length; i++)
									fgKeywords.remove(sel[i]);
								break;
							case 1:
								for (int i = 0; i < sel.length; i++)
									fgStrKeywords.remove(sel[i]);
								break;
							case 2:
								for (int i = 0; i < sel.length; i++)
									fgTypes.remove(sel[i]);
								break;
							case 3:
								for (int i = 0; i < sel.length; i++)
									fgConstants.remove(sel[i]);
								break;
							default:							
								for (int i = 0; i < sel.length; i++)
									fgStrCommon.remove(sel[i]);
								break;
						}									
					}				
				};
				break;
			// add member
			default:
				sl = new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent event) {
						//Handle a default selection.
					}
					public void widgetSelected(SelectionEvent event) {
						String toAdd = tfAddMember.getText();
						// check text is valid
						if (toAdd.toLowerCase().charAt(0) < 'a' ||
							toAdd.toLowerCase().charAt(0) > 'z')
							return;
	
						Iterator i;
						int selected = lCategory.getSelectionIndex();
						boolean added = false;
						switch (selected) {
							case 0: 
								added = fgKeywords.add(toAdd);
								i = fgKeywords.iterator(); break;
							case 1:
								added = fgStrKeywords.add(toAdd);
								i = fgStrKeywords.iterator(); break;
							case 2:
								added = fgTypes.add(toAdd); 
								i = fgTypes.iterator(); break;
							case 3:
								added = fgConstants.add(toAdd); 
								i = fgConstants.iterator(); break;
							default:
								added = fgStrCommon.add(toAdd); 
								i = fgStrCommon.iterator(); break;						
						}
						if (!added) return;
						
						lMember.removeAll();
						while (i.hasNext())
							lMember.add((String) i.next());
						lMember.deselectAll();
						lMember.setSelection(0);
						
						tfAddMember.setText(IPreferenceStore.STRING_DEFAULT_DEFAULT);
					}
				};
				break;
		}
		return sl;
	}
    
    /**
     * Utility method that creates a push button instance
     * and sets the default layout data.
     *
     * @param parent  the parent for the new button
     * @param label  the label for the new button
     * @return the newly-created button
     */
    private Button createPushButton(Composite parent, String label,
    								SelectionListener sl) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.addSelectionListener(sl);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = 100;
		button.setLayoutData(data);
		return button;
    }

    /**
     * Create a text field specific for this application
     *
     * @param parent  the parent of the new text field
     * @return the new text field
     */
    private Text createTextField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = 75;
		text.setLayoutData(data);
		return text;
    }

	private List createList(Composite parent, int height, int width) {
		List list = new List(parent, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = height; 
		data.heightHint = width;
		list.setLayoutData(data);
		return list;
	}

    /**
     * Utility method that creates a label instance
     * and sets the default layout data.
     *
     * @param parent  the parent for the new label
     * @param text  the text for the new label
     * @return the new label
     */
    private Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
    }

    /**
     * Creates composite control and sets the default layout data.
     *
     * @param parent  the parent of the new composite
     * @param numColumns  the number of columns for the new composite
     * @return the newly-created composite
     */
    private Composite createComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NULL);
	
		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);
		
		//GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
    }

    /** (non-Javadoc)
     * Method declared on PreferencePage
     */
    protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(parent, 
				      IStreamItConstants.PREFERENCE_PAGE_CONTEXT);
	
		// Category 
		Composite cCat = createComposite(parent, 2);
	
		// cCatL << cCat
		Composite cCatL = createComposite(cCat, 1);
		createLabel(cCatL, StreamItEditorMessages.
									getString("Category(s)"));
		//$NON-NLS-1$
		lCategory = createList(cCatL, 150, SWT.DEFAULT);
		lCategory.addSelectionListener(createSelListener(0));
				
		// Member
		Composite cMem = createComposite(parent, 2);
	
		// cMemL << cMem
		Composite cMemL = createComposite(cMem, 1);
		createLabel(cMemL, StreamItEditorMessages.
							getString("Member(s)"));
		//$NON-NLS-1$
		lMember = createList(cMemL, 150, 150);
		
		// cMemR << cMem
		Composite cMemR = createComposite(cMem, 2);
		createComposite(cMemR, 1);
		pbDeleteMember = createPushButton(cMemR,
						  StreamItEditorMessages.
						  getString("Delete_Member(s)"), createSelListener(1));
		//$NON-NLS-1$
	
	
		// cAddMem << cMemR
		tfAddMember = createTextField(cMemR);
		pbAddMember = createPushButton(cMemR,
					       StreamItEditorMessages.
					       getString("Add_Member"), createSelListener(2));
		//$NON-NLS-1$
	
		initializeValues();
		//font = null;
		return new Composite(parent, SWT.NULL);
    }

    /** 
     * The <code>StreamItPreferencePage</code> implementation of this
     * <code>PreferencePage</code> method 
     * returns preference store that belongs to our plugin.
     * This is important because we want to store
     * our preferences separately from the desktop.
     */
    protected IPreferenceStore doGetPreferenceStore() {
		return StreamItPlugin.getDefault().getPreferenceStore();
    }
    
    /* (non-Javadoc)
     * Method declared on IWorkbenchPreferencePage
     */
    public void init(IWorkbench workbench){
    }

    /**
     * Initializes states of the controls from the preference store.
     */
    private void initializeValues() {
	    IPreferenceStore store = getPreferenceStore();
	
		// initial category list
		lCategory.setItems(IStreamItConstants.CATEGORY_DEFAULTS);
		lCategory.deselectAll();
		lCategory.setSelection(0);
		
		initializeDefaults();
    }
    
	/**
	 * Initializes states of the controls using default values
	 * in the preference store.
	 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
				
		// init all member lists
		StringTokenizer strtok;
		String temp;
		Set toAdd;
		String toUse;
		
		for (int i = 0; i < 5; i++) {
			toAdd = new TreeSet();
			switch (i) {
				case 0: toUse = IStreamItConstants.PRE_KEYWORD; break;
				case 1: toUse = IStreamItConstants.PRE_STR_KEYWORD; break;
				case 2: toUse = IStreamItConstants.PRE_TYPE; break;
				case 3: toUse = IStreamItConstants.PRE_CONSTANT; break;
				default: toUse = IStreamItConstants.PRE_STR_COMMON; break;
			}
			temp = store.getString(toUse);
			strtok = new StringTokenizer(temp, "\n");
			while (strtok.hasMoreTokens())
				toAdd.add(strtok.nextToken());
			switch (i) {
				case 0: fgKeywords = toAdd; break;
				case 1: fgStrKeywords = toAdd; break;
				case 2: fgTypes = toAdd; break;
				case 3: fgConstants = toAdd; break;
				default: fgStrCommon = toAdd; break;
			}	
		}
		
		// init member list to show
		Iterator i = fgKeywords.iterator();
		lMember.removeAll();
		while (i.hasNext())
			lMember.add((String) i.next());
		lMember.deselectAll();
		lMember.setSelection(0);
	
		tfAddMember.setText(IPreferenceStore.STRING_DEFAULT_DEFAULT);
	}

    /* (non-Javadoc)
     * Method declared on PreferencePage
     */
    protected void performDefaults() {
		super.performDefaults();
		StreamItPlugin.getDefault().initializeDefaultPreferences(getPreferenceStore());
		initializeDefaults();
    }

    /* (non-Javadoc)
     * Method declared on PreferencePage
     */
    public boolean performOk() {    	
		storeValues();
		StreamItPlugin.getDefault().savePluginPreferences();
		return true;
    }

	protected void performApply() {
		performOk();	
	}
	
    /**
     * Stores the values of the controls back to the preference store.
     */
    private void storeValues() {
		IPreferenceStore store = getPreferenceStore();
		StringBuffer temp;
		Iterator i;
		String toUse;
		
		for (int j = 0; j < 5; j++) {
			temp = new StringBuffer();
			switch (j) {
				case 0: 
					toUse = IStreamItConstants.PRE_KEYWORD;
					i = fgKeywords.iterator(); break;
				case 1:
					toUse = IStreamItConstants.PRE_STR_KEYWORD;
					i = fgStrKeywords.iterator(); break;
				case 2:
					toUse = IStreamItConstants.PRE_TYPE;
					i = fgTypes.iterator(); break;
				case 3: 
					toUse = IStreamItConstants.PRE_CONSTANT;
					i = fgConstants.iterator(); break;
				default: 
					toUse = IStreamItConstants.PRE_STR_COMMON;
					i = fgStrCommon.iterator(); break;
			}
			while (i.hasNext()) {
				temp.append(i.next());
				if (i.hasNext())
					temp.append("\n");
			}
			store.setDefault(toUse, temp.toString());
		}
    }
}