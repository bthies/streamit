import streamit.*;

class RandomSource extends Filter
{
    public void InitIO ()
    {
        output = new Channel (Float.TYPE);
    }
    
    public void Work ()
    {
        float value = (float) java.lang.Math.random ();
        output.PushFloat (value);
    }
}

class Butterfly extends Stream {
    public Butterfly (ParameterContainer params) { super (params); }
   public void Init(ParameterContainer params) {
       final int N = params.GetIntParam ("N");
       final int W = params.GetIntParam ("W");
      Add(new SplitJoin() {
         public void Init() {
            SetSplitter(WEIGHTED_ROUND_ROBIN(N, N));
            Add(new Filter() {
                public void InitIO () 
                {
                    input = new Channel(Float.TYPE);
                    output = new Channel(Float.TYPE);
                }
                float calcWeight (int i, int N, int W)
                {
                    //ASSERT (false); // must implement this function!
                    return 1;
                }
               float weights[] = new float[W];
               int curr;
               public void Init() {
                  for(int i=0; i<W; i++)
                     weights[i] = calcWeight(i, N, W);
                  curr = 0;
               }
               public void Work() {
                  output.PushFloat(input.PopFloat()*weights[curr++]);
                  if(curr>= W) curr = 0;
               }    
            });
            Add(new Identity(Float.TYPE));
            SetJoiner(ROUND_ROBIN ());
      }});
      Add(new SplitJoin() {
         public void Init() {
            SetSplitter(DUPLICATE ());
            Add(new Filter() {   
                public void InitIO ()
                {
                   input = new Channel(Float.TYPE);
                   output = new Channel(Float.TYPE);
                }
               public void Work() {
                  float val = input.PopFloat();
                  output.PushFloat(val - input.PopFloat());
               }
            });
            Add(new Filter() {   
                public void InitIO ()
                {
                   input = new Channel(Float.TYPE);
                   output = new Channel(Float.TYPE);
                }
               public void Work() {
                  float val = input.PopFloat();
                  output.PushFloat(val + input.PopFloat());
               }
            });
            SetJoiner(WEIGHTED_ROUND_ROBIN(N, N));
      }});
}}

class FFT extends Stream {
    FFT (int N) { super (N); }
   public void Init(final int K) {
       System.out.println ("K1 = " + K);
       Add (new SplitJoin() {
         public void Init() {
            System.out.println ("K2 = " + K);
            SetSplitter(WEIGHTED_ROUND_ROBIN(K/2, K/2));
            for(int i=0; i<2; i++) 
            
               Add(new SplitJoin() {
                  public void Init() {
                     SetSplitter(ROUND_ROBIN ());
                     Add(new Identity(Float.TYPE));
                     Add(new Identity(Float.TYPE));
                     SetJoiner (WEIGHTED_ROUND_ROBIN(K/4, K/4));
               }});
            SetJoiner(ROUND_ROBIN ());
      }});
      for(int i=2; i<K; i*=2)
        Add(new Butterfly(new ParameterContainer ("").Add ("N", i).Add ("W", K)));
    }
}

class FIR extends Filter {
    public FIR (int N)
    {
        super (N);
    }
    public void InitIO ()
    {
       input = new Channel(Float.TYPE);
       output = new Channel(Float.TYPE);
    }
    
   float FIR_COEFF[][];
   
   int N;
   public void Init(int N) {
      this.N = N;
   }
   public void Work() {
      float sum = 0;
      for (int i=0; i<N; i++) {
         sum += input.PeekFloat(i)*FIR_COEFF[i][N];
      }
      input.PopFloat();
      output.PushFloat(sum);
   }
}

class Booster extends Stream {
    Booster (ParameterContainer params) { super (params); }
    public void Init(ParameterContainer params) {
        int N = params.GetIntParam ("N");
        boolean adds = params.GetBoolParam ("adds");
        if (adds) Add(new FIR(N));
        else Add (new Identity (Float.TYPE));
    }
}

