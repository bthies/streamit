package streamit.eclipse.debugger.model;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaValue;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;

/**
 * @author kkuo
 */
public class SplitJoin extends StreamStructure {

	private boolean fDuplicateSplitter;
	private Vector fSplitterWeights;	// Vector of strings
	private Vector fJoinerWeights;		// Vector of strings

	public SplitJoin(IValue splitjoinVal, String name, String staticId, StreamStructureModelFactory factoryInst) throws DebugException {
		super(name, splitjoinVal.getValueString(), staticId, splitjoinVal.getVariables(), factoryInst);
		IVariable[] splitjoinVars = splitjoinVal.getVariables();
		
		// expanded children
		IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(splitjoinVars, IStreamItGraphConstants.CHILDRENSTREAMS_FIELD), IStreamItGraphConstants.HEADER_FIELD), IStreamItGraphConstants.NEXT_FIELD);
		Vector elements = factoryInst.getLinkedListElements(vars);
		IJavaValue val;
		IJavaClassType type;
		String streamType, streamName;
		fChildren = new Vector();
		for (int i = 0; i < elements.size(); i++) {
			val = (IJavaValue) ((IVariable) elements.get(i)).getValue();
			type = (IJavaClassType) val.getJavaType();
			streamName = type.getName();
			streamType = type.getSuperclass().getName();
			if (streamType.equals(IStreamItGraphConstants.FILTER_FIELD) ||
				streamType.equals(IStreamItGraphConstants.IDENTITY_FILTER_FIELD)) {
				fChildren.add(new Filter(val, streamName, getStaticId() + Integer.toString(i), factoryInst));
			} else if (streamType.equals(IStreamItGraphConstants.PIPELINE_FIELD)) {
				fChildren.add(new Pipeline(val, streamName, getStaticId() + Integer.toString(i), factoryInst));
			} else if (streamType.equals(IStreamItGraphConstants.SPLITJOIN_FIELD)) {
				fChildren.add(new SplitJoin(val, streamName, getStaticId() + Integer.toString(i), factoryInst));	
			} else if (streamType.equals(IStreamItGraphConstants.FEEDBACKLOOP_FIELD)) {
				fChildren.add(new FeedbackLoop(val, streamName, getStaticId() + Integer.toString(i), factoryInst));
			}
		}
		int numChildren = fChildren.size();
		if (numChildren == 0) return;
		
		// splitter weights
		fSplitterWeights = new Vector();
		fDuplicateSplitter = factoryInst.createWeights(splitjoinVars, true, fSplitterWeights, numChildren);

		// joiner weights
		fJoinerWeights = new Vector();
		factoryInst.createWeights(splitjoinVars, false, fJoinerWeights, numChildren);
	}

	protected void update(IValue splitjoinVal, StreamStructureModelFactory factoryInst) throws DebugException {
		IVariable[] splitjoinVars = splitjoinVal.getVariables();
		updateDynamicData(splitjoinVars, factoryInst);
		
		// update children
		IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(splitjoinVars, IStreamItGraphConstants.CHILDRENSTREAMS_FIELD), IStreamItGraphConstants.HEADER_FIELD), IStreamItGraphConstants.NEXT_FIELD);
		Vector elements = factoryInst.getLinkedListElements(vars);
		for (int i = 0; i < fChildren.size(); i++) {
			((StreamStructure) fChildren.get(i)).update((IJavaValue) ((IVariable) elements.get(i)).getValue(), factoryInst);
		}
	}	
	
	public Vector getWeights(boolean splitter) {
		if (splitter) return fSplitterWeights;
		return fJoinerWeights;
	}

	public boolean isDuplicateSplitter() {
		return fDuplicateSplitter;
	}
}