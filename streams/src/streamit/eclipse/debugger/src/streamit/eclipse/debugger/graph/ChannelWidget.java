package streamit.eclipse.debugger.graph;

import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.model.Channel;
import streamit.eclipse.debugger.model.Datum;

/**
 * @author kkuo
 */
public class ChannelWidget extends Label {

	private Channel fModel;
	private Vector fQueue; 		// vector of Datum, where some datum are dummys (for fQueue.size < 3) 
								// order of data s.t. they must ALWAYS be reversed for display
	private String fId;			// id for expanded list of channels
	private ChannelToggle fIcon;

	private boolean fForward; 	// whether layout is going top-down
	private boolean fTurnedOff; // used for expanded inputs of pipelines
	private boolean fDraw; 		// whether the channel should be draw and the location set (void channels need to be drawn)
	private boolean fExpanded; 	// if channel is expanded by user 
	private boolean fLessThanFour; // if channel is has 3 or less items
	private int fNumDummies;	// number of dummy

	public ChannelWidget(Channel model, String id, Figure parent, boolean forward, boolean lastChild, OptionData optionData, StreamItViewFactory factoryInst) throws DebugException {
		super();
		fModel = model;
		fId = id;
		fForward = forward;

		// channel is void
		if (fModel.isVoid()) {
			fDraw = true;
			fExpanded = false;
			fNumDummies = 0;
			
			parent.add(this);
			setVisible(false);
			Dimension d = FigureUtilities.getTextExtents(IStreamItGraphConstants.EMPTY_STRING, factoryInst.getFont());
			d.width = IStreamItGraphConstants.CHANNEL_WIDTH;
			d.expand(0, IStreamItGraphConstants.MARGIN);
			factoryInst.roundUpEven(d);
			setSize(d);
			return;
		}
		boolean input = fModel.isInput();
		fDraw = input || lastChild;
		fExpanded = optionData.containsChannel(id);

		// get contents
		Vector data = fModel.getQueue();

		// calculate size
		int realSize = data.size();
		if (realSize < 4) fLessThanFour = true;

		// create contents
		fQueue = new Vector();
		Label datum;
		int childrenHeight = 0;
		for (int i = 0; i < realSize; i++) {
			// add datum
			datum = new DatumWidget((Datum) data.get(i), false, optionData, factoryInst);

			fQueue.add(datum);
			add(datum);

			// size
			childrenHeight += datum.getSize().height;
		}

		// add dummy children to opposite of where they will be placed (fQueue will be reversed)
		childrenHeight += addDummy(factoryInst, ((input && fForward) || (!input && !fForward)), realSize);
		
		// style
		setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		if (fDraw) {
			// self
			parent.add(this);

			// icon
			fIcon = new ChannelToggle(id, fExpanded || fLessThanFour, realSize, factoryInst);
			parent.add(fIcon);
		}
		
		// size
		Dimension d;
		int channelHeight = 0;
		if (fExpanded || fLessThanFour) channelHeight = childrenHeight;
		else {
			// find height of last 3 of fQueue
			for (int i = fQueue.size() - 1; i > fQueue.size() - 4; i--)
				channelHeight += ((DatumWidget) fQueue.get(i)).getSize().height;
		}
		d = new Dimension(IStreamItGraphConstants.CHANNEL_WIDTH, channelHeight);
		factoryInst.roundUpEven(d);
		setSize(d);
		
		// selector
		new ChannelSelector(this);
	}
	
	private int addDummy(StreamItViewFactory factoryInst, boolean append, int realSize) {
		fNumDummies = Math.max(3 - realSize, 0);
		int dummyHeight = 0;
		
		for (int i = 0; i < fNumDummies; i++) dummyHeight += addDummy(factoryInst, append);
		return dummyHeight;
	}
	
	private int addDummy(StreamItViewFactory factoryInst, boolean append) {
		DatumWidget datum = new DatumWidget(null, true, null, factoryInst);
		if (append) fQueue.add(datum);
		else fQueue.insertElementAt(datum, 0);
		add(datum);
		return datum.getSize().height;
	}

	public String getId() {
		return fId;
	}
	
	public boolean isExpanded() {
		return fExpanded;
	}

	public int getChannelToggleWidth() {
		if (fIcon == null) return 0;
		return fIcon.getSize().width;
	}
	
