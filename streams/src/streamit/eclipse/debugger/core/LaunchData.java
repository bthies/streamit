package streamit.eclipse.debugger.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;

/**
 * @author kkuo
 */
public class LaunchData {
	
	// key = Integer javaLineNumber, entry = StreamItLineNumber
	private HashMap fJavaToStrLineNumbers;

	// key = IVariable filter, entry = StrFilter
	private static HashMap fFilters = new HashMap();
	
	// key = IVariable pipeline, entry = StrPipeline
	// ok to be static because IVar are unique across debug sessions
	private static HashMap fPipelines = new HashMap();
	
	// key = IVariable splitjoin, entry = StrSplitjoin
	// ok to be static because IVar are unique across debug sessions
	private static HashMap fSplitjoins = new HashMap();
	
	// key = IValue channel, entry = StrChannel
	// ok to be static because IValues are unique across debug sessions
	private static HashMap fChannels = new HashMap();
	
	// key = names (without id)
	private Vector fPipelineNames;
	private Vector fSplitjoinNames;
	private Vector fFeedbackLoopNames;

	// key = filter name (without id), entry = String[] fields
	private HashMap fFilterNameToStateVariables;
	
	// key = IVariable datum, entry = HashMap attributes
	private static HashMap fDataAttributes = new HashMap();
	
	// 0 = init, 1 = work, 2 = prework, 3 = others
	// entry = vector of breakpoints
	private List[] fAllBreakpoints;

	protected LaunchData() {
		fJavaToStrLineNumbers = new HashMap();

		fPipelineNames = new Vector();
		fSplitjoinNames = new Vector();
		fFeedbackLoopNames = new Vector();
		
		fFilterNameToStateVariables = new HashMap();
		fAllBreakpoints = new List[4];
		for (int i = 0; i < fAllBreakpoints.length; i++) fAllBreakpoints[i] = new Vector();
	}
	
	protected void mapJavaToStr(IFile javaFile) throws CoreException, IOException {
		fJavaToStrLineNumbers.clear();

		// look for commented mappings
		InputStream inputStream = javaFile.getContents();
		InputStreamReader isr = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(isr);
		String strLine = null;
		int javaLineNumber = 0;
		int index;
		while (true) {
			javaLineNumber++;
			strLine = br.readLine();
			if (strLine == null) {
				inputStream.close();
				return;
			} 
			index = strLine.lastIndexOf(IPath.DEVICE_SEPARATOR);
			if (index != -1) {
				String strFileName = strLine.substring(strLine.lastIndexOf(File.separatorChar) + 1, index); 
				fJavaToStrLineNumbers.put(new Integer(javaLineNumber), 
										new StreamItLineNumber(Integer.valueOf(strLine.substring(index + 1)).intValue(), strFileName));
			} 	
		}
	}

	protected int getLineNumber(IFile javaFile, Integer javaLineNumber) {
		StreamItLineNumber strLineNumber = (StreamItLineNumber) fJavaToStrLineNumbers.get(javaLineNumber);
		if (strLineNumber == null) return -1;
		return strLineNumber.getLineNumber();
	}
	
	protected static Object getStrStream(IVariable var) {
		if (fPipelines.containsKey(var)) return fPipelines.get(var);
		if (fSplitjoins.containsKey(var)) return fSplitjoins.get(var);
		return fFilters.get(var);
	}
	
	protected StrPipeline createPipeline(IVariable var, String streamName, String streamNameWithId) {
		if (fPipelineNames.contains(streamName)) {
			StrPipeline pipeline = new StrPipeline(streamNameWithId); 
			fPipelines.put(var, pipeline);
			return pipeline;
		}
		return null;
	}

	protected StrSplitJoin createSplitjoin(IVariable var, String streamName, String streamNameWithId) {
		if (fSplitjoinNames.contains(streamName)) {
			StrSplitJoin splitjoin = new StrSplitJoin(streamNameWithId); 
			fSplitjoins.put(var, splitjoin);
			return splitjoin;
		}
		return null;
	}
	
