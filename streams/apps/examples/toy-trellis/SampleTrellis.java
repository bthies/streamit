/*
 * Trellis Encoder/Decoder system that encodes/decodes blocks of 8 bytes.
 */

import streamit.library.*;

class SampleTrellis extends StreamIt
{
    static public void main (String [] t)
    {
        SampleTrellis test = new SampleTrellis ();
        test.run (t);
    }

    public void init ()
    {
	add (new DataSource());
	add (new FrameMaker());
	add (new Shifter());
	//add (new BinaryPasser("original data"));
	add (new NoiseSource(39));
	add (new RateDoubler());
	add (new SplitJoin() {
		public void init() {
		    setSplitter(WEIGHTED_ROUND_ROBIN(1,1));
		    this.add(new SampleTrellisPipeline());
		    this.add(new OneToOne());
		    setJoiner(WEIGHTED_ROUND_ROBIN(1,1));
		}
	    });

	add (new DownSample());
	//add (new BinaryPasser("translated data"));
	add (new UnShifter());
	add (new UnFrameMaker());
	add (new CharPrinter());
    }
}

class SampleTrellisPipeline extends Pipeline {
    public void init() {
	add(new TrellisEncoder());
	add(new TrellisDecoder(FrameMaker.FRAMESIZE * 8));
    }
}


