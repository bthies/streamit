/*
 * @(#)GPTransferable.java	1.2 11/11/02
 *
 * Copyright (C) 2001 Gaudenz Alder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Rectangle;
import java.util.Map;

import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.GraphTransferable;
import org.jgraph.graph.ParentMap;

public class GPTransferable extends GraphTransferable {

	protected String text;

	public GPTransferable(
		String text,
		Object[] cells,
		Map viewAttributes,
		Rectangle bounds,
		ConnectionSet cs,
		ParentMap pm) {
		super(cells, viewAttributes, bounds, cs, pm);
		this.text = text;
	}

	public boolean isPlainSupported() {
		return (text != null);
	}

	public String getPlainData() {
		return text;
	}

}
