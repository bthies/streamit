
int main() {
  float coeff__6__46[[]] =  absarray1(3);
  float coeff__6__29[[]] =  absarray1(2);
  float x__0__23 = 0.0f;
  float __INTER_BUFFER_5[[]] =  absarray1(2);
  float __INTER_BUFFER__3_1[[]] =  absarray1(1);
  float __INTER_BUFFER__3_0[[]] =  absarray1(1);
  float __INTER_BUFFER_4[[]] =  absarray1(4);
  float __INTER_BUFFER_2[[]] =  absarray1(2);
  float __INTER_BUFFER_1[[]] =  absarray1(3);
  { 
/* init function calls omitted for brevity */
  }
  { 
/*SIR: Init Schedule*/
    int rr_1_1 = 0;
    int rr_1_0 = 0;
    int rr_1 = 0;
    int work_0 = 0;
/*SIRFilter name=Source__3__26_10*/
    doloop (int work_0 = 0; 3; 1) 
      {
        (__INTER_BUFFER_1[[work_0]] = x__0__23);
        (x__0__23 = (x__0__23 + ((float)1.0)));
      }
    
/*SIRSplitter:WEIGHTED_ROUND_ROBIN_Splitter_14*/
    doloop (int rr_1 = 0; 1; 1) 
      {
        doloop (int rr_1_0 = 0; 1; 1) 
          (__INTER_BUFFER_2[[((rr_1 * 1) + rr_1_0)]] = 
	   __INTER_BUFFER_1[[((rr_1 * 3) + (0 + rr_1_0))]]);

        doloop (int rr_1_1 = 0; 2; 1) 
          (__INTER_BUFFER_4[[((rr_1 * 2) + rr_1_1)]] = 
	   __INTER_BUFFER_1[[((rr_1 * 3) + (1 + rr_1_1))]]);

      }

  }

  while (1) {
/*SIR: Steady-State Schedule*/
    int work_5 = 0;
    int joiner_3_1 = 0;
    int joiner_3_0 = 0;
    int joiner_3 = 0;
    int remain_4 = 0;
    int work_4 = 0;
    int remain_2 = 0;
    int work_2 = 0;
    int rr_1_1 = 0;
    int rr_1_0 = 0;
    int rr_1 = 0;
    int work_0 = 0;
/*SIRFilter name=Source__3__26_10*/
    doloop (int work_0 = 0; 3; 1) 
      {
        (__INTER_BUFFER_1[[work_0]] = x__0__23);
        (x__0__23 = (x__0__23 + ((float)1.0)));
      }
    
/*SIRSplitter:WEIGHTED_ROUND_ROBIN_Splitter_14*/
    doloop (int rr_1 = 0; 1; 1) 
      {
        doloop (int rr_1_0 = 0; 1; 1) 
          (__INTER_BUFFER_2[[(((rr_1 * 1) + rr_1_0) + 1)]] = 
	   __INTER_BUFFER_1[[((rr_1 * 3) + (0 + rr_1_0))]]);

        doloop (int rr_1_1 = 0; 2; 1) 
          (__INTER_BUFFER_4[[(((rr_1 * 2) + rr_1_1) + 2)]] = 
	   __INTER_BUFFER_1[[((rr_1 * 3) + (1 + rr_1_1))]]);

      }
    
/*SIRFilter name=LowPassFilter__18__39_11*/
    doloop (int work_2 = 0; 1; 1) 
      {
        float sum__9__32 = 0.0f;
        float __sa0__12__33 = 0.0f;

        float __sa1__14__35 = 0.0f;

        float __sa2__10__37 = 0.0f;
        (sum__9__32 = ((float)0.0));
        doloop (int i__11__34 = 0; 2; 1) 
          {
            (__sa0__12__33 =
	     __INTER_BUFFER_2[[(i__11__34 + work_2)]]);
            (sum__9__32 = 
	     (sum__9__32 + (__sa0__12__33 * coeff__6__29[[i__11__34]])));
          }

        (__INTER_BUFFER__3_0[[work_2]] = sum__9__32);
        (__sa2__10__37 = __INTER_BUFFER_2[[work_2]]);
      }

    doloop (int remain_2 = 0; 1; 1) 
      (__INTER_BUFFER_2[[remain_2]] = 
       __INTER_BUFFER_2[[(remain_2 + 1)]]);
    
/*SIRFilter name=LowPassFilter__18__56_12*/
    doloop (int work_4 = 0; 1; 1) 
      {
        float sum__9__49 = 0.0f;
        float __sa0__12__50 = 0.0f;

        float __sa1__14__52 = 0.0f;

        float __sa2__10__54 = 0.0f;
        (sum__9__49 = ((float)0.0));
        doloop (int i__11__51 = 0; 3; 1) 
          {
            (__sa0__12__50 = 
	     __INTER_BUFFER_4[[(i__11__51 + (work_4 * 2))]]);
            (sum__9__49 = 
	     (sum__9__49 + (__sa0__12__50 * coeff__6__46[[i__11__51]])));
          }

        (__INTER_BUFFER__3_1[[work_4]] = sum__9__49);
        doloop (int i__13__53 = 0; 1; 1) {
	  (__sa1__14__52 = 
	   __INTER_BUFFER_4[[(i__13__53 + (work_4 * 2))]]);
	}
        (__sa2__10__54 = __INTER_BUFFER_4[[((work_4 * 2) + 1)]]);
      }

    doloop (int remain_4 = 0; 2; 1) 
      (__INTER_BUFFER_4[[remain_4]] = __INTER_BUFFER_4[[(remain_4 + 2)]]);
    
/*SIRJoiner:WEIGHTED_ROUND_ROBIN_Joiner_15*/
    doloop (int joiner_3 = 0; 1; 1) 
      {
        doloop (int joiner_3_0 = 0; 1; 1) 
          (__INTER_BUFFER_5[[((joiner_3 * 2) + (0 + joiner_3_0))]] = 
	   __INTER_BUFFER__3_0[[((1 * joiner_3) + joiner_3_0)]]);

        doloop (int joiner_3_1 = 0; 1; 1) 
          (__INTER_BUFFER_5[[((joiner_3 * 2) + (1 + joiner_3_1))]] = 
	   __INTER_BUFFER__3_1[[((1 * joiner_3) + joiner_3_1)]]);

      }
    
/*SIRFilter name=Sink__22__43_13*/
    doloop (int work_5 = 0; 2; 1) 
      {
        float __sa3__21__42 = 0.0f;
        (__sa3__21__42 = __INTER_BUFFER_5[[work_5]]);
        printf("%f\n", __sa3__21__42);
      }

  }
  return 0;
}
