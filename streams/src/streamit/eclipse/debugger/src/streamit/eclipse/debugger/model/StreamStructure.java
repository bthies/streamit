package streamit.eclipse.debugger.model;

import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;

/**
 * @author kkuo
 */
public class StreamStructure {
	
	private String fNameWithoutId, fRuntimeId, fStaticId;
	protected Channel fInputChannel, fOutputChannel; // vector of string (string rep of data)
	protected String fInputType, fOutputType;
	protected String fPopRate, fPeekRate, fPushRate;
	protected Vector fChildren; // vector of StreamStructures
	
	// dynamic data
	private String fPopped, fPushed, fMaxPeeked;
	
	public StreamStructure(String name, String runtimeId, String staticId, IVariable[] vars, StreamStructureModelFactory factoryInst) throws DebugException {
		fNameWithoutId = name;
		fRuntimeId = runtimeId;
		fStaticId = staticId;
		IVariable[] inputVars = factoryInst.findVariables(vars, IStreamItGraphConstants.INPUT_FIELD);
		IVariable[] outputVars = factoryInst.findVariables(vars, IStreamItGraphConstants.OUTPUT_FIELD);

		fInputChannel = new Channel(inputVars, true, factoryInst);
		fOutputChannel = new Channel(outputVars, false, factoryInst);
		fInputType = IStreamItGraphConstants.VOID_STRING;
		fOutputType = IStreamItGraphConstants.VOID_STRING;
		fPopRate = IStreamItGraphConstants.NA_STRING;
		fPeekRate = IStreamItGraphConstants.NA_STRING;
		fPushRate = IStreamItGraphConstants.NA_STRING;
		fChildren = new Vector();

		if (inputVars.length != 0) {
			StringTokenizer st = new StringTokenizer(factoryInst.getValueString(inputVars, IStreamItGraphConstants.TYPE_FIELD), IStreamItGraphConstants.PAREN_FIELD);
			if (st.countTokens() > 0) fInputType = st.nextToken();
			
			fPeekRate = factoryInst.getValueString(factoryInst.findVariables(inputVars, IStreamItGraphConstants.PEEKCOUNT_FIELD), IStreamItGraphConstants.VALUE_FIELD);
			fPopRate = factoryInst.getValueString(factoryInst.findVariables(inputVars, IStreamItGraphConstants.POPPUSHCOUNT_FIELD), IStreamItGraphConstants.VALUE_FIELD);
		}
		
		if (outputVars.length != 0) {
			StringTokenizer st = new StringTokenizer(factoryInst.getValueString(outputVars, IStreamItGraphConstants.TYPE_FIELD), IStreamItGraphConstants.PAREN_FIELD);
			if (st.countTokens() > 0) fOutputType = st.nextToken();
			
			fPushRate = factoryInst.getValueString(factoryInst.findVariables(outputVars, IStreamItGraphConstants.POPPUSHCOUNT_FIELD), IStreamItGraphConstants.VALUE_FIELD);
		}
		
		setDynamicData(vars, factoryInst);
	}

	protected void updateDynamicData(IVariable[] vars, StreamStructureModelFactory factoryInst) throws DebugException {
		IVariable[] inputVars = factoryInst.findVariables(vars, IStreamItGraphConstants.INPUT_FIELD);
		IVariable[] outputVars = factoryInst.findVariables(vars, IStreamItGraphConstants.OUTPUT_FIELD);
		fInputChannel.update(inputVars, factoryInst);
		fOutputChannel.update(outputVars, factoryInst);
		
		setDynamicData(vars, factoryInst);
	}
	
	protected void setDynamicData(IVariable[] vars, StreamStructureModelFactory factoryInst) throws DebugException {
		fPopped = factoryInst.getValueString(vars, IStreamItGraphConstants.POPPED_FIELD);
		fPushed = factoryInst.getValueString(vars, IStreamItGraphConstants.PUSHED_FIELD);
		fMaxPeeked = factoryInst.getValueString(vars, IStreamItGraphConstants.PEEKED_FIELD);
	}
	
	protected void update(IValue val, StreamStructureModelFactory factoryInst) throws DebugException {
	}

	public String getNameWithRuntimeId() {
		return fNameWithoutId + fRuntimeId;
	}
	
	public String getNameWithStaticId() {
		return fNameWithoutId + fStaticId;
	}
	
	public String getNameWithoutId() {
		return fNameWithoutId;
	}
	
	public String getRuntimeId() {
		return fRuntimeId;
	}
	
	public String getStaticId() {
		return fStaticId;
	}
	
	public Channel getInputChannel() {
		return fInputChannel;
	}
	
	public Channel getOutputChannel() {
		return fOutputChannel;
	}
	
	public String getInputType() {
		return fInputType;
	}
	
	public String getOutputType() {
		return fOutputType;
	}
	
	public String getPopRate() {
		return fPopRate;
	}
	
	public String getPeekRate() {
		return fPeekRate;
	}
	
	public String getPushRate() {
		return fPushRate;
	}
	
	public Vector getChildStreams() {
		return fChildren;
	}
	
	public String getMaxPeeked() {
		return fMaxPeeked;
	}

	public String getPopped() {
		return fPopped;
	}

	public String getPushed() {
		return fPushed;
	}

}