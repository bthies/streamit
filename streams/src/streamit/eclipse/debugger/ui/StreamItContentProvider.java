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
package streamit.eclipse.debugger.ui;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class StreamItContentProvider implements IStructuredContentProvider {


	public Object[] getElements(Object inputElement) {
		return null;
	}
	
	public void dispose() {
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
/*
	private TableViewer viewer;
	public static StrFilter[] filters;
		
	public StreamItContentProvider(TableViewer v) {
		viewer = v;
		filters = new StrFilter[1];
		String[] test = new String[2];
		test[0] = "test11";
		test[1] = "test12";
		filters[0] = new StrFilter("test1", test);
		filters[0].setInputOutputType(true, "test2");
		filters[0].setInputOutputType(false, "test3");
		filters[0].setPeekCount("test4");
		filters[0].setPopCount("test5");
		filters[0].setPushCount("test6");
		filters[0].setStage("test7");
		filters[0].worked();
	}
	
	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	public Object[] getElements(Object inputElement) {
		try {
			//IMarker[] markers = getMarkers();
			viewer.getControl().getDisplay().syncExec(new Runnable() {		
				public void run() {
					viewer.refresh();
				}
			});
			return filters;
		} catch (Exception e) {
			return new StrFilter[0];
		}
	}
	*/
}