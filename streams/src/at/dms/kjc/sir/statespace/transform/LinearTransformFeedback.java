package at.dms.kjc.sir.statespace.transform;

import at.dms.kjc.sir.statespace.*;
import java.util.*;

public class LinearTransformFeedback extends LinearTransform {

    LinearFilterRepresentation bodyRep, loopRep;
    int inputToBody, loopToBody, bodyToLoop, bodyToOutput;

    private LinearTransformFeedback(LinearFilterRepresentation bodyRep, LinearFilterRepresentation loopRep, int[] weights) {

	this.bodyRep = bodyRep;
	this.loopRep = loopRep;
	this.inputToBody = weights[0];
	this.loopToBody = weights[1];
	this.bodyToOutput = weights[2];
	this.bodyToLoop = weights[3];

    }

    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	
	FilterMatrix A1, B1, C1, D1, A2, B2, C2, D2;
	FilterMatrix B2_1, B2_2, C2_1, C2_2, D2_11, D2_12, D2_21, D2_22;
	int state1Count, state2Count, totalInput, totalOutput;

	A1 = loopRep.getA();
	B1 = loopRep.getB();
	C1 = loopRep.getC();
	D1 = loopRep.getD();

	A2 = bodyRep.getA();
	B2 = bodyRep.getB();
	C2 = bodyRep.getC();
	D2 = bodyRep.getD();

	totalInput = inputToBody;
	totalOutput = bodyToOutput;

	state1Count = loopRep.getStateCount();
	state2Count = bodyRep.getStateCount();

	B2_1 = new FilterMatrix(state2Count,inputToBody);
	B2_2 = new FilterMatrix(state2Count,loopToBody);
	B2_1.copyColumnsAt(0,B2,0,inputToBody);
	B2_2.copyColumnsAt(0,B2,inputToBody,loopToBody);

	C2_1 = new FilterMatrix(bodyToOutput,state2Count);
	C2_2 = new FilterMatrix(bodyToLoop,state2Count);
	C2_1.copyRowsAt(0,C2,0,bodyToOutput);
	C2_2.copyRowsAt(0,C2,bodyToOutput,bodyToLoop);

	D2_11 = new FilterMatrix(bodyToOutput,inputToBody);
	D2_12 = new FilterMatrix(bodyToOutput,loopToBody);
	D2_21 = new FilterMatrix(bodyToLoop,inputToBody);
	D2_22 = new FilterMatrix(bodyToLoop,loopToBody);
	D2_11.copyRowsAndColsAt(0,0,D2,0,0,bodyToOutput,inputToBody);
	D2_12.copyRowsAndColsAt(0,0,D2,0,inputToBody,bodyToOutput,loopToBody);
	D2_21.copyRowsAndColsAt(0,0,D2,bodyToOutput,0,bodyToLoop,inputToBody);
	D2_22.copyRowsAndColsAt(0,0,D2,bodyToOutput,inputToBody,bodyToLoop,loopToBody);

	return null;
    }

    public static LinearTransform calculateFeedback(LinearFilterRepresentation bodyRep, LinearFilterRepresentation loopRep, int[] weights) {

	int bodyInput, bodyOutput;

	bodyInput = weights[0] + weights[1];
	bodyOutput = weights[2] + weights[3];

	if(bodyInput != bodyRep.getPopCount())
	    return new LinearTransformNull("Body pop count doesn't match --> feedback is unschedulable");

	if(bodyOutput != bodyRep.getPushCount())
	    return new LinearTransformNull("Body push count doesn't match --> feedback is unschedulable");

	if(weights[3] != loopRep.getPopCount())
	    return new LinearTransformNull("Loop pop count doesn't match --> feedback is unschedulable");
	
	if(weights[1] != loopRep.getPushCount())
	    return new LinearTransformNull("Loop push count doesn't match --> feedback is unschedulable");

	return new LinearTransformFeedback(bodyRep,loopRep,weights);

    }

}

