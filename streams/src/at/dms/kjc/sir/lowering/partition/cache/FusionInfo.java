
package at.dms.kjc.sir.lowering.partition.cache;

class FusionInfo {

    private int work_estimate;
    private int work_estimate_no_penalty;
    
    private int code_size;
    private int data_size;

    private int pop_int;
    private int peek_int;
    private int push_int;

    private int input_size;
    private int output_size;

    FusionInfo(int work_estimate, int work_estimate_no_penalty, int code_size, int data_size, int pop, int peek, int push, int input_size, int output_size) {
	this.work_estimate = work_estimate;
	this.work_estimate_no_penalty = work_estimate_no_penalty;
	this.code_size = code_size;
	this.data_size = data_size;
	pop_int = pop;
	peek_int = peek;
	push_int = push;
	this.input_size = input_size;
	this.output_size = output_size;
    }

    CCost getCost() {
	int cost = work_estimate;
	if (code_size > 16000) cost += cost/2; // ICode > L1 Instruction Cache 
	if (data_size > 10000) cost += cost/3; // Data > 2/3 of L1 Data Cache
	return new CCost(cost);
    }

    int getWorkEstimate() {
	return work_estimate;
    }

    int getWorkEstimateNoPenalty() {
	return work_estimate_no_penalty;
    }

    int getCodeSize() {
	return code_size;
    }

    int getDataSize() {
	return data_size;
    }

    int getPopInt() {
	return pop_int;
    }

    int getPeekInt() {
	return peek_int;
    }

    int getPushInt() {
	return push_int;
    }

    int getInputSize() {
	return input_size;
    }

    int getOutputSize() {
	return output_size;
    }
    
}

