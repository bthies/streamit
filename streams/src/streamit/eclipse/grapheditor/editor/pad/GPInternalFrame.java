package streamit.eclipse.grapheditor.editor.pad;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import javax.swing.JInternalFrame;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * The graphpad internal frame is a container
 * is a container for one GPDocument.
 * There is a split between a document and
 * the joint internal frame, sothat you can use
 * the document without using internal frames.
 *
 * @author sven.luzar
 *
 */
public class GPInternalFrame extends streamit.eclipse.grapheditor.editor.utils.gui.GPInternalFrame {

	/** A link to the Graphpad Document of this frame
	 */
	GPDocument document;

	/**
	 * Constructor for GPInternalFrame.
	 */
	public GPInternalFrame(GPDocument document) {
		super(document.getFrameTitle(), true, true, true, true);
		this.setFrameIcon(GPGraphpad.getApplicationIcon());
		this.document = document;
		this.document.setInternalFrame(this);
		this.getContentPane().add(document);
		this.addVetoableChangeListener(new GPVetoableListner(document));
		//this.setPreferredSize(new Dimension(600, 400));
		//this.pack();
	}

	/**
	 * Returns the document.
	 * @return GPDocument
	 */
	public GPDocument getDocument() {
		return document;
	}

	/**
	 * Sets the document.
	 * @param document The document to set
	 */
	public void setDocument(GPDocument document) {
		this.remove(this.document);
		this.document = document;
		this.add(this.document);
		//this.pack();
	}

}
class GPVetoableListner implements VetoableChangeListener {

	GPDocument document;

	GPVetoableListner(GPDocument doc) {
		this.document = doc;
	}
	/**
	 * @see javax.swing.event.InternalFrameListener#internalFrameClosing(InternalFrameEvent)
	 */
	public void vetoableChange(PropertyChangeEvent evt)
		throws PropertyVetoException {
		if (evt.getPropertyName() != JInternalFrame.IS_CLOSED_PROPERTY)
			return;

		if (((Boolean)evt.getNewValue()).booleanValue() && document.close(true)){
				document.getGraphpad().removeDocument(document);
		} else {
			throw new PropertyVetoException("Can't close the Internal Frame", evt) ;
		}
	}

}
