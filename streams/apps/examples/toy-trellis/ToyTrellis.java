/*
 * Simple trellis encoder/decoder system.
 */

import streamit.library.*;

class ToyTrellis extends StreamIt
{
    static public void main (String [] t)
    {
        ToyTrellis test = new ToyTrellis ();
        test.run (t);
    }

    public void init ()
    {
	//add (new DataSource());
	//add (new Shifter());	
	add (new ExampleSource());
	add (new RateDoubler());
	add (new SplitJoin() {
		public void init() {
		    setSplitter(WEIGHTED_ROUND_ROBIN(1,1));
		    this.add(new TrellisPipeline());
		    this.add(new OneToOne());
		    setJoiner(WEIGHTED_ROUND_ROBIN(1,1));
		}
	    });
	
	//add (new UnShifter());	
	//add (new CharPrinter());
	add (new DownSample());
	add (new IntPrinter());
    }
}


class TrellisPipeline extends Pipeline {
    public void init() {
	add(new TrellisEncoder());
	add(new TrellisDecoder(17));
    }
}

