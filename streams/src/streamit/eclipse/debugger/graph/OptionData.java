package streamit.eclipse.debugger.graph;

import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.debug.core.model.IVariable;

/**
 * @author kkuo
 */
public class OptionData {

	private Set fExpandedStream; // set of String streamNameWithId
	private Set fExpandedChannel; // set of String channelId
	private Vector fHighlightedData; // vector of IVariable queueVar
	private boolean fExpandAll; // for expandall button
	private boolean fHighlighting; // highlighting from StreamItDebugEventSetListener or from Variable/Launch Views
	private boolean fHideLines; // for hide lines button
	
	public OptionData(boolean highlighting) {
		fExpandedStream = new TreeSet();
		fExpandedChannel = new TreeSet();
		fHighlightedData = new Vector();
		fExpandAll = false;
		fHighlighting = highlighting;
		fHideLines = false;
	}
	
	public boolean containsStream(String streamNameWithId, boolean filter) {
		if (fExpandAll) {
			fExpandedStream.add(streamNameWithId);
			return true;
		}
		
		if (fHighlighting) {
			if (filter) return fExpandedStream.contains(streamNameWithId);
			fExpandedStream.add(streamNameWithId);
			return true;
		}
		
		return fExpandedStream.contains(streamNameWithId);
	}

	public void setHighlightSelect(boolean highlighting) {
		fHighlighting = highlighting;
	}
	
	public void toggleStream(String streamNameWithId) {
		if (!fExpandedStream.remove(streamNameWithId)) fExpandedStream.add(streamNameWithId);
	}
	
	public boolean containsChannel(String channelId) {
		return fExpandedChannel.contains(channelId);
	}

	public void toggleChannel(String channelId) {
		if (!fExpandedChannel.remove(channelId)) fExpandedChannel.add(channelId);
	}
	
	public boolean isHighlighted(IVariable var) {
		return fHighlightedData.contains(var);
	}

	public void toggleChannel(IVariable var) {
		if (!fHighlightedData.remove(var)) fHighlightedData.add(var);
	}
	
	public void setExpandAll(boolean b) {
		fExpandAll = b;
	}
	
	public boolean isExpandAll() {
		return fExpandAll;
	}
	
	public void clearExpanded() {
		fExpandedStream = new TreeSet();
		fExpandedChannel = new TreeSet();
	}
	
	public boolean isHideLines() {
		return fHideLines;
	}
	
	public void setHideLines(boolean b) {
		fHideLines = b;
	}
}