package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * @author kkuo
 */
public class StreamItViewFactory {

	private static StreamItViewFactory fInstance = new StreamItViewFactory();
	
	private Image fPlus;
	private Image fMinus;
	private Image fUpArrow;
	private Image fDownArrow;
	private int fIconWidth;
	private int fIconHeight;
	private int fArrowHeight;
	private int fArrowWidth;
	private Font fParentFont;
		
	/**
	 * Creates a new StreamItModelFactory.
	 */
	private StreamItViewFactory() {
	}
	
	/**
	 * Returns the singleton StreamItModelFactory.
	 */
	public static StreamItViewFactory getInstance() {
		return fInstance;
	}
	
	public Figure makeStream(IVariable topLevelPipeline, String streamNameWithId, OptionData optionData, boolean expandAll) {
		try {
			optionData.setExpandAll(expandAll);
			Figure f = new MainPipeline(topLevelPipeline, streamNameWithId, optionData, getInstance());
			optionData.setExpandAll(false);
			return f;			
		} catch (DebugException e) {
		}
		return null;
	}
	
	public void setUtilities(Image plus, Image minus, Image upArrow, Image downArrow, Font parentFont) {
		fPlus = plus;
		fMinus = minus;
		fUpArrow = upArrow;
		fDownArrow = downArrow;
		fParentFont = parentFont;
		fIconWidth = Math.max(fPlus.getBounds().width, fMinus.getBounds().width);
		fIconHeight = Math.max(fPlus.getBounds().height, fMinus.getBounds().height);
		fArrowHeight = Math.max(fUpArrow.getBounds().height, fDownArrow.getBounds().height);  
		fArrowWidth = Math.max(fUpArrow.getBounds().width, fDownArrow.getBounds().width); 
		if (fArrowHeight % 2 != 0) fArrowHeight++; 
		if (fArrowWidth % 2 != 0) fArrowWidth++;
	}
	
	public Image getMinus() {
		return fMinus;
	}
	
	public Image getPlus() {
		return fPlus;
	}
	
	public Image getArrow(boolean forward) {
		if (forward) return fUpArrow;
		else return fDownArrow;
	}
	
	public int getImageWidth() {
		return fIconWidth;
	}
	
	public int getImageHeight() {
		return fIconHeight;
	}
	
	public int getArrowHeight() {
		return fArrowHeight;
	}
	
	public int getArrowWidth() {
		return fArrowWidth;
	}
	
	public Font getFont() {
		return fParentFont;
	}
		
	protected IVariable getVariable(IVariable[] vars, String name) throws DebugException {
		for (int i = 0; i < vars.length; i++) if (vars[i].getName().equals(name)) return vars[i];
		return null;
	}
	
	protected IVariable[] findVariables(IVariable[] vars, String name) throws DebugException {
		return getVariable(vars, name).getValue().getVariables();
	}
	
	protected String getValueString(IVariable[] vars, String name) throws DebugException {
		return getVariable(vars, name).getValue().getValueString();
	}
		
	protected Vector getLinkedListElements(IVariable[] vars) throws DebugException {
		Vector v = new Vector();
		getLinkedListElementsHelper(vars, v);
		return v;
	}
	
	protected void getLinkedListElementsHelper(IVariable[] vars, Vector v) throws DebugException {
		IVariable element = null;
		IValue next = null;
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals("element")) element = vars[i];
			else if (vars[i].getName().equals("next")) next = vars[i].getValue();
		}
		
		if (!element.getValue().getValueString().equals("null")) {
			v.add(element);
			getLinkedListElementsHelper(next.getVariables(), v);
		}
	}
	
	protected void roundUpEven(Dimension d) {
		if (d.height % 2 != 0) d.height++; 
		if (d.width % 2 != 0) d.width++;
	}
}