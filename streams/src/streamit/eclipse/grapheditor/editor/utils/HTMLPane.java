/*
 * This program is free software; you can redistribute it and/or
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

package streamit.eclipse.grapheditor.editor.utils;

import java.awt.*;
import java.awt.event.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * This pane extends JEditorPane's functionality to add support for
 * HTML related display and behavior. Specifically, it handles the necessary
 * implementation to support URL tooltip support and launching of a platform
 * specific HTML/Internet browser.
 *
 * @author Van Woods
 */
public class HTMLPane extends JEditorPane {
	protected CustomLinkHandler linkHandler = new CustomLinkHandler(this);
	protected boolean toolTipOriginalEnabledStatus = true;

	/**
	 * Convenience constructor with no required parameters which automatically
	 * creates an uneditable editor instance.
	 */
	public HTMLPane() {
		this(false);
	}

	/**
	 * Constructor for the HTMLPane object
	 *
	 * @param editable  If <code>true</code>, this component supports HTML editing.
	 * This class only manages the tooltip and URL mouse events, not any
	 * graphical layout and editing capability. This class could be used, however,
	 * in building such a tool as it does have support for disabling URL tracking
	 * during editing (ie no launch of browser or tooltip display).
	 */
	public HTMLPane(boolean editable) {
		this.setEditable(editable);

		// make sure type is html
		this.setContentType("text/html");

		// register listener responsible for launching browser
		this.addHyperlinkListener(linkHandler);

		// initiliaze tooltip
		toolTipOriginalEnabledStatus = ToolTipManager.sharedInstance().isEnabled();
		ToolTipManager.sharedInstance().registerComponent(this);
	}

	/**
	 * Override tool tip method to display URL
	 *
	 * @param event  event passed
	 * @return       tooltip as URL
	 */
	public String getToolTipText(MouseEvent event) {
		if (linkHandler.isHoveringOverHyperlink() && (linkHandler.getHoveredURL() != null)) {
			// have to manually toggle tooltip enabled status to prevent empty
			// tooltip from appearing when not hovering over url
			ToolTipManager.sharedInstance().setEnabled(true);
			return linkHandler.getHoveredURL();
		}
		else {
			ToolTipManager.sharedInstance().setEnabled(false);
			return null;
		}
	}

	/**
	 * Override Swing's poor label position choice. The new behaviour
	 * shows the label relative to the current location of the mouse.
	 *
	 * @param event  tool tip location event
	 * @return       tool tip location
	 */
	public Point getToolTipLocation(MouseEvent event) {
		return new Point(event.getX() + 10, event.getY() + 25);
	}

	/**
	 * Determines if current mouse location is hovering over a hyperlink.
	 * Remember, <code>CustomLinkHandler</code> is NOT notified of hyperlink
	 * events if editing is enabled by defintion in JEditorPane. In otherwords,
	 * when HTML code is being displayed, then hyperlink tracking is not occuring.
	 *
	 * @return   <code>true</code> if mouse if hovering over hyperlink and pane
	 * is not editable
	 */
	public boolean isHoveringOverHyperlink() {
		return linkHandler.isHoveringOverHyperlink();
	}

	/**
	 * Gets the URL being hovered over.
	 *
	 * @return   The URL value if mouse is currently hovering over a URL, or
	 * <code>null</code> if not currently hovering over a URL
	 */
	public String getHoveredURL() {
		return linkHandler.getHoveredURL();
	}


// *****************************************************************************

	/**
	 * Handles URL hyperlink events and provides status information.
	 *
	 * @author    Van Woods
	 */
	protected class CustomLinkHandler implements HyperlinkListener {
		protected JEditorPane pane = null;
		protected boolean isHovering = false;
		protected String hoveredURLString = null;

		/**
		 * Constructor for the CustomLinkHandler object
		 *
		 * @param inpane  Description of Parameter
		 */
		public CustomLinkHandler(JEditorPane inpane) {
			this.pane = inpane;
		}

		/**
		 * Prevent class from being instantiated without required parameter.
		 */
		private CustomLinkHandler() {
		}

		/**
		 * Determines if current mouse location is hovering over a hyperlink.
		 * Remember, <code>CustomLinkHandler</code> is NOT notified of hyperlink
		 * events if editing is enabled by defintion in JEditorPane. In otherwords,
		 * when HTML code is being displayed, then hyperlink tracking is not occuring.
		 *
		 * @return   true if mouse if hovering over hyperlink and pane is not editable
		 */
		public boolean isHoveringOverHyperlink() {
			// check if pane is editable as caller could have changed editability after
			// hyperlinkUpdate was fired causing indeterminability in hovering status
			if (pane.isEditable()) {
				return false;
			}
			else {
				return isHovering;
			}
		}

		/**
		 * Gets the URL being hovered over.
		 *
		 * @return   The URL value if mouse is currently hovering over a URL, or
		 * <code>null</code> if not currently hovering over a URL
		 */
		public String getHoveredURL() {
			return hoveredURLString;
		}

		/**
		 * Launch browser if hyperlink is clicked by user. Will go to existing opened
		 * browser if one exists. If not clicked, then store url pointed to.
		 *
		 * @param e  event passed by source
		 */
		public void hyperlinkUpdate(HyperlinkEvent e) {
			// track mouse enters and exits
			if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				isHovering = true;
				URL url = e.getURL();
				if (url != null) {
					hoveredURLString = url.toExternalForm();
				}
				else {
					// error case
					hoveredURLString = null;
				}
				//System.out.println("hyperlinkUpdate fired");
				//System.out.println("     entered->");
			}
			else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
				isHovering = false;
				hoveredURLString = null;
				//System.out.println("     <-exited");
			}

			// launch native browser if URL is clicked
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				try {
					BrowserLauncher.openURL(e.getURL());
				}
				catch (Exception ex) {
				    //Utilities.errorMessage(Translator.getString("Error.DealingWithBrowser"/*#Finished:Original="Error dealing with browser."*/), ex);
					System.out.println("Error dealing with browser."/*#Frozen*/);
					ex.printStackTrace();
				}
			}
		}
	}
}


