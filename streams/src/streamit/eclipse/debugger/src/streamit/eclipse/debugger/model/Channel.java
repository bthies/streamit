package streamit.eclipse.debugger.model;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaArray;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;

/**
 * @author kkuo
 */
public class Channel {
	private Vector fQueue; 		// vector of Datum
	private boolean fArray; 	// data in channel is type array
	private boolean fComplex; 	// data in channel is type complex
	private boolean fInput; 	// if input channel
	private boolean fVoid; 		// if channel is void
	
	public Channel(IVariable[] vars, boolean input, StreamStructureModelFactory factoryInst) throws DebugException {
		
		fInput = input;
			
		// channel is void
		if (vars.length == 0) {
			fVoid = true;
			fArray = false;
			fComplex = false;
			return;
		}
		fVoid = false;
		fArray = false;
		fComplex = false;
		
		update(vars, factoryInst);
	}
	
	public void update(IVariable[] vars, StreamStructureModelFactory factoryInst) throws DebugException {
		if (fVoid) return;
		
		// get contents
		vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(vars, IStreamItGraphConstants.QUEUE_FIELD), IStreamItGraphConstants.HEADER_FIELD), IStreamItGraphConstants.NEXT_FIELD);
		Vector v = factoryInst.getLinkedListElements(vars);

		// calculate size
		int realSize = v.size();

		// create contents
		fQueue = new Vector();
		IVariable var;
		IValue val;
		Datum datum;
		int childrenHeight = 0;
		for (int i = 0; i < realSize; i++) {
			// get IVariable
			var = (IVariable) v.get(i);
			val = var.getValue();
			if (i == 0) {
				if (val instanceof IJavaArray) fArray = true;
				else if (val.getReferenceTypeName().equals(IStreamItGraphConstants.COMPLEX_FIELD)) fComplex = true;
			}			
			if (!fArray && !fComplex)
				var = factoryInst.getVariable(val.getVariables(), IStreamItGraphConstants.VALUE_FIELD);

			// add datum
			datum = new Datum(var, fArray, fComplex, factoryInst);
			fQueue.add(datum);
		}
	}
	
	public Vector getQueue() {
		return fQueue;
	}
	
	public boolean isArray() {
		return fArray;
	}
	
	public boolean isComplex() {
		return fComplex;
	}
	
	public boolean isInput() {
		return fInput;
	}
	
	public boolean isVoid() {
		return fVoid;
	}
}