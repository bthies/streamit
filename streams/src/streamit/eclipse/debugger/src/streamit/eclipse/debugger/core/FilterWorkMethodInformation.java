package streamit.eclipse.debugger.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.IMethod;

/**
 * @author kkuo
 */
public class FilterWorkMethodInformation {
	
	private IMethod fMethod;
	private int fMethodStart, fMethodEnd;
	private String fTypeName, fMethodSignature, fMethodName;
	
	// String ids
	private HashMap fRuntimeIdsWithBreakpoints; // ids with breakpoints during runtime
	private Set fStaticIdsWithBreakpoints; // ids with breakpoints after termination
	private Set fOldStaticIdsWithBreakpoints; // ids with breakpoints that need to be made into runtime breakpoints

	public FilterWorkMethodInformation(IMethod method, int methodStart, int methodEnd, String typeName, String methodSignature, String methodName) {
		fMethod = method;
		fMethodStart = methodStart;
		fMethodEnd = methodEnd;
		fTypeName = typeName;
		fMethodSignature = methodSignature;
		fMethodName = methodName;
		fRuntimeIdsWithBreakpoints = new HashMap();
		fStaticIdsWithBreakpoints = new TreeSet();
		fOldStaticIdsWithBreakpoints = new TreeSet();
	}
	
	public boolean hasId(String runtimeId, String staticId, boolean terminated) {
		if (!terminated && staticId != null && fOldStaticIdsWithBreakpoints.size() != 0) {
			if (fOldStaticIdsWithBreakpoints.contains(staticId)) {
				fRuntimeIdsWithBreakpoints.put(runtimeId, staticId);
				fOldStaticIdsWithBreakpoints.remove(staticId);
				return true;
			}
		}
		
		if (!terminated) return fRuntimeIdsWithBreakpoints.containsKey(runtimeId);
		else return fStaticIdsWithBreakpoints.contains(staticId);
	}
	
	public boolean hasNoIds(boolean terminated) {
		if (!terminated) return fRuntimeIdsWithBreakpoints.size() == 0;
		else return fStaticIdsWithBreakpoints.size() == 0;
	}
	
	public void addId(String runtimeId, String staticId, boolean terminated) {
		if (!terminated) fRuntimeIdsWithBreakpoints.put(runtimeId, staticId);
		else fStaticIdsWithBreakpoints.add(staticId);
	}
	
	public void removeId(String runtimeId, String staticId, boolean terminated) {
		if (!terminated) fRuntimeIdsWithBreakpoints.remove(runtimeId);
		else fStaticIdsWithBreakpoints.remove(staticId);
	}
	
	public void moveRuntimeToStaticIds() {
		Iterator i = fRuntimeIdsWithBreakpoints.keySet().iterator();
		while (i.hasNext()) {
			fStaticIdsWithBreakpoints.add(fRuntimeIdsWithBreakpoints.get(i.next()));
		}
		fRuntimeIdsWithBreakpoints.clear();
	}
	
	public Set getStaticIdsWithBreakpoints() {
		return fStaticIdsWithBreakpoints;
	}
	
	public void addOldStaticIdsWithBreakpoints(Set s) {
		fOldStaticIdsWithBreakpoints.addAll(s);
	}
	
	public IMethod getMethod() {
		return fMethod;
	}
	
	public int getMethodEnd() {
		return fMethodEnd;
	}

	public String getMethodSignature() {
		return fMethodSignature;
	}

	public int getMethodStart() {
		return fMethodStart;
	}

	public String getTypeName() {
		return fTypeName;
	}
	
	public String getMethodName() {
		return fMethodName;
	}
}