	protected StrFilter createFilter(IVariable filterVar, String streamName, String streamNameWithId) {
		StrFilter filter = new StrFilter(streamNameWithId, getFilterVariables(streamName));
		fFilters.put(filterVar, filter);
		return filter;
	}
	
	protected static StrFilter getFilter(IVariable[] vars) {
		if (vars.length < 1) return null;
		return (StrFilter) fFilters.get(vars[0]);
	}
	
	protected void addPipelineName(String name) {
		fPipelineNames.add(name);
	}
	
	protected void addSplitjoinName(String name) {
		fSplitjoinNames.add(name);
	}
	
	protected void addFeedbackLoopName(String name) {
		fFeedbackLoopNames.add(name);
	}
	
	protected static void addChannel(IValue channel, boolean setInput, String filterName) throws Exception {
		fChannels.put(channel, new StrChannel(setInput, filterName));
	}

	protected static Object getChannel(IValue channel) {
		return fChannels.get(channel);
	}
	
	protected void addFilterVariables(String filterName, String[] fields) {
		fFilterNameToStateVariables.put(filterName, fields);		
	}
	
	protected String[] getFilterVariables(String filterName) {
		return (String[]) fFilterNameToStateVariables.get(filterName);
	}
	
	protected static void addAttributes(IVariable dataVar, HashMap attributes) {
		fDataAttributes.put(dataVar, attributes);	
	}

	protected static HashMap getAttributes(IVariable dataVar) {
		return (HashMap) fDataAttributes.get(dataVar);	
	}
	
	protected static boolean isHighlighted(IVariable dataVar) {
		HashMap h = getAttributes(dataVar);
		if (h == null) return false;
		return h.containsKey(IStreamItDebuggerConstants.ATTR_HIGHLIGHT);
	}
	
	public static void changeHighlightAttribute(IVariable datumVar, boolean highlight) {
		HashMap h = getAttributes(datumVar);
		if (h == null) h = new HashMap();
		if (highlight) h.put(IStreamItDebuggerConstants.ATTR_HIGHLIGHT, null);
		else h.remove(IStreamItDebuggerConstants.ATTR_HIGHLIGHT);
	}

	protected void addBreakpoint(int methodType, boolean resume, IJavaMethodBreakpoint breakpoint) throws CoreException {
		fAllBreakpoints[methodType].add(breakpoint);
		breakpoint.getMarker().setAttribute(IStreamItDebuggerConstants.RESUME, resume);
	}
	
	protected boolean containsInitBreakpoint(IBreakpoint b) {
		return fAllBreakpoints[IStreamItDebuggerConstants.INIT_BREAKPOINTS].contains(b);
	}
	
	protected boolean containsWorkBreakpoint(IBreakpoint b) {
		return fAllBreakpoints[IStreamItDebuggerConstants.WORK_BREAKPOINTS].contains(b);
	}
	
	protected boolean containsPreworkBreakpoint(IBreakpoint b) {
		return fAllBreakpoints[IStreamItDebuggerConstants.PREWORK_BREAKPOINTS].contains(b);
	}
	
	protected boolean containsOtherBreakpoint(IBreakpoint b) {
		return fAllBreakpoints[IStreamItDebuggerConstants.OTHER_BREAKPOINTS].contains(b);
	}

	protected void clearBreakpoints() {			
		// do unset entries for entries that were changed from true
		try {
			Iterator i;
			IJavaMethodBreakpoint b;
			
			for (int j = 0; j < 4; j++) {
				i = fAllBreakpoints[j].iterator();
				while (i.hasNext()) {
					b = (IJavaMethodBreakpoint) i.next();
					//if (b.getMarker().getAttribute(IStreamItDebuggerConstants.RESUME, true)) b.setEntry(false);
					b.setExit(false);
					b.setEntry(false);
				}
			}
		} catch (CoreException e) {
		}
	}
}
