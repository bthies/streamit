package streamit.eclipse.debugger.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;

/**
 * @author kkuo
 */
public class Filter extends StreamStructure {

	// dynamic data
	private String fInitExecutionCount, fSteadyExecutionCount;
	private String fNumWork;
	
	public Filter(IValue filterVal, String name, String staticId, StreamStructureModelFactory factoryInst) throws DebugException {
		super(name, filterVal.getValueString(), staticId, filterVal.getVariables(), factoryInst);
		setDynamicData(filterVal.getVariables(), factoryInst);
	}
	
	protected void setDynamicData(IVariable[] filterVars, StreamStructureModelFactory factoryInst) throws DebugException {
		super.setDynamicData(filterVars, factoryInst);

		IVariable[] inputVars = factoryInst.findVariables(filterVars, IStreamItGraphConstants.INPUT_FIELD);
		IVariable[] outputVars = factoryInst.findVariables(filterVars, IStreamItGraphConstants.OUTPUT_FIELD);

		// dynamic data
		fInitExecutionCount = factoryInst.getValueString(filterVars, IStreamItGraphConstants.INIT_EXECUTION_COUNT_FIELD);
		fSteadyExecutionCount = factoryInst.getValueString(filterVars, IStreamItGraphConstants.STEADY_EXECUTION_COUNT_FIELD);
		fNumWork = IStreamItGraphConstants.ZERO_STRING;
		int numWork = 0;
		
		if (inputVars.length != 0 && !fPopRate.equals(IStreamItGraphConstants.NA_STRING)) {
			int divisor = Integer.valueOf(fPopRate).intValue();
			int totalItemsPopped = Integer.valueOf(factoryInst.getValueString(inputVars, IStreamItGraphConstants.TOTALITEMSPOPPEDCOUNT_FIELD)).intValue();
			numWork = totalItemsPopped/divisor;
			fNumWork = String.valueOf(numWork);
		}
		
		if (outputVars.length != 0 && fPushRate.equals(IStreamItGraphConstants.NA_STRING)) {
			int divisor = Integer.valueOf(fPushRate).intValue();
			int totalItemsPushed = Integer.valueOf(factoryInst.getValueString(outputVars, IStreamItGraphConstants.TOTALITEMSPUSEDCOUNT_FIELD)).intValue();
			numWork = totalItemsPushed/divisor;
			fNumWork = String.valueOf(numWork);
		}
	}
	
	protected void update(IValue pipelineVal, StreamStructureModelFactory factoryInst) throws DebugException {
		IVariable[] pipelineVars = pipelineVal.getVariables(); 
		updateDynamicData(pipelineVars, factoryInst);
	}	

	public String getInitExecutionCount() {
		return fInitExecutionCount;
	}
	
	public String getSteadyExecutionCount() {
		return fSteadyExecutionCount;
	}
	
	public String getWorkExecutions() {
		return fNumWork;
	}
}