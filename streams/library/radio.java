import streamit.*;

class Butterfly extends Stream {
   void Init(final int N, final int W) {
      Add(new SplitJoin() {
         public void Init() {
            SetSplitter(WEIGHTED_ROUND_ROBIN(N, N));
            Add(new Filter() {
                public void InitIO () 
                {
                    input = new FloatChannel();
                    output = new FloatChannel();
                }
               float weights[] = new float[W];
               int curr;
               public void Init() {
                  for(int i=0; i<W; i++)
                     weights[i] = calcWeight(i, N, W);
                  curr = 0;
               }
               public void Work() {
                  output.Push(input.Pop()*weights[curr++]);
                  if(curr>= W) curr = 0;
               }    
            });
            Add(new Identity());
            SetJoiner(ROUND_ROBIN);
      }});
      Add(new SplitJoin() {
         public void Init() {
            SetSplitter(DUPLICATE);
            Add(new Filter() {   
                public void InitIO ()
                {
                   input = new FloatChannel();
                   output = new FloatChannel();
                }
               public void Work() {
                  float val = input.Pop();
                  output.Push(val - input.Pop());
               }
            });
            Add(new Filter() {   
                public void InitIO ()
                {
                   input = new FloatChannel();
                   output = new FloatChannel();
                }
               public void Work() {
                  float val = input.Pop();
                  output.Push(val + input.Pop());
               }
            });
            SetJoiner(WEIGHTED_ROUND_ROBIN(N, N));
      }});
}}

class FFT extends Stream {
   public void Init(int N) {
      Add(new SplitJoin() {
         public void Init() {
            SetSplitter(WEIGHTED_ROUND_ROBIN(N/2, N/2));
            for(int i=0; i<2; i++) 
               Add(new SplitJoin() {
                  public void Init() {
                     SetSplitter(ROUND_ROBIN);
                     Add(new Identity());
                     Add(new Identity());
                     SetJoiner(WEIGHTED_ROUND_ROBIN(N/4, N/4));
               }});
            SetJoiner(ROUND_ROBIN);
      }});
      for(int i=2; i<N; i*=2)
        Add(new Butterfly(i, N));
    }
}

class FIR extends Filter {
    public void InitIO ()
    {
       Channel input = new FloatChannel();
       Channel output = new FloatChannel();           
    }
    
   int N;
   public void Init(int N) {
      this.N = N;
   }
   public void Work() {
      float sum = 0;
      for (int i=0; i<N; i++) {
         sum += input.Peek(i)*FIR_COEFF[i][N];
      }
      input.Pop();
      output.Push(sum);
   }
}

class Booster extends Stream {
   void Init(int N, boolean adds) {
      if (adds) add(new FIR(N));
   }
}

class RFtoIF extends Filter {
    public void InitIO ()
    {
       input = new FloatChannel();
       output = new FloatChannel();
    }
   int size, count;
   float weight[];
   void Init(float f) {
      setf(f);
   }
   public void Work() {
      output.Push(input.Pop()*weight[i++]);
      if (count==size) count = 0;
   }
   void setf(float f) {
      count = 0;
      size = CARRIER_FREQ/f*N;
      weight = new float[size];
      for(int i=0; i<size; i++)
         weight[i] = sine(i*PI/size);
   }
}

class CheckFreqHop extends SplitJoin {
   public void Init() {
      SetSplitter(WEIGHTED_ROUND_ROBIN(N/4-2,1,1,N/2,1,1,N/4-2));
      int k = 0;
      for (int i=1; i<=5; i++) {
         if ((i==2)||(i==4)) {
            for (int j=0; j<2; j++) {
               Add(new Filter() {
                   public void InitIO ()
                   {
                      input = new FloatChannel();
                      output = new FloatChannel();
                   }
                  public void Work() {
                     float val = input.Pop();
                     output.Push(val);
                  }
               });
               k++;
            }
         } else Add(new Identity());
      }
      SetJoiner(WEIGHTED_ROUND_ROBIN(N/4-2,1,1,N/2,1,1,N/4-2));
   }
}

class CheckQuality extends Filter {
    public void InitIO ()
    {
       input = new FloatChannel();
       output = new FloatChannel();
    }
   float aveHi, aveLo;
   boolean boosterOn;
   public void Init(boolean boosterOn) {
      aveHi = 0; aveLo = 1;
      this.boosterOn = boosterOn;
   }
   public void Work() {
      float val = input.Pop();
      aveHi = max(0.9*aveHi, val);
      aveLo = min(1.1*aveLo, val);
      if (aveHi - aveLo < QUAL_BAD_THRESHOLD && !booosterOn) {
         boosterSwitch.Init(true, BEST_EFFORT);
         boosterOn = true;
      }
      if(aveHi - aveLo > QUAL_GOOD_THRESHOLD & boosterOn) {
         boosterSwitch.Init(false, BEST_EFFORT);
         boosterOn = false;
      }
      output.Push(val);
   }
}

class TrunkedRadio extends Stream {
   int N = 64;
   public void Init() {
      ReadFromAtoD in = Add(new ReadFromAtoD());
      RFtoIF rf2if = Add(new RFtoIF(STARTFREQ));
      Booster iss = Add(new Booster(N, false));
      Add(new FFT(N));
      Add(new CheckFreqHop(freqHop));
      Add(new CheckQuality(onOff, false));
      AudioBackEnd out = Add(new AudioBackEnd());

      MAX_LATENCY(in, out, 10);
   }
}
