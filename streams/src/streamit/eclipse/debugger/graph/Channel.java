package streamit.eclipse.debugger.graph;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

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
	
	private String fId;
	private ChannelToggle fIcon;

	public Channel(IVariable[] vars, String id, Figure parent, boolean input, boolean forward, boolean lastChild, Expanded allExpanded, StreamItViewFactory factoryInst) throws DebugException {
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
		fExpanded = allExpanded.containsChannel(id);
		
		// contents
		vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(vars, "queue"), "header"), "next");
		Vector v = factoryInst.getLinkedListElements(vars);
		fQueue = new Vector();
		fHighlights = new HashMap();
		IVariable var; 
		for (int i = 0; i < v.size(); i++) {
			var = factoryInst.getVariable(((IVariable) v.get(i)).getValue().getVariables(), "value");
			fQueue.add(var);
			if (allExpanded.isHighlighted(var))
				fHighlights.put(var, new Label(var.getValue().getValueString()));
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
				fIcon.setLocation(p.getTranslated(-IStreamItGraphConstants.MARGIN/2 - StreamItViewFactory.getInstance().getImageWidth(), IStreamItGraphConstants.MARGIN));
				
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
					highlight.setBackgroundColor(ColorConstants.tooltipBackground);
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

		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = 0; i < size; i++) {
				text.insert(0, ((IVariable) fQueue.get(i)).getValue().getValueString());
				if (i < size - 1) text.insert(0, "\n");
			}

			if (realSize < 2) text.insert(0, "\n\n");
			else if (realSize == 2) text.insert(0, "\n");
		} else {
			for (int i = 0; i < size; i++) {
				text.append(((IVariable) fQueue.get(i)).getValue().getValueString());
				if (i < size - 1) text.append("\n");
			}
			if (realSize < 2) text.append("\n\n");
			else if (realSize == 2) text.append("\n");

		}

		setText(text.toString());
		return realSize;
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
		StreamItViewFactory factoryInst = StreamItViewFactory.getInstance();
		StringBuffer text = new StringBuffer("");

		int size = fQueue.size();
		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = 0; i < size; i++) {
				text.insert(0, ((IVariable) fQueue.get(i)).getValue().getValueString());
				if (i < size - 1) text.insert(0, "\n");
			}
		} else {
			for (int i = 0; i < size; i++) {
				text.append(((IVariable) fQueue.get(i)).getValue().getValueString());
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
				if (!((IVariable) fQueue.get(i)).verifyValue(st.nextToken())) return false;
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (!((IVariable) fQueue.get(i)).verifyValue(st.nextToken())) return false;			
			}
		}
		
		return true;
	}

	public void update(String input) throws DebugException {
		StringTokenizer st = new StringTokenizer(input);
		int size = fQueue.size();
		if ((fInput && fForward) || (!fInput && !fForward)) {
			for (int i = size - 1; i > -1; i--) ((IVariable) fQueue.get(i)).setValue(st.nextToken());	
		} else {
			for (int i = 0; i < size; i++) ((IVariable) fQueue.get(i)).setValue(st.nextToken());
		}
		
		setChannelText();
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
	
	public boolean isHighlighted(int index, Expanded allExpanded) {
		return allExpanded.isHighlighted((IVariable) fQueue.get(index));
	}
	
	public void highlight(int index, Expanded allExpanded) {
		IVariable datum = (IVariable) fQueue.get(index);
		allExpanded.toggleChannel(datum);
		
		// create highlight
		String text;
		try {
			text = datum.getValue().getValueString();
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
		highlight.setBackgroundColor(ColorConstants.tooltipBackground);
		add(highlight);

		highlight.setLocation(getLocation().getTranslated(0, index*datumSize));
		fHighlights.put(datum, highlight);
	}
	
	public void unhighlight(int index, Expanded allExpanded) {
		IVariable datum = (IVariable) fQueue.get(index);
		allExpanded.toggleChannel(datum);

		remove((Figure) fHighlights.remove(datum));
	}
}