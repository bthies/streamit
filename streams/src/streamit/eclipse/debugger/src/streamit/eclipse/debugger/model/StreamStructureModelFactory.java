package streamit.eclipse.debugger.model;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;

/**
 * @author kkuo
 */
public class StreamStructureModelFactory {

	private static StreamStructureModelFactory fInstance = new StreamStructureModelFactory();
	
	/**
	 * Creates a new StreamStructureModelFactory.
	 */
	private StreamStructureModelFactory() {
	}
	
	/**
	 * Returns the singleton StreamStructureModelFactory.
	 */
	public static StreamStructureModelFactory getInstance() {
		return fInstance;
	}
	
	public Pipeline makeStream(IVariable topLevelPipeline) throws DebugException {
		return new Pipeline(topLevelPipeline, getInstance());
	}

	public IVariable getVariable(IVariable[] vars, String name) throws DebugException {
		for (int i = 0; i < vars.length; i++) if (vars[i].getName().equals(name)) return vars[i];
		return null;
	}
	
	public IVariable[] findVariables(IVariable[] vars, String name) throws DebugException {
		IVariable var = getVariable(vars, name);
		if (var == null) return new IVariable[0];
		return var.getValue().getVariables();
	}
	
	public String getValueString(IVariable[] vars, String name) throws DebugException {
		IVariable var = getVariable(vars, name);
		if (var == null) return IStreamItGraphConstants.EMPTY_STRING;
		return var.getValue().getValueString();
	}

	public Vector getLinkedListElements(IVariable[] vars) throws DebugException {
		Vector v = new Vector();
		getLinkedListElementsHelper(vars, v);
		return v;
	}
	
	public void getLinkedListElementsHelper(IVariable[] vars, Vector v) throws DebugException {
		IVariable element = null;
		IValue next = null;
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals(IStreamItGraphConstants.ELEMENT_FIELD)) element = vars[i];
			else if (vars[i].getName().equals(IStreamItGraphConstants.NEXT_FIELD)) next = vars[i].getValue();
		}
		
		if (element != null && !element.getValue().getValueString().equals(IStreamItGraphConstants.NULL_VALUE)) {
			v.add(element);
			getLinkedListElementsHelper(next.getVariables(), v);
		}
	}
	
	protected boolean createWeights(IVariable[] streamVars, boolean splitter, Vector weights, int numChildren) throws DebugException {
		String typeField, splitterJoiner, weightField;
		if (splitter) {
			typeField = IStreamItGraphConstants.SPLITTYPE_FIELD;
			splitterJoiner = IStreamItGraphConstants.SPLITTER_FIELD;
			weightField = IStreamItGraphConstants.DEST_WEIGHT_FIELD;
		} else {
			typeField = IStreamItGraphConstants.JOINTYPE_FIELD;
			splitterJoiner = IStreamItGraphConstants.JOINER_FIELD;
			weightField = IStreamItGraphConstants.SRCS_WEIGHT_FIELD;
		}

		String type = getValueString(findVariables(streamVars, typeField), IStreamItGraphConstants.TYPE_FIELD);
		String weight;

		if (type.equals(IStreamItGraphConstants.EMPTY_STRING)) return false;
		// 1 - round robin, 2 - weighted round robin, 3 - duplicate,  4 - null
		switch (Integer.parseInt(type)) {
			case 1: // same weight to each
				weight = getValueString(findVariables(streamVars, splitterJoiner), IStreamItGraphConstants.WEIGHT_FIELD);
				for (int i = 0; i < numChildren; i++) {
					weights.add(weight);
				}
				return false;
			case 2: // different weights to children
				IVariable[] vars = findVariables(findVariables(findVariables(streamVars, splitterJoiner), weightField), IStreamItGraphConstants.ELEMENT_DATA_FIELD);
				for (int i = 0; i < vars.length; i++) {
					weight = getValueString(vars[i].getValue().getVariables(), IStreamItGraphConstants.VALUE_FIELD);
					if (weight.equals(IStreamItGraphConstants.EMPTY_STRING)) return false;
					weights.add(weight);
				}
				return false;
			case 3:
				// add blank weights
				for (int i = 0; i < numChildren; i++) {
					weights.add(IStreamItGraphConstants.EMPTY_STRING);
				}
				return true;
			default:
				return false;
		}
	}
}
