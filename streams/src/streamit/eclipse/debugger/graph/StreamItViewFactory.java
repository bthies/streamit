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
	
	public Figure makeStream(IVariable topLevelPipeline, Font parentFont, String streamNameWithId, Expanded allExpandedChildren) {
		try {
			return new MainPipeline(topLevelPipeline, parentFont, streamNameWithId, allExpandedChildren, getInstance());			
		} catch (DebugException e) {
			System.out.println("makeStream error:  " + e.toString());
		}
		return null;
	}
	
	public void setImages(Image plus, Image minus) {
		fPlus = plus;
		fMinus = minus;
	}
	
	public Image getMinus() {
		return fMinus;
	}
	
	public Image getPlus() {
		return fPlus;
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