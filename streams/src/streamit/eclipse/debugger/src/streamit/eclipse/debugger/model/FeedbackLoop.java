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
public class FeedbackLoop extends StreamStructure {
	private boolean fDuplicateSplitter;
	private Vector fSplitterWeights;	// Vector of Figures
	private Vector fJoinerWeights;		// Vector of Figures

	public FeedbackLoop(IValue feedbackloopVal, String name, String staticId, StreamStructureModelFactory factoryInst) throws DebugException {
		super(name, feedbackloopVal.getValueString(), staticId, feedbackloopVal.getVariables(), factoryInst);
		IVariable[] feedbackloopVars = feedbackloopVal.getVariables();

		Vector elements = new Vector();
		IVariable var = factoryInst.getVariable(feedbackloopVars, IStreamItGraphConstants.BODY_FIELD); 
		if (var != null) elements.add(var);
		var = factoryInst.getVariable(feedbackloopVars, IStreamItGraphConstants.LOOP_FIELD);
		if (var != null) elements.add(var);
		IJavaValue val;
		IJavaClassType type;
		String streamType, streamName;
		fChildren = new Vector();
		for (int i = 0; i < elements.size(); i++) {
			val = (IJavaValue) ((IVariable) elements.get(i)).getValue();
			type = (IJavaClassType) val.getJavaType();
			if (type == null) continue;
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

		// joiner weights
		fJoinerWeights = new Vector();
		factoryInst.createWeights(feedbackloopVars, false, fJoinerWeights, numChildren);

		// splitter weights
		fSplitterWeights = new Vector();
		fDuplicateSplitter = factoryInst.createWeights(feedbackloopVars, true, fSplitterWeights, numChildren);
	}
	
	protected void update(IValue feedbackloopVal, StreamStructureModelFactory factoryInst) throws DebugException {
		IVariable[] feedbackloopVars = feedbackloopVal.getVariables();
		updateDynamicData(feedbackloopVars, factoryInst);
		
		// update children
		Vector elements = new Vector();
		IVariable var = factoryInst.getVariable(feedbackloopVars, IStreamItGraphConstants.BODY_FIELD); 
		if (var != null) elements.add(var);
		var = factoryInst.getVariable(feedbackloopVars, IStreamItGraphConstants.LOOP_FIELD);
		if (var != null) elements.add(var);
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