package streamit.eclipse.debugger.graph;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author kkuo
 */
public class Expanded {

	private Set fExpanded;
	private boolean fHighlighting;
	
	public Expanded(boolean highlighting) {
		fExpanded = new TreeSet();
		fHighlighting = highlighting;
	}
	
	public boolean contains(String streamNameWithId, boolean filter) {
		if (fHighlighting) {
			if (filter) return false;
			fExpanded.add(streamNameWithId);
			return true;
		}
		return fExpanded.contains(streamNameWithId);
	}
	
	public void setHighlighting(boolean highlighting) {
		fHighlighting = highlighting;
	}
	
	public void toggle(String streamNameWithId) {
		if (!fExpanded.remove(streamNameWithId)) fExpanded.add(streamNameWithId);
	}
}