
package at.dms.kjc.sir.lowering.partition.cache;

class FusionInfo {

    private long work_estimate;
    private long work_estimate_no_penalty;
    
    private int code_size;
    private int data_size;

    private int pop_int;
    private int peek_int;
    private int push_int;

    private int input_size;
    private int output_size;

    FusionInfo(long work_estimate, long work_estimate_no_penalty, int code_size, int data_size, int pop, int peek, int push, int input_size, int output_size) {
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
        long cost = work_estimate;

        // ICode > (L1 Instruction Cache) / 0.8 

        //if (code_size > 200*1000) { // 200 Kb instruction limit

        if (code_size > (CachePartitioner.getCodeCacheSize()*10/8)) {       
            cost += cost/2; 
        }
    
        // Data > 2/3 of L1 Data Cache
        if (data_size > (CachePartitioner.getDataCacheSize()*2/3)) {
            cost += cost/2;
        }
    
        return new CCost(cost);
    }

    void addPenalty(int amount) {
        work_estimate += amount;
    }

    long getWorkEstimate() {
        return work_estimate;
    }

    long getWorkEstimateNoPenalty() {
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