class RFtoIF extends Filter {
    public void InitIO ()
    {
       input = new Channel(Float.TYPE);
       output = new Channel(Float.TYPE);
    }
   int size, count;
   int CARRIER_FREQ, N;
   double PI;
   float weight[];
   
   RFtoIF (float f)
   {
       super (f);
   }
   
   public void Init(float f) {
      setf(f);
   }
   public void Work() {
      output.PushFloat(input.PopFloat()* /* BUGBUG uncomment this: weight[count++]*/ 1);
      if (count==size) count = 0;
   }
   void setf(float f) {
      count = 0;
      size = (int)(CARRIER_FREQ/f*N);
      weight = new float[size];
      for(int i=0; i<size; i++)
         weight[i] = (float) java.lang.Math.sin(i*PI/size);
   }
}

class CheckFreqHop extends SplitJoin {
   public void Init(int N) {
      SetSplitter(WEIGHTED_ROUND_ROBIN(N/4-2,1,1,N/2,1,1,N/4-2));
      int k = 0;
      for (int i=1; i<=5; i++) {
         if ((i==2)||(i==4)) {
            for (int j=0; j<2; j++) {
               Add(new Filter() {
                   public void InitIO ()
                   {
                      input = new Channel(Float.TYPE);
                      output = new Channel(Float.TYPE);
                   }
                  public void Work() {
                     float val = input.PopFloat();
                     output.PushFloat(val);
                  }
               });
               k++;
            }
         } else Add(new Identity(Float.TYPE));
      }
      SetJoiner(WEIGHTED_ROUND_ROBIN(N/4-2,1,1,N/2,1,1,N/4-2));
   }
}

class CheckQuality extends Filter {
    public void InitIO ()
    {
       input = new Channel(Float.TYPE);
       output = new Channel(Float.TYPE);
    }
   float aveHi, aveLo;
   float QUAL_BAD_THRESHOLD, QUAL_GOOD_THRESHOLD;
   
   boolean boosterOn;
   public void Init(boolean boosterOn) {
      aveHi = 0; aveLo = 1;
      this.boosterOn = boosterOn;
   }
   public void Work() {
      float val = input.PopFloat();
      aveHi = java.lang.Math.max(0.9f*aveHi, val);
      aveLo = java.lang.Math.min(1.1f*aveLo, val);
      if (aveHi - aveLo < QUAL_BAD_THRESHOLD && !boosterOn) {
         // BUGBUG - uncomment this line boosterSwitch.Init(true, BEST_EFFORT);
         boosterOn = true;
      }
      if(aveHi - aveLo > QUAL_GOOD_THRESHOLD & boosterOn) {
         // BUGBUG - uncomment this line boosterSwitch.Init(false, BEST_EFFORT);
         boosterOn = false;
      }
      output.PushFloat(val);
   }
}

class FloatPrinter extends Filter 
{
    public void InitIO ()
    {
        input = new Channel(Float.TYPE);
    }

    public void Work()
    {
        System.out.println(input.PopFloat() + ", ");
    }

}

class TrunkedRadio extends Stream {
   int N = 64;
   TrunkedRadio () { super (); }
   public void Init() {
       N = 64;
      //ReadFromAtoD in = Add(new ReadFromAtoD());
       Add (new RandomSource ());
       Add(new RFtoIF(/*STARTFREQ*/ 50));
      Add(new Booster(new ParameterContainer ("").Add ("N",N).Add ("adds", false)));
      Add(new FFT(N));
      //Add(new CheckFreqHop(freqHop));
      //Add(new CheckQuality(onOff, false));
      //AudioBackEnd out = Add(new AudioBackEnd());
      Add (new FloatPrinter());

      // MAX_LATENCY(in, out, 10);
   }
}

public class radio extends Stream
{

    // presumably some main function invokes the stream
    public static void main(String args[])
    {
    	new radio().Run();
    }
    
    // this is the defining part of the stream
    public void Init() 
    {
        Add (new TrunkedRadio ());
    }
}
