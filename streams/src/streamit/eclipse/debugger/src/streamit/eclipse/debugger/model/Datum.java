package streamit.eclipse.debugger.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaValue;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;

/**
 * @author kkuo
 */
public class Datum {
	
	private IVariable fVariable;
	private String fText;

	public Datum(IVariable var, boolean array, boolean complex, StreamStructureModelFactory factoryInst) {
		super();
		fVariable = var;
		
		// set text
		try {
			if (var == null) fText = IStreamItGraphConstants.EMPTY_STRING;
			else if (array) fText = getValueStringFromArray(var);
			else if (complex) fText = getValueStringFromComplex(var);
			else fText = var.getValue().getValueString();
		} catch (DebugException e) {
			fText = IStreamItGraphConstants.EMPTY_STRING;
		}
	}
	
	protected static String getValueStringFromArray(IVariable var) throws DebugException {
		StringBuffer array = new StringBuffer(IStreamItGraphConstants.EMPTY_STRING);
		IJavaValue[] vals = ((IJavaArray) var.getValue()).getValues();
		for (int j = 0; j < vals.length; j++) {
			array.append(IStreamItGraphConstants.LEFT_CURLY_BRACKET_CHAR);
			IVariable[] vars = vals[j].getVariables();
			for (int k = 0; k < vars.length; k++) {
				array.append(IStreamItGraphConstants.LEFT_BRACKET_CHAR + vars[k].getValue().getValueString() + IStreamItGraphConstants.RIGHT_BRACKET_CHAR);
			}
			array.append(IStreamItGraphConstants.RIGHT_CURLY_BRACKET_CHAR);
		}
		return array.toString();
	}
	
	protected static String getValueStringFromComplex(IVariable var) throws DebugException {
		StreamStructureModelFactory factoryInst = StreamStructureModelFactory.getInstance();
		IVariable[] vars = var.getValue().getVariables();
		return factoryInst.getValueString(vars, IStreamItGraphConstants.REAL_FIELD) + IStreamItGraphConstants.PLUS_CHAR + factoryInst.getValueString(vars, IStreamItGraphConstants.IMAG_FIELD) + IStreamItGraphConstants.I_CHAR;
	}
	
	public boolean verify(Channel model, String input) throws DebugException {
		if (model.isArray()) return verifyArray(input);
		else if (model.isComplex()) return verifyComplex(input);
		else return fVariable.verifyValue(input);
	}
	
	private boolean verifyArray(String array) throws DebugException {
		IJavaValue[] vals = ((IJavaArray) fVariable.getValue()).getValues();
		int index = 0;
		int verifyStop;
		for (int j = 0; j < vals.length; j++) {
			if (array.charAt(index) != IStreamItGraphConstants.LEFT_CURLY_BRACKET_CHAR) return false;
			index++;
			IVariable[] vars = vals[j].getVariables();
			for (int k = 0; k < vars.length; k++) {
				if (array.charAt(index) != IStreamItGraphConstants.LEFT_BRACKET_CHAR) return false;
				index++;
				
				verifyStop = array.indexOf(IStreamItGraphConstants.RIGHT_BRACKET_CHAR, index);
				if (!vars[k].verifyValue(array.substring(index, verifyStop))) return false;
				index = verifyStop;

				if (array.charAt(index) != IStreamItGraphConstants.RIGHT_BRACKET_CHAR) return false;
				index++;
			}
			if (array.charAt(index) != IStreamItGraphConstants.RIGHT_CURLY_BRACKET_CHAR) return false;
			index++;
		}
		return true;
	}
	
	private boolean verifyComplex(String complex) throws DebugException {	
		StreamStructureModelFactory factoryInst = StreamStructureModelFactory.getInstance();
		IVariable[] vars = fVariable.getValue().getVariables();
		
		int plus = complex.indexOf(IStreamItGraphConstants.PLUS_CHAR);
		int i = complex.indexOf(IStreamItGraphConstants.I_CHAR);
		if (plus == -1 || i == -1) return false;
		String real = complex.substring(0, plus);
		String imag = complex.substring(plus + 1, i);
		
		IVariable v = factoryInst.getVariable(vars, IStreamItGraphConstants.REAL_FIELD);
		if (v == null || !v.verifyValue(real)) return false;
		v = factoryInst.getVariable(vars, IStreamItGraphConstants.IMAG_FIELD);
		if (v == null || !v.verifyValue(imag)) return false;
		
		return true;
	}
	
	public void update(Channel model, String input) throws DebugException {
		if (model.isArray()) updateArray(input);
		else if (model.isComplex()) updateComplex(input);
		else fVariable.setValue(input);
		fText = input;
	}
	
	private void updateArray(String array) throws DebugException {
		IJavaValue[] vals = ((IJavaArray) fVariable.getValue()).getValues();
		int index = 0;
		int updateStop;
		
		for (int j = 0; j < vals.length; j++) {
			index++;
			IVariable[] vars = vals[j].getVariables();
			for (int k = 0; k < vars.length; k++) {
				index++;
				
				updateStop = array.indexOf(IStreamItGraphConstants.RIGHT_BRACKET_CHAR, index);
				vars[k].setValue(array.substring(index, updateStop));
				index = updateStop;

				index++;
			}
			index++;
		}
	}
	
	private void updateComplex(String complex) throws DebugException {
		StreamStructureModelFactory factoryInst = StreamStructureModelFactory.getInstance();
		IVariable[] vars = fVariable.getValue().getVariables();
		
		int plus = complex.indexOf(IStreamItGraphConstants.PLUS_CHAR);
		int i = complex.indexOf(IStreamItGraphConstants.I_CHAR);
		if (plus == -1 || i == -1) return;
		String real = complex.substring(0, plus);
		String imag = complex.substring(plus + 1, i);
		
		IVariable v = factoryInst.getVariable(vars, IStreamItGraphConstants.REAL_FIELD); 
		if (v != null) v.setValue(real);
		v = factoryInst.getVariable(vars, IStreamItGraphConstants.IMAG_FIELD);
		if (v != null) v.setValue(imag);
	}

	public String getText() {
		return fText;
	}

	public IVariable getVariable() {
		return fVariable;
	}
}