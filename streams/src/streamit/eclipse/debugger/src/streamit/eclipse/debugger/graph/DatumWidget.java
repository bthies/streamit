package streamit.eclipse.debugger.graph;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.model.Channel;
import streamit.eclipse.debugger.model.Datum;

/**
 * @author kkuo
 */
public class DatumWidget extends Label {
	private Datum fModel;
	private boolean fDummy;
	private boolean fHighlighted;

	public DatumWidget(Datum model, boolean dummy, OptionData optionData, StreamItViewFactory factoryInst) {
		super();
		fModel = model;
		fDummy = dummy;
		
		// style
		setOpaque(true);

		if (fModel != null && optionData.isHighlighted(fModel.getVariable())) {
			// highlight
			setForegroundColor(ColorConstants.menuBackgroundSelected);
			setBackgroundColor(ColorConstants.menuForegroundSelected);
			fHighlighted = true;
		} else {
			setForegroundColor(ColorConstants.menuForeground);
			setBackgroundColor(ColorConstants.white);
			fHighlighted = false;
		}


		// set text
		String text = IStreamItGraphConstants.EMPTY_STRING;
		if (fModel != null) text = fModel.getText(); 
		setText(text);
		
		// size
		Dimension d = FigureUtilities.getTextExtents(text, factoryInst.getFont());
		d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
		d.expand(0, IStreamItGraphConstants.MARGIN);
		setSize(d);
	}
	
	public int setLocationFromBottom(Point parentBottomLeft, int currentHeight) {
		int set = currentHeight - getSize().height;
		setLocation(parentBottomLeft.getTranslated(0, set));
		return set;
	}
	
	public int setLocationFromTop(Point parentTopLeft, int currentHeight) {
		setLocation(parentTopLeft.getTranslated(0, currentHeight));
		return currentHeight + getSize().height;
	}
	
	public boolean isDummy() {
		return fDummy;
	}
	
	public boolean isHighlighted() {
		return fHighlighted;
	}

	public void highlight(OptionData optionData) {
		optionData.toggleChannel(fModel.getVariable());
		
		// highlight
		setForegroundColor(ColorConstants.menuBackgroundSelected);
		setBackgroundColor(ColorConstants.menuForegroundSelected);
		fHighlighted = true;
	}
	
	public void unhighlight(OptionData optionData) {
		optionData.toggleChannel(fModel.getVariable());
		
		// unhighlight
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);
		fHighlighted = false;
	}
	
	public IVariable getVariable() {
		return fModel.getVariable();
	}
	
	public boolean verify(Channel model, String input) throws DebugException {
		return fModel.verify(model, input);	
	}
	
	public void update(Channel model, String input) throws DebugException {
		fModel.update(model, input);
		setText(fModel.getText());
	}
}
