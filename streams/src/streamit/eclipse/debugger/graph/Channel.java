package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.swt.graphics.Font;

/**
 * @author kkuo
 */
public class Channel extends Label {

	private Vector fQueue;

	/**
	 * 
	 */
	public Channel(IVariable[] vars, Font parentFont, boolean input, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// channel is void
		if (vars.length == 0) {
			setVisible(false);
			Dimension d = FigureUtilities.getTextExtents("", parentFont);
			d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
			d.expand(0, IStreamItGraphConstants.MARGIN);
			factoryInst.roundUpEven(d);
			setSize(d);
			return;
		}
		
		// contents
		vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(vars, "queue"), "header"), "next");
		fQueue = factoryInst.getLinkedListElements(vars);
		IJavaValue val;
		String text = "";
		int size = fQueue.size();
		for (int i = 0; i < size; i++) {
			text += factoryInst.getValueString(((IVariable) fQueue.get(i)).getValue().getVariables(), "value");
			if (i < size - 1) text += "\n";
		}
		
		if (size < 2) {
			if (input) text = "\n\n" + text;
			else text += "\n\n";
		} else if (size == 2) {
			if (input) text = "\n" + text;
			else text += "\n";
		}
				
		// style
		setText(text);
		setBorder(new LineBorder());
		
		// size
		Dimension d = FigureUtilities.getTextExtents("\n\n", parentFont);
		d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
		d.expand(0, IStreamItGraphConstants.MARGIN);
		factoryInst.roundUpEven(d);
		setSize(d);
		
		// selector
		new ChannelSelector(this); 
	}
	
	public void update() {
		//fHeader.setText(fVariable.getValue().getValueString());
	}
	
	public IVariable getVariable() {
		return (IVariable) fQueue.get(0);
	}
}
