package streamit.eclipse.debugger.graph;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaValue;

/**
 * @author kkuo
 */
public class Channel extends Label {

	private Vector fQueue;
	private HashMap fHighlights; // key = IVariable datum, entry = Label highlight
	private boolean fTurnedOff; // used for expanded inputs of pipelines

	private boolean fInput;
	private boolean fForward;
	private boolean fVoid;
	private boolean fDraw;
	private boolean fExpanded;
	private boolean fArray;
	private boolean fComplex;
	
	private String fId;
	private ChannelToggle fIcon;

	public Channel(IVariable[] vars, String id, Figure parent, boolean input, boolean forward, boolean lastChild, OptionData optionData, StreamItViewFactory factoryInst) throws DebugException {
		super();
		fId = id;
		fInput = input;
		fForward = forward;
		
		// channel is void
		if (vars.length == 0) {
			fVoid = true;
			fDraw = true;
			fExpanded = false;
			parent.add(this);
			setVisible(false);
			Dimension d = FigureUtilities.getTextExtents("", factoryInst.getFont());
			d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
			d.expand(0, IStreamItGraphConstants.MARGIN);
			factoryInst.roundUpEven(d);
			setSize(d);
			return;
		}
		fVoid = false;
		fDraw = fInput || lastChild;
		fExpanded = optionData.containsChannel(id);
		fArray = false;
		fComplex = false;
		
		// contents
		vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(vars, "queue"), "header"), "next");
		Vector v = factoryInst.getLinkedListElements(vars);
		fQueue = new Vector();
		fHighlights = new HashMap();
		IVariable var;
		IValue val;
		for (int i = 0; i < v.size(); i++) {
			var = (IVariable) v.get(i);
			val = var.getValue();
			if (i == 0) {
				if (val instanceof IJavaArray) fArray = true;
				else if (val.getReferenceTypeName().equals("Complex")) fComplex = true;
			}
			
			if (!fArray && !fComplex)
				var = factoryInst.getVariable(val.getVariables(), "value");
			fQueue.add(var);

			if (optionData.isHighlighted(var)) {
				if (fArray) fHighlights.put(var, new Label(getValueStringFromArray(var)));
				else if (fComplex) fHighlights.put(var, new Label(getValueStringFromComplex(var))); 
				else fHighlights.put(var, new Label(var.getValue().getValueString()));
			}
		}
		int realSize = setChannelText();
		
		// style
		setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		if (fDraw) {
			// self
			parent.add(this);

			// icon
			fIcon = new ChannelToggle(id, fExpanded, realSize, factoryInst);
			parent.add(fIcon);
			
		}
		
		// size
		Dimension d;
		if (fExpanded) d = FigureUtilities.getTextExtents(getText(), factoryInst.getFont());
		else d = FigureUtilities.getTextExtents("\n\n", factoryInst.getFont());
		d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
		d.expand(0, IStreamItGraphConstants.MARGIN);
		factoryInst.roundUpEven(d);
		setSize(d);
		
		// selector
		new ChannelSelector(this);
	}

	public String getId() {
		return fId;
	}
	
	public boolean setSelf(Point p) {
		// self
		if (fDraw) {
			setLocation(p);
			
			if (!fVoid) {
				if (fTurnedOff) {
					fIcon.setVisible(false);
					return fDraw;
				} 
				
				// icon
				if (fQueue.size() < 4) {
					fIcon.setVisible(false);
				} else {
					int w = -IStreamItGraphConstants.MARGIN/2 - fIcon.getSize().width; 	
					fIcon.setLocation(p.getTranslated(w, IStreamItGraphConstants.MARGIN));
				}
				
				// highlights
				int realSize = fQueue.size();
				for (int i = 0; i < realSize; i++) {
					Label highlight = (Label) fHighlights.get(fQueue.get(i));
					if (highlight == null) continue;

					// reverse index
					int size;
					if (fExpanded) size = Math.max(realSize, 3);
					else size = 3;
					int datumSize = getSize().height/size;
					int index = i;
					if ((fInput && fForward) || (!fInput && !fForward)) index = size - 1 - index;
					
					// highlight style
					Dimension d = FigureUtilities.getTextExtents(highlight.getText(), StreamItViewFactory.getInstance().getFont());
					d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
					if (index == 0 || index == size - 1)
						d.expand(0, IStreamItGraphConstants.MARGIN/2);
					highlight.setSize(d);
					highlight.setOpaque(true);
					highlight.setForegroundColor(ColorConstants.menuBackgroundSelected);
					highlight.setBackgroundColor(ColorConstants.menuForegroundSelected);
					add(highlight);

					highlight.setLocation(getLocation().getTranslated(0, index*datumSize));
				}
			}
		} 
		
		return fDraw;
	}

	public int setChannelText() throws DebugException {
		int size;
		int realSize = fQueue.size();
		if (!fExpanded && realSize < 4) fExpanded = true;
		if (fExpanded) size = realSize; 
		else size = 3;

		StringBuffer text = new StringBuffer("");
		IVariable var;

		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = 0; i < size; i++) {
				var = (IVariable) fQueue.get(i);
				if (fArray) text.insert(0, getValueStringFromArray(var));
				else if (fComplex) text.insert(0, getValueStringFromComplex(var));
				else text.insert(0, var.getValue().getValueString());

				if (i < size - 1) text.insert(0, "\n");
			}

			if (realSize < 2) text.insert(0, "\n\n");
			else if (realSize == 2) text.insert(0, "\n");
		} else {
			for (int i = 0; i < size; i++) {
				var = (IVariable) fQueue.get(i);
				if (fArray) text.append(getValueStringFromArray(var));
				else if (fComplex) text.append(getValueStringFromComplex(var));
				else text.append(var.getValue().getValueString());

				if (i < size - 1) text.append("\n");
			}
			if (realSize < 2) text.append("\n\n");
			else if (realSize == 2) text.append("\n");

		}

		setText(text.toString());
		return realSize;
	}
	
	private String getValueStringFromArray(IVariable var) throws DebugException {
		StringBuffer array = new StringBuffer("");
		IJavaValue[] vals = ((IJavaArray) var.getValue()).getValues();
		for (int j = 0; j < vals.length; j++) {
			array.append("{");
			IVariable[] vars = vals[j].getVariables();
			for (int k = 0; k < vars.length; k++) {
				array.append("[" + vars[k].getValue().getValueString() + "]");
			}
			array.append("}");
		}
		return array.toString();
	}
	
	private String getValueStringFromComplex(IVariable var) throws DebugException {
		StreamItViewFactory factoryInst = StreamItViewFactory.getInstance();
		IVariable[] vars = var.getValue().getVariables();
		return factoryInst.getValueString(vars, "real") + '+' + factoryInst.getValueString(vars, "imag") + 'i';
	}

	public void turnOff(boolean expanded) {
		// only called by non-top-level pipelines
		fTurnedOff = expanded;
		if (fTurnedOff) setText("");
	}
	
	// for ChangeQueueValuesAction
	public boolean isChangeable() {
		if (fTurnedOff) return false;
		return (fQueue.size() > 0);
	}
	
	public String getQueueAsString() throws DebugException {
		StringBuffer text = new StringBuffer("");
		int size = fQueue.size();
		IVariable var;

		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = 0; i < size; i++) {
				var = (IVariable) fQueue.get(i);
				if (fArray) text.insert(0, getValueStringFromArray(var));
				else if (fComplex) text.insert(0, getValueStringFromComplex(var));
				else text.insert(0, var.getValue().getValueString());

				if (i < size - 1) text.insert(0, "\n");				
			}
		} else {
			for (int i = 0; i < size; i++) {
				var = (IVariable) fQueue.get(i);
				if (fArray) text.append(getValueStringFromArray(var));
				else if (fComplex) text.append(getValueStringFromComplex(var));
				else text.append(var.getValue().getValueString());

				if (i < size - 1) text.append("\n");
			}
		}

		return text.toString();
	}
	
	public boolean verifyValues(String input) throws DebugException {
		StringTokenizer st = new StringTokenizer(input);
		int size = fQueue.size();
		if (size != st.countTokens()) return false;
		
		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = size - 1; i > -1; i--) {
				if (fArray) {
					if (!verifyArray((IVariable) fQueue.get(i), st.nextToken())) return false;
				} else if (fComplex) {
					if (!verifyComplex((IVariable) fQueue.get(i), st.nextToken())) return false;
				} else {
					if (!((IVariable) fQueue.get(i)).verifyValue(st.nextToken())) return false;
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (fArray) {
					if (!verifyArray((IVariable) fQueue.get(i), st.nextToken())) return false;
				} else if (fComplex) {
					if (!verifyComplex((IVariable) fQueue.get(i), st.nextToken())) return false;
				} else {
					if (!((IVariable) fQueue.get(i)).verifyValue(st.nextToken())) return false;
				}
			}
		}
		
		return true;
	}

	private boolean verifyArray(IVariable var, String array) throws DebugException {
		IJavaValue[] vals = ((IJavaArray) var.getValue()).getValues();
		int index = 0;
		int verifyStop;
		for (int j = 0; j < vals.length; j++) {
			if (array.charAt(index) != '{') return false;
			index++;
			IVariable[] vars = vals[j].getVariables();
			for (int k = 0; k < vars.length; k++) {
				if (array.charAt(index) != '[') return false;
				index++;
				
				verifyStop = array.indexOf(']', index);
				if (!vars[k].verifyValue(array.substring(index, verifyStop))) return false;
				index = verifyStop;

				if (array.charAt(index) != ']') return false;
				index++;
			}
			if (array.charAt(index) != '}') return false;
			index++;
		}
		return true;
	}
	
	private boolean verifyComplex(IVariable var, String complex) throws DebugException {	
		StreamItViewFactory factoryInst = StreamItViewFactory.getInstance();
		IVariable[] vars = var.getValue().getVariables();
		
		int plus = complex.indexOf('+');
		int i = complex.indexOf('i');
		if (plus == -1 || i == -1) return false;
		String real = complex.substring(0, plus);
		String imag = complex.substring(plus + 1, i);
		
		if (!factoryInst.getVariable(vars, "real").verifyValue(real)) return false;
		if (!factoryInst.getVariable(vars, "imag").verifyValue(imag)) return false;
		
		return true;
	}
	
	public void update(String input) throws DebugException {
		StringTokenizer st = new StringTokenizer(input);
		int size = fQueue.size();
		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = size - 1; i > -1; i--) {
				if (fArray) {
					updateArray((IVariable) fQueue.get(i), st.nextToken());
				} else if (fComplex) {
					updateComplex((IVariable) fQueue.get(i), st.nextToken());
				} else {
					((IVariable) fQueue.get(i)).setValue(st.nextToken());	
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (fArray) {
					updateArray((IVariable) fQueue.get(i), st.nextToken());
				} else if (fComplex) {
					updateComplex((IVariable) fQueue.get(i), st.nextToken());
				} else {
					((IVariable) fQueue.get(i)).setValue(st.nextToken());					
				}
			} 
		}
		
		setChannelText();
	}
	
	private void updateArray(IVariable var, String array) throws DebugException {
		IJavaValue[] vals = ((IJavaArray) var.getValue()).getValues();
		int index = 0;
		int updateStop;
		
		for (int j = 0; j < vals.length; j++) {
			index++;
			IVariable[] vars = vals[j].getVariables();
			for (int k = 0; k < vars.length; k++) {
				index++;
				
				updateStop = array.indexOf(']', index);
				vars[k].setValue(array.substring(index, updateStop));
				index = updateStop;

				index++;
			}
			index++;
		}
	}
	
	private void updateComplex(IVariable var, String complex) throws DebugException {
		StreamItViewFactory factoryInst = StreamItViewFactory.getInstance();
		IVariable[] vars = var.getValue().getVariables();
		
		int plus = complex.indexOf('+');
		int i = complex.indexOf('i');
		if (plus == -1 || i == -1) return;
		String real = complex.substring(0, plus);
		String imag = complex.substring(plus + 1, i);
		
		factoryInst.getVariable(vars, "real").setValue(real);
		factoryInst.getVariable(vars, "imag").setValue(imag);
	}

	// for HighlightDatumAction
	public int onDatum(int y) {
		if (fTurnedOff) return -1;
		
		int realSize = fQueue.size();
		int size;
		if (fExpanded) size = Math.max(realSize, 3);
		else size = 3;
		int datumSize = getSize().height/size;

		int index = (y - getBounds().getTop().y - IStreamItGraphConstants.MARGIN/2)/datumSize;
		if ((fInput && fForward) || (!fInput && !fForward)) index = size - 1 - index;
		if (index + 1 > realSize) return -1;

		return index;
	}
	
	public boolean isHighlighted(int index, OptionData optionData) {
		return optionData.isHighlighted((IVariable) fQueue.get(index));
	}
	
	public void highlight(int index, OptionData optionData) {
		IVariable datum = (IVariable) fQueue.get(index);
		optionData.toggleChannel(datum);
		
		// create highlight
		String text;
		try {
			if (fArray) text = getValueStringFromArray(datum);
			else if (fComplex) text = getValueStringFromComplex(datum); 
			else text = datum.getValue().getValueString();
		} catch (DebugException e) {
			text = "";
		}
		Label highlight = new Label(text);

		// reverse index		
		int realSize = fQueue.size();
		int size;
		if (fExpanded) size = Math.max(realSize, 3);
		else size = 3;
		int datumSize = getSize().height/size;
		if ((fInput && fForward) || (!fInput && !fForward)) index = size - 1 - index;

		// highlight style
		Dimension d = FigureUtilities.getTextExtents(text, StreamItViewFactory.getInstance().getFont());
		d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
		if (index == 0 || index == size - 1)
			d.expand(0, IStreamItGraphConstants.MARGIN/2);
		highlight.setSize(d);
		highlight.setOpaque(true);
		highlight.setForegroundColor(ColorConstants.menuBackgroundSelected);
		highlight.setBackgroundColor(ColorConstants.menuForegroundSelected);
		add(highlight);

		highlight.setLocation(getLocation().getTranslated(0, index*datumSize));
		fHighlights.put(datum, highlight);
	}
	
	public void unhighlight(int index, OptionData optionData) {
		IVariable datum = (IVariable) fQueue.get(index);
		optionData.toggleChannel(datum);

		remove((Figure) fHighlights.remove(datum));
	}
	
	public int getChannelToggleWidth() {
		if (fIcon == null) return 0;
		return fIcon.getSize().width;
	}
}