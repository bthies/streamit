import streamit.library.*;

class Expander extends Filter {
    int M, K;

    public void init(int K, int M) {
	this.M = M;
	this.K = K;
	input = new Channel(Float.TYPE, K, M); 
	onput = new Channel(new float[M].getClass(), 1); 
    }
    public void work() {
	int i;
	float tmp[] = new float[M];
	for(i=0; i< M; i++)
	    tmp[i] = input.peekFloat(i);
	output.push1DFloat(tmp);
	for(i=0; i<K; i++)
	    input.popFloat();
    }
}

class VectSorter extends Filter {
    int M;
    public void init(int M) {
	this.M = M;
	input = new Channel(new float[M].getClass(), 1); 
	onput = new Channel(new float[M].getClass(), 1); 
    }
    public void work() {
	float tmp[];
	tmp = input.pop1DFloat();
	for(int i=0; i<M-1; i++)
	    for(int j=i+1; j<M; j++)
		if(tmp[i] > tmp[j]) {
		    int x = tmp[i];
		    tmp[i] = tmp[j];
		    tmp[j] = x;
		}
	output.push1Dfloat(tmp);
    }
}


class FindMedian extends Filter {
    int M, K;
    public void init(int K, int M) {
	this.M = M;
	this.K = K;
	input = new Channel(new float[M].getClass(), K, M); 
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float tmp[] = new float[M*M];
	for(i=0; i<M; i++)
	    for(j=0; j<M; j++)
		tmp[M*i+j] = input.peek1DFloat(i)[j];
	for(i=0; i<M*M-1; i++)
	    for(j=i+1; j<M*M; j++)
		if(tmp[i] > tmp[j]) {
		    int x = tmp[i];
		    tmp[i] = tmp[j];
		    tmp[j] = x;
		}
	Float v = tmp[0];
	int c = 1;
	Float vmax;
	int cmax = 0;
	for(i=1; i < M*M; i++)
	    if(v == tmp[i])
		c++;
	    else {
		if(c > cmax) {
		    cmax = c;
		    vmax = v;
		}
		v = tmp[i];
		c = 1;
	    }
	ouput.pushFloat(vmax);
	for(i=0; i<K; i++)
	    input.pop1DFloat();
    }
}
		
	

	

class medianFilter extends Pipeline {
    public void init(int K, int M, int N) {
	add(new Expander(K, M));
	add(new VectSorter(M));
	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    for(int i=0; i<N; i++)
			add(new FindMedian(K, M));
		    setJointer(ROUND_ROBIN());
		}
	    });
    }
}
	



class SortInput extends Filter {
    // the number to count up to
    int N;

    public SortInput(int N) {

    }

    public void init(int N) {
        this.N = N;
        output = new Channel(Integer.TYPE, N);
    }

    public void work() {
        for (int i=0; i<N; i++) {
            output.pushInt(N-i);
        }
    }
}

/**
 * Prints an integer stream.
 */
class IntPrinter extends Filter {

    public void init () {
        input = new Channel(Integer.TYPE, 1);
    }

    public void work () {
        System.out.println(input.popInt());
    }
}

/**
 * The toplevel class.  
 */
class Median extends StreamIt {

    public static void main(String args[]) {
        new MergeSort().run(args);
    }

    public void init() {
        add(new SortInput(10));
        add(new medianFilter(1, 4, 32));
        // add a printer
        add(new IntPrinter());
    }

}


	
	
		
		    
    
