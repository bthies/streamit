/** 
 * TwoWeightedRR.java - Two back-to-back splitjoins with same round_robin weights to test 
 *                      sync-removal reordering transformation 
 * 
 * TODO: 
 *   Make two SJs as separate classes and pass some args to their init 
 *   fns. Then test if you sync-removal combines those init fns properly. 
 */ 

import streamit.library.*;

class Source extends Filter {
    int i;
    public void init() {
	output = new Channel(Integer.TYPE, 1);
	i=0;
    }
    public void work() {
	output.pushInt(i++);
    }
}

class Sink extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	System.out.println(input.popInt());
    }
}

class OneToOne extends Filter {
    int scale; 
    public OneToOne(int scale) { 
        super(scale); 
    } 
    public void init(final int scale) {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
        this.scale = scale; 
    }
    public void work() {
	output.pushInt(scale * input.popInt());
    }
}

class TwoToTwo extends Pipeline {
    public TwoToTwo(int scale) 
    { 
	super(scale); 
    }   
    public void init(final int scale) {
    	this.add(new OneToOne(scale)); 
    	this.add(new OneToOne(scale)); 
    }
}

public class TwoWeightedRR extends StreamIt {

    public static void main(String[] args) {
	new TwoWeightedRR().run(args);
    }

    public void init() {
	add(new Source());
	add(new SplitJoin() {
		public void init() {
            	    int p1, p2, p3; 
            	    p1=1; p2=2; p3=3; 
		    setSplitter(WEIGHTED_ROUND_ROBIN(2,2,2));
		    this.add(new OneToOne(p1));
		    this.add(new OneToOne(p2));
		    this.add(new OneToOne(p3));
		    setJoiner(WEIGHTED_ROUND_ROBIN(2,2,2));
		} 
	    });
	add(new SplitJoin() {
		public void init() {
		    int p=9; 
		    setSplitter(WEIGHTED_ROUND_ROBIN(2,2,2));
		    this.add(new TwoToTwo(++p));
		    p = 10*p; 
		    this.add(new TwoToTwo(p));
		    p = 10*p;  
		    this.add(new TwoToTwo(p));
            	    setJoiner(WEIGHTED_ROUND_ROBIN(2,2,2));
		}
		
	    });
	add(new Sink());
    }
}

/* After SyncRemovalSJPair((SIRSplitJoin)topLevel.get(1), (SIRSplitJoin)topLevel.get(2)); 
   the diffusedSJ should look sth like this: 

   	Compilation1: Too messy and wrong!!! (not worth looking) 
	add(new SplitJoin() { 
		public void init() { 
		    /* either rename the variables inside the two inits and copy-paste here * 	
		    int p1, p2, p3; 
            	    p1=1; p2=2; p3=3; 
		    int p=9; 
		    ++p; p=10*p; p=10*p; 	
		    /* or make a fn call here to the inits without the this.add calls * 
		    init_sj1_stripped(); 
		    init_sj2_stripped(); 
		    /*** Above two is not legal because this.add() calls 
		     * can be interleaved between the parameter initialization statements and if we 
		     * move UP all parameter setup statements, we lose this interleaving and so some 
		     * some data dep can get messed up. eg: the p++ and p=10*p above.  
		     *  
		    this.setSplitter(WEIGHTED_ROUND_ROBIN(2,2,2)); 
		    this.add(new Pipeline() { 
			public void init() { 
			    this.add(new OneToOne(p1)); 
			    this.add(new TwoToTwo(p)); 
			} 
		    }); 		
		    this.add(new Pipeline() { 
			public void init() { 
			    this.add(new OneToOne(p2)); 
			    this.add(new TwoToTwo(p)); 
			} 
		    }); 		
		    this.add(new Pipeline() { 
			public void init() { 
			    this.add(new OneToOne(p3)); 
			    this.add(new TwoToTwo(p)); 
			} 
		    }); 
		}     
	}); 	

   	
	Compilation2: Somewhat neat and looks correct (but dunno how complex it will be to implement) 
	add(new SplitJoin() { 
		public void init() { 
		    /* find the arguments to the children inits * 
		    int sj1_a1, sj1_a2, sj1_a3, sj2_a1, sj2_a2, sj2_a3; 
		    /* have to rename these variables since the sj1 and sj2 variables 
		     * can get mixed up
		     * 
		    int p1, p2, p3; 
            	    p1=1; p2=2; p3=3; 
		    sj1_a1=1; sj1_a2=p2; sj1_a3=p3; 
		    int p=9; 
		    ++p; sj2_a1=p; 
		    p = 10*p; sj2_a2=p; 
		    p = 10*p; sj2_a3=p; 

		    this.setSplitter(WEIGHTED_ROUND_ROBIN(2,2,2)); 
		    this.add(new Pipeline(sj1_a1, sj2_a1) { 
			public void init() { 
			    this.add(new OneToOne(sj1_a1)); 
			    this.add(new TwoToTwo(sj2_a1)); 
			} 
		    }); 		
		    this.add(new Pipeline(sj1_a2, sj2_a2) { 
			public void init() { 
			    this.add(new OneToOne(sj1_a2)); 
			    this.add(new TwoToTwo(sj2_a2)); 
			} 
		    }); 		
		    this.add(new Pipeline(sj1_a3, sj2_a3) { 
			public void init() { 
			    this.add(new OneToOne(sj1_a3)); 
			    this.add(new TwoToTwo(sj2_a3)); 
			} 
		    }); 
		    this.setJoiner(WEIGHTED_ROUND_ROBIN(2,2,2)); 
		}     
	}); 	
	


*/ 
	
