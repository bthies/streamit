/*
 * @(#)GPBarEntry.java	1.0 18.02.2003
 *
 * Copyright (C) 2003 luzar
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package streamit.eclipse.grapheditor.editor.pad;

/**An entry for inserts into the menubar, toolbars, graph popup
 * menu or library popup menu
 * 
 * @author luzar
 * @version 1.0
 */
public class GPBarEntry {

	/** The name of the bar key where to insert this 
	 *  bar entry.
	 */
	String barKey;
	
	/** The position where to insert this 
	 *  bar entry.
	 */
	int pos;
	
	
	/** The name of this bar value. Must be equal with
	 *  the Action name. 
	 */
	String barValue;

	/**
	 * Constructor for GPBarEntry.
	 */
	public GPBarEntry(String barKey, int pos, String barValue) {
		this.barKey = barKey;
		this.pos = pos;
		this.barValue = barValue;
	}


	/**
	 * Returns the barKey.
	 * @return String
	 */
	public String getBarKey() {
		return barKey;
	}

	/**
	 * Returns the barValue.
	 * @return String
	 */
	public String getBarValue() {
		return barValue;
	}

	/**
	 * Returns the pos.
	 * @return int
	 */
	public int getPos() {
		return pos;
	}

	/**
	 * Sets the barKey.
	 * @param barKey The barKey to set
	 */
	public void setBarKey(String barKey) {
		this.barKey = barKey;
	}

	/**
	 * Sets the barValue.
	 * @param barValue The barValue to set
	 */
	public void setBarValue(String barValue) {
		this.barValue = barValue;
	}

	/**
	 * Sets the pos.
	 * @param pos The pos to set
	 */
	public void setPos(int pos) {
		this.pos = pos;
	}

	/** Prints the Entry with all properties.
	 * 
	 */	
	public String toString(){
		StringBuffer b = new StringBuffer();
		b.append("GPBarEntry: barKey=");
		b.append(barKey);
		b.append("; pos=");
		b.append(pos);
		b.append("; barValue=");
		b.append(barValue);
		return b.toString() ;
	}

}
