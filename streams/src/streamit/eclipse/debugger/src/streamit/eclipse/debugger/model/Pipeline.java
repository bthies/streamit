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
public class Pipeline extends StreamStructure {
	
	// for top level pipeline only!
	public Pipeline(IVariable pipelineVar, StreamStructureModelFactory factoryInst) throws DebugException {
		super(pipelineVar.getReferenceTypeName(), pipelineVar.getValue().getValueString(), IStreamItGraphConstants.ZERO_STRING, pipelineVar.getValue().getVariables(), factoryInst);
		createChildren(pipelineVar.getValue(), factoryInst);
	}
	
	public Pipeline(IValue pipelineVal, String name, String staticId, StreamStructureModelFactory factoryInst) throws DebugException {
		super(name, pipelineVal.getValueString(), staticId, pipelineVal.getVariables(), factoryInst);
		createChildren(pipelineVal, factoryInst);
	}
	
	private void createChildren(IValue pipelineVal, StreamStructureModelFactory factoryInst) throws DebugException {
		IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(pipelineVal.getVariables(), IStreamItGraphConstants.STREAMELEMENTS_FIELD), IStreamItGraphConstants.HEADER_FIELD), IStreamItGraphConstants.NEXT_FIELD);
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
	}
	
	public void update(IVariable var) throws DebugException {
		update(var.getValue(), StreamStructureModelFactory.getInstance());
	}
	
	protected void update(IValue pipelineVal, StreamStructureModelFactory factoryInst) throws DebugException {
		updateDynamicData(pipelineVal.getVariables(), factoryInst);
		
		// update children
		IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(pipelineVal.getVariables(), IStreamItGraphConstants.STREAMELEMENTS_FIELD), IStreamItGraphConstants.HEADER_FIELD), IStreamItGraphConstants.NEXT_FIELD);
		Vector elements = factoryInst.getLinkedListElements(vars);
		for (int i = 0; i < fChildren.size(); i++) {
			((StreamStructure) fChildren.get(i)).update((IJavaValue) ((IVariable) elements.get(i)).getValue(), factoryInst);
		}
	}	

}