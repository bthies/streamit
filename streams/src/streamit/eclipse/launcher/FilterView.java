/*******************************************************************************
 * StreamIt Launcher adapted from 
 * org.eclipse.ui.examples.readmetool.ReadmeSectionsView
 * @author kkuo
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
package launcher;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.SWT;

/**
 * This class demonstrates a simple view containing a single viewer.
 */
public class FilterView extends ViewPart {
	
	TableViewer viewer;

	/**
	 * Creates a new FilterView.
	 */
	public FilterView() {
		super();
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbenchPart
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent);
		viewer.setContentProvider(new StreamItContentProvider());
		viewer.setLabelProvider(new StreamItLabelProvider());
		
		// make lines visible
		Table t = viewer.getTable();
		t.setLinesVisible(true);
		t.setHeaderVisible(true);

		TableColumn[] tcs = new TableColumn[StreamItLauncherPlugin.FILTER_VIEW_HEADERS.length];
		for (int i = 0; i < tcs.length; i++) {
			tcs[i] = new TableColumn(t, SWT.LEFT); 
			tcs[i].setWidth(100);
			tcs[i].setText(StreamItLauncherPlugin.FILTER_VIEW_HEADERS[i]);
		}
	}
	
	protected Table getTable() {
		return viewer.getTable();
	}
	
	public void dispose() {
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbenchPart
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}