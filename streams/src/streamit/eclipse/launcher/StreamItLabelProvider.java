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
package streamit.eclipse.launcher;

import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.LabelProvider;

public class StreamItLabelProvider extends LabelProvider {

	public Image getColumnImage(Object element,	int columnIndex) {
		return null;
	}
	
	public String getColumnText(Object element, int columnIndex) {

		if (element instanceof StrFilter) {
			StrFilter f = (StrFilter) element;
			switch (columnIndex) {
				case 0: return "Filter Name";
				case 1: return "Input Type";
				case 2: return "Output Type";
				case 3: return "State Variables";
				case 4: return "Pop Count";
				case 5: return "Push Count";
				case 6: return "Peek Count";
			}
		}		
		return "";
	}
}