	public boolean setSelf(Point p, boolean top) {
		if (!fDraw) return fDraw;

		// self
		setLocation(p);
		
		if (fModel.isVoid()) return fDraw;

		if (fTurnedOff) {
			fIcon.setVisible(false);
			return fDraw;
		}
				
		// icon
		if (fQueue.size() - fNumDummies < 4) {
			fIcon.setVisible(false);
		} else {
			int w = -IStreamItGraphConstants.MARGIN/2 - fIcon.getSize().width; 	
			fIcon.setLocation(p.getTranslated(w, IStreamItGraphConstants.MARGIN));
		}

		// data
		DatumWidget datum;
		int size = fQueue.size();
		int currentHeight = 0;
		if (top) {
			Point bottom = p.getTranslated(0, getSize().height);
			for (int i = 0; i < size; i++) {
				if (!fExpanded && i < size - 3) ((DatumWidget) fQueue.get(i)).setVisible(false);
				else currentHeight = ((DatumWidget) fQueue.get(i)).setLocationFromBottom(bottom, currentHeight);
			}
		} else {
			for (int i = size - 1; i > -1; i--) {
				if (!fExpanded && i < size - 3) ((DatumWidget) fQueue.get(i)).setVisible(false);
				else currentHeight = ((DatumWidget) fQueue.get(i)).setLocationFromTop(p, currentHeight);
			}			
		}
		
		return fDraw;
	}

	public void turnOff(boolean expanded) {
		// only called by non-top-level pipelines
		fTurnedOff = expanded;
		if (fTurnedOff && !fModel.isVoid()) for (int i = 0; i < fQueue.size(); i++) ((DatumWidget) fQueue.get(i)).setVisible(false);
	}
	
	// color-code Queue, gray = peeked or pushed
	public void grayData(int uptoIndex) {
		if (!fDraw || fModel.isVoid() || fTurnedOff || uptoIndex < 0) return;
		int size = fQueue.size();
		DatumWidget datum;
		int stop = uptoIndex;
		if (!fExpanded) stop = 3;
		
		for (int i = 0; i < stop; i++) {
			datum = (DatumWidget) fQueue.get(i);
			datum.setForegroundColor(ColorConstants.gray);
		}
	}
	
	// for ChangeQueueValuesAction
	public boolean isChangeable() {
		if (fTurnedOff) return false;
		return (fQueue.size() - fNumDummies > 0);
	}
	
	public String getQueueAsString() throws DebugException {
		StringBuffer text = new StringBuffer(IStreamItGraphConstants.EMPTY_STRING);
		int size = fQueue.size();
		DatumWidget datum;
				
		for (int i = size - 1; i > -1; i--) {
			datum = (DatumWidget) fQueue.get(i);
			if (!datum.isDummy()) text.append(datum.getText());
			if (i > 0) text.append(IStreamItGraphConstants.NEWLINE_STRING);
		}
		return text.toString();
	}
	
	public boolean verifyValues(String input) throws DebugException {
		StringTokenizer st = new StringTokenizer(input);
		int size = fQueue.size();
		if (size - fNumDummies != st.countTokens()) return false;
		DatumWidget datum;
		
		for (int i = size - 1; i > -1; i--) {
			datum = (DatumWidget) fQueue.get(i);
			if (datum.isDummy()) continue;
			if (!datum.verify(fModel, st.nextToken())) return false;
		}
		return true;
	}

	public void update(String input) throws DebugException {
		StringTokenizer st = new StringTokenizer(input);
		int size = fQueue.size();
		DatumWidget datum;

		for (int i = size - 1; i > -1; i--) {
			datum = (DatumWidget) fQueue.get(i);
			if (datum.isDummy()) continue;
			datum.update(fModel, st.nextToken());
		}
	}

	// for HighlightDatumAction
	public DatumWidget onDatum(Point p) {
		if (fTurnedOff) return null;
		
		DatumWidget datum;
		int size = fQueue.size();
		for (int i = size - 1; i > -1; i--) {
			datum = (DatumWidget) fQueue.get(i);
			if (!datum.isVisible()) return null;
			if (!datum.isDummy() && datum.getBounds().contains(p)) return datum;
		}
		return null;
	}
}