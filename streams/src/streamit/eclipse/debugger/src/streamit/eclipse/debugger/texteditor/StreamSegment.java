package streamit.eclipse.debugger.texteditor;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.jface.text.Position;

/**
 * @author kkuo
 */
public class StreamSegment implements Comparable {

	protected static final int FIELD_TYPE = 1;
	protected static final int METHOD_TYPE = 2;
	protected static final int FILTER_TYPE = 3;
	protected static final int PIPELINE_TYPE = 4;
	protected static final int SPLITJOIN_TYPE = 5;
	protected static final int FEEDBACKLOOP_TYPE = 6;

	private String fName;
	private Position fPosition;
	private StreamSegment fParent;
	private Vector fFields;
	private TreeSet fMethods;
	private int fType;

	public StreamSegment(String name, Position position, StreamSegment parent, int type) {
		fName = name;
		fPosition = position;
		fParent = parent;
		fFields = new Vector();
		fMethods = new TreeSet();
		fType = type;
	}
		
	public String toString() {
		return fName;
	}
		
	public Position getPosition() {
		return fPosition;
	}

	public StreamSegment getParent() {
		return fParent;
	}

	public void addVariable(StreamSegment v) {
		fFields.add(v);
	}
		
	public void addMethod(StreamSegment m) {
		fMethods.add(m);
	}
	
	public boolean hasChildren() {
		return fFields.size() + fMethods.size() != 0;
	}
	
	public Object[] getChildren() {
		int size = fFields.size();
		Object[] o = new Object[size + fMethods.size()];
		Iterator vi = fFields.iterator();
		Iterator mi = fMethods.iterator();
		for (int i = 0; i < o.length; i++) {
			if (i < size) o[i] = vi.next();
			else o[i] = mi.next();
		}
		return o;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof StreamSegment) {
			return fPosition.getOffset() - ((StreamSegment) o).getPosition().getOffset();
		}
		return 0;
	}
	
	public int getType() {
		return fType;
	}
}
