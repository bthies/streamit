package streamit.eclipse.debugger.graph;

import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.debug.core.model.IVariable;

/**
 * @author kkuo
 */
public class Expanded {

	private Set fExpandedStream; // set of String streamNameWithId
	private Set fExpandedChannel; // set of String channelId
	private Vector fHighlightedData; // vector of IVariable queueVar 
	private boolean fHighlighting; // highlighting from StreamItDebugEventSetListener or from Variable/Launch Views
	
	public Expanded(boolean highlighting) {
		fExpandedStream = new TreeSet();
		fExpandedChannel = new TreeSet();
		fHighlightedData = new Vector();
		fHighlighting = highlighting;
	}
	
	public boolean containsStream(String streamNameWithId, boolean filter) {
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
}