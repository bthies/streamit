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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class StreamItLabelProvider extends LabelProvider {
	public Image getColumnImage(Object element,	int columnIndex) {
		return null;
	}
	
	public String getColumnText(Object element, int columnIndex) {
		return "";
	}

/*
import org.eclipse.jface.viewers.ITableLabelProvider;

public class StreamItLabelProvider extends LabelProvider implements ITableLabelProvider {
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex >= 0 && columnIndex < StreamItDebuggerPlugin.FILTER_VIEW_HEADERS.length
			&& element instanceof StrFilter) {

			StrFilter f = (StrFilter) element;
			switch (columnIndex) {
				case 0: return f.getName();
				case 1: return f.getInputType();
				case 2: return f.getOutputType();
				case 3: return f.getFields();
				case 4: return f.getPopCount();
				case 5: return f.getPushCount();
				case 6: return f.getPeekCount();
				case 7: return f.getStage();
				case 8: return f.getNumWork();
			}
		}
		return "";
	}
	
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}
*/
}