import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class DEScoder extends Pipeline // DES.str:13
{
  private int __param_vector;
  public static DEScoder __construct(int vector)
  {
    DEScoder __obj = new DEScoder();
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector);
  }
  public void init(final int vector) { // DES.str:14
    add(doIP.__construct()); // DES.str:16
    for (int i = 0; (i < TheGlobal.__get_instance().MAXROUNDS); i++) { // DES.str:18
      add(SP_DEScoder.__construct(vector, i)); // DES.str:19
    }; // DES.str:18
    add(CrissCross.__construct()); // DES.str:21
    add(doIPm1.__construct()); // DES.str:23
  }
}
class SP_DEScoder extends SplitJoin // DES.str:25
{
  private int __param_vector;
  private int __param_i;
  public static SP_DEScoder __construct(int vector, int i)
  {
    SP_DEScoder __obj = new SP_DEScoder();
    __obj.__param_vector = vector;
    __obj.__param_i = i;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_i);
  }
  public void init(final int vector, final int i) { // DES.str:25
    setSplitter(DUPLICATE()); // DES.str:26
    add(nextR.__construct(vector, i)); // DES.str:28
    add(nextL.__construct()); // DES.str:30
    setJoiner(WEIGHTED_ROUND_ROBIN(32, 32)); // DES.str:31
  }
}
class doIP extends Filter // DES.str:34
{
  public static doIP __construct()
  {
    doIP __obj = new doIP();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:36
    for (int i = 0; (i < 64); i++) { // DES.str:37
      outputChannel.pushInt(inputChannel.peekInt((TheGlobal.__get_instance().IP[i] - 1))); // DES.str:38
    }; // DES.str:37
    for (int i = 0; (i < 64); i++) { // DES.str:40
      inputChannel.popInt(); // DES.str:41
    }; // DES.str:40
  }
  public void init() { // DES.str:34
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:34
    addSteadyPhase(64, 64, 64, "work"); // DES.str:36
  }
}
class nextL extends Pipeline // DES.str:59
{
  public static nextL __construct()
  {
    nextL __obj = new nextL();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // DES.str:59
    add(AnonFilter_a0.__construct()); // DES.str:60
  }
}
class nextR extends Pipeline // DES.str:77
{
  private int __param_vector;
  private int __param_round;
  public static nextR __construct(int vector, int round)
  {
    nextR __obj = new nextR();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:78
    add(SP_nextR.__construct(vector, round)); // DES.str:79
    add(Xor.__construct(2)); // DES.str:80
  }
}
class SP_nextR extends SplitJoin // DES.str:83
{
  private int __param_vector;
  private int __param_round;
  public static SP_nextR __construct(int vector, int round)
  {
    SP_nextR __obj = new SP_nextR();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:83
    setSplitter(WEIGHTED_ROUND_ROBIN(32, 32)); // DES.str:84
    add(f.__construct(vector, round)); // DES.str:85
    add(new Identity(Integer.TYPE)); // DES.str:86
    setJoiner(ROUND_ROBIN(1)); // DES.str:87
  }
}
class f extends Pipeline // DES.str:90
{
  private int __param_vector;
  private int __param_round;
  public static f __construct(int vector, int round)
  {
    f __obj = new f();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:91
    add(SP_f.__construct(vector, round)); // DES.str:93
    add(Xor.__construct(2)); // DES.str:94
    add(Sboxes.__construct()); // DES.str:97
    add(doP.__construct()); // DES.str:100
  }
}
class SP_f extends SplitJoin // DES.str:103
{
  private int __param_vector;
  private int __param_round;
  public static SP_f __construct(int vector, int round)
  {
    SP_f __obj = new SP_f();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:103
    setSplitter(DUPLICATE()); // DES.str:104
    add(doE.__construct()); // DES.str:105
    add(KeySchedule.__construct(vector, round)); // DES.str:106
    setJoiner(ROUND_ROBIN(1)); // DES.str:107
  }
}
class doE extends Filter // DES.str:110
{
  public static doE __construct()
  {
    doE __obj = new doE();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:112
    for (int i = 0; (i < 48); i++) { // DES.str:113
      outputChannel.pushInt(inputChannel.peekInt((TheGlobal.__get_instance().E[i] - 1))); // DES.str:114
    }; // DES.str:113
    for (int i = 0; (i < 32); i++) { // DES.str:116
      inputChannel.popInt(); // DES.str:117
    }; // DES.str:116
  }
  public void init() { // DES.str:110
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:110
    addSteadyPhase(32, 32, 48, "work"); // DES.str:112
  }
}
class doP extends Filter // DES.str:122
{
  public static doP __construct()
  {
    doP __obj = new doP();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:124
    for (int i = 31; (i >= 0); i--) { // DES.str:133
      outputChannel.pushInt(inputChannel.peekInt((32 - TheGlobal.__get_instance().P[i]))); // DES.str:134
    }; // DES.str:133
    for (int i = 0; (i < 32); i++) { // DES.str:136
      inputChannel.popInt(); // DES.str:137
    }; // DES.str:136
  }
  public void init() { // DES.str:122
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:122
    addSteadyPhase(32, 32, 32, "work"); // DES.str:124
  }
}
class doIPm1 extends Filter // DES.str:142
{
  public static doIPm1 __construct()
  {
    doIPm1 __obj = new doIPm1();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:144
    for (int i = 0; (i < 64); i++) { // DES.str:145
      outputChannel.pushInt(inputChannel.peekInt((TheGlobal.__get_instance().IPm1[i] - 1))); // DES.str:146
    }; // DES.str:145
    for (int i = 0; (i < 64); i++) { // DES.str:148
      inputChannel.popInt(); // DES.str:149
    }; // DES.str:148
  }
  public void init() { // DES.str:142
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:142
    addSteadyPhase(64, 64, 64, "work"); // DES.str:144
  }
}
class Xor extends Filter // DES.str:159
{
  private int __param__param_n;
  public static Xor __construct(int _param_n)
  {
    Xor __obj = new Xor();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  int n; // DES.str:159
  public void work() { // DES.str:161
    int _bit_x = inputChannel.popInt(); // DES.str:162
    for (int i = 1; (i < n); i++) { // DES.str:163
      int _bit_y; // DES.str:164
      _bit_y = inputChannel.popInt(); // DES.str:164
      _bit_x = (_bit_x ^ _bit_y); // DES.str:165
    }; // DES.str:163
    outputChannel.pushInt(_bit_x); // DES.str:167
  }
  public void init(final int _param_n) { // DES.str:159
    n = _param_n; // DES.str:159
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:159
    addSteadyPhase(n, n, 1, "work"); // DES.str:161
  }
}
class CrissCross extends Filter // DES.str:172
{
  public static CrissCross __construct()
  {
    CrissCross __obj = new CrissCross();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:174
    for (int i = 0; (i < 32); i++) { // DES.str:175
      outputChannel.pushInt(inputChannel.peekInt((32 + i))); // DES.str:176
    }; // DES.str:175
    for (int i = 0; (i < 32); i++) { // DES.str:178
      outputChannel.pushInt(inputChannel.popInt()); // DES.str:179
    }; // DES.str:178
    for (int i = 0; (i < 32); i++) { // DES.str:181
      inputChannel.popInt(); // DES.str:182
    }; // DES.str:181
  }
  public void init() { // DES.str:172
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:172
    addSteadyPhase((2 * 32), (2 * 32), (2 * 32), "work"); // DES.str:174
  }
}
class IntoBits extends Filter // DES.str:189
{
  public static IntoBits __construct()
  {
    IntoBits __obj = new IntoBits();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:191
    int v = inputChannel.popInt(); // DES.str:192
    int m = 1; // DES.str:193
    for (int i = 0; (i < 32); i++) { // DES.str:195
      if ((((v & m) >> i) != 0)) { // DES.str:197
        outputChannel.pushInt(1); // DES.str:197
      } else { // DES.str:199
        outputChannel.pushInt(0); // DES.str:199
      } // DES.str:196
      m = (m << 1); // DES.str:200
    }; // DES.str:195
  }
  public void init() { // DES.str:189
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:189
    addSteadyPhase(1, 1, 32, "work"); // DES.str:191
  }
}
class BitstoInts extends Filter // DES.str:207
{
  private int __param__param_n;
  public static BitstoInts __construct(int _param_n)
  {
    BitstoInts __obj = new BitstoInts();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  int n; // DES.str:207
  public void work() { // DES.str:209
    int v = 0; // DES.str:210
    for (int i = 0; (i < n); i++) { // DES.str:211
      v = (v | (inputChannel.popInt() << i)); // DES.str:212
    }; // DES.str:211
    outputChannel.pushInt(v); // DES.str:214
  }
  public void init(final int _param_n) { // DES.str:207
    n = _param_n; // DES.str:207
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:207
    addSteadyPhase(n, n, 1, "work"); // DES.str:209
  }
}
class BitSlice extends SplitJoin // DES.str:220
{
  private int __param_w;
  private int __param_b;
  public static BitSlice __construct(int w, int b)
  {
    BitSlice __obj = new BitSlice();
    __obj.__param_w = w;
    __obj.__param_b = b;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_w, __param_b);
  }
  public void init(final int w, final int b) { // DES.str:221
    setSplitter(ROUND_ROBIN(1)); // DES.str:222
    for (int l = 0; (l < b); l++) { // DES.str:223
      add(new Identity(Integer.TYPE)); // DES.str:224
    }; // DES.str:223
    setJoiner(ROUND_ROBIN(w)); // DES.str:226
  }
}
class HexPrinter extends Pipeline // DES.str:232
{
  private int __param_descriptor;
  private int __param_n;
  public static HexPrinter __construct(int descriptor, int n)
  {
    HexPrinter __obj = new HexPrinter();
    __obj.__param_descriptor = descriptor;
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_descriptor, __param_n);
  }
  public void init(final int descriptor, final int n) { // DES.str:233
    int bits = n; // DES.str:234
    int bytes = (bits / 4); // DES.str:235
    add(BitstoInts.__construct(4)); // DES.str:237
    add(F_HexPrinter.__construct(descriptor, n, bytes)); // DES.str:238
  }
}
class F_HexPrinter extends Filter // DES.str:241
{
  private int __param__param_descriptor;
  private int __param__param_n;
  private int __param__param_bytes;
  public static F_HexPrinter __construct(int _param_descriptor, int _param_n, int _param_bytes)
  {
    F_HexPrinter __obj = new F_HexPrinter();
    __obj.__param__param_descriptor = _param_descriptor;
    __obj.__param__param_n = _param_n;
    __obj.__param__param_bytes = _param_bytes;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_descriptor, __param__param_n, __param__param_bytes);
  }
  int descriptor; // DES.str:241
  int n; // DES.str:241
  int bytes; // DES.str:241
  public void work() { // DES.str:242
    if (TheGlobal.__get_instance().PRINTINFO) { // DES.str:243
      if ((descriptor == TheGlobal.__get_instance().PLAINTEXT)) { // DES.str:244
        System.out.print("P: "); // DES.str:245
      } else { // DES.str:246
        if ((descriptor == TheGlobal.__get_instance().USERKEY)) { // DES.str:246
          System.out.print("K: "); // DES.str:247
        } else { // DES.str:248
          if ((descriptor == TheGlobal.__get_instance().CIPHERTEXT)) { // DES.str:248
            System.out.print("C: "); // DES.str:249
          } // DES.str:248
        } // DES.str:246
      } // DES.str:244
    } // DES.str:243
    for (int i = (bytes - 1); (i >= 0); i--) { // DES.str:253
      int v; // DES.str:254
      v = inputChannel.peekInt(i); // DES.str:254
      if ((v < 10)) { // DES.str:255
        System.out.print(v); // DES.str:256
      } else { // DES.str:257
        if ((v == 10)) { // DES.str:257
          System.out.print("A"); // DES.str:258
        } else { // DES.str:259
          if ((v == 11)) { // DES.str:259
            System.out.print("B"); // DES.str:260
          } else { // DES.str:261
            if ((v == 12)) { // DES.str:261
              System.out.print("C"); // DES.str:262
            } else { // DES.str:263
              if ((v == 13)) { // DES.str:263
                System.out.print("D"); // DES.str:264
              } else { // DES.str:265
                if ((v == 14)) { // DES.str:265
                  System.out.print("E"); // DES.str:266
                } else { // DES.str:267
                  if ((v == 15)) { // DES.str:267
                    System.out.print("F"); // DES.str:268
                  } else { // DES.str:269
                    System.out.print("ERROR: "); // DES.str:270
                    System.out.println(v); // DES.str:271
                  } // DES.str:267
                } // DES.str:265
              } // DES.str:263
            } // DES.str:261
          } // DES.str:259
        } // DES.str:257
      } // DES.str:255
    }; // DES.str:253
    System.out.println(""); // DES.str:274
    for (int i = 0; (i < bytes); i++) { // DES.str:277
      inputChannel.popInt(); // DES.str:277
    }; // DES.str:276
  }
  public void init(final int _param_descriptor, final int _param_n, final int _param_bytes) { // DES.str:241
    descriptor = _param_descriptor; // DES.str:241
    n = _param_n; // DES.str:241
    bytes = _param_bytes; // DES.str:241
    setIOTypes(Integer.TYPE, Void.TYPE); // DES.str:241
    addSteadyPhase(bytes, bytes, 0, "work"); // DES.str:242
  }
}
class ShowIntermediate extends SplitJoin // DES.str:284
{
  private int __param_n;
  public static ShowIntermediate __construct(int n)
  {
    ShowIntermediate __obj = new ShowIntermediate();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // DES.str:285
    int bits = n; // DES.str:286
    int bytes = (bits / 4); // DES.str:287
    add(P_ShowIntermediate.__construct(n, bytes)); // DES.str:288
    setSplitter(DUPLICATE()); // DES.str:289
    add(new Identity(Integer.TYPE)); // DES.str:290
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 0)); // DES.str:291
  }
}
class P_ShowIntermediate extends Pipeline // DES.str:294
{
  private int __param_n;
  private int __param_bytes;
  public static P_ShowIntermediate __construct(int n, int bytes)
  {
    P_ShowIntermediate __obj = new P_ShowIntermediate();
    __obj.__param_n = n;
    __obj.__param_bytes = bytes;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n, __param_bytes);
  }
  public void init(final int n, final int bytes) { // DES.str:294
    add(BitstoInts.__construct(4)); // DES.str:295
    add(F_P_ShowIntermediate.__construct(n, bytes)); // DES.str:296
  }
}
class F_P_ShowIntermediate extends Filter // DES.str:299
{
  private int __param__param_n;
  private int __param__param_bytes;
  public static F_P_ShowIntermediate __construct(int _param_n, int _param_bytes)
  {
    F_P_ShowIntermediate __obj = new F_P_ShowIntermediate();
    __obj.__param__param_n = _param_n;
    __obj.__param__param_bytes = _param_bytes;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n, __param__param_bytes);
  }
  int n; // DES.str:299
  int bytes; // DES.str:299
  public void work() { // DES.str:300
    for (int i = (bytes - 1); (i >= 0); i--) { // DES.str:301
      int v; // DES.str:302
      v = inputChannel.peekInt(i); // DES.str:302
      if ((v < 10)) { // DES.str:303
        System.out.print(v); // DES.str:304
      } else { // DES.str:305
        if ((v == 10)) { // DES.str:305
          System.out.print("A"); // DES.str:306
        } else { // DES.str:307
          if ((v == 11)) { // DES.str:307
            System.out.print("B"); // DES.str:308
          } else { // DES.str:309
            if ((v == 12)) { // DES.str:309
              System.out.print("C"); // DES.str:310
            } else { // DES.str:311
              if ((v == 13)) { // DES.str:311
                System.out.print("D"); // DES.str:312
              } else { // DES.str:313
                if ((v == 14)) { // DES.str:313
                  System.out.print("E"); // DES.str:314
                } else { // DES.str:315
                  if ((v == 15)) { // DES.str:315
                    System.out.print("F"); // DES.str:316
                  } else { // DES.str:317
                    System.out.print("ERROR: "); // DES.str:318
                    System.out.println(v); // DES.str:319
                  } // DES.str:315
                } // DES.str:313
              } // DES.str:311
            } // DES.str:309
          } // DES.str:307
        } // DES.str:305
      } // DES.str:303
    }; // DES.str:301
    System.out.println(""); // DES.str:322
    for (int i = 0; (i < bytes); i++) { // DES.str:325
      inputChannel.popInt(); // DES.str:325
    }; // DES.str:324
  }
  public void init(final int _param_n, final int _param_bytes) { // DES.str:299
    n = _param_n; // DES.str:299
    bytes = _param_bytes; // DES.str:299
    setIOTypes(Integer.TYPE, Void.TYPE); // DES.str:299
    addSteadyPhase(bytes, bytes, 0, "work"); // DES.str:300
  }
}
class ShowBitStream extends Filter // DES.str:332
{
  private int __param__param_n;
  private int __param__param_w;
  public static ShowBitStream __construct(int _param_n, int _param_w)
  {
    ShowBitStream __obj = new ShowBitStream();
    __obj.__param__param_n = _param_n;
    __obj.__param__param_w = _param_w;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n, __param__param_w);
  }
  int n; // DES.str:332
  int w; // DES.str:332
  public void work() { // DES.str:334
    for (int i = (n - 1); (i >= 0); i--) { // DES.str:335
      System.out.print(inputChannel.peekInt(i)); // DES.str:336
      if (((i % w) == 0)) { // DES.str:338
        System.out.print(" "); // DES.str:338
      } // DES.str:337
    }; // DES.str:335
    System.out.println(""); // DES.str:340
    for (int i = 0; (i < n); i++) { // DES.str:342
      outputChannel.pushInt(inputChannel.popInt()); // DES.str:342
    }; // DES.str:341
  }
  public void init(final int _param_n, final int _param_w) { // DES.str:332
    n = _param_n; // DES.str:332
    w = _param_w; // DES.str:332
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:332
    addSteadyPhase(n, n, n, "work"); // DES.str:334
  }
}
class TheGlobal extends Global // DES.str:365
{
  private static TheGlobal __instance = null;
  private TheGlobal() {}
  public static TheGlobal __get_instance() {
    if (__instance == null) { __instance = new TheGlobal(); __instance.init(); }
    return __instance;
  }
  public boolean PRINTINFO; // DES.str:367
  public int PLAINTEXT; // DES.str:368
  public int USERKEY; // DES.str:369
  public int CIPHERTEXT; // DES.str:370
  public int MAXROUNDS; // DES.str:373
  public int[][] USERKEYS = {{0,0},
{-1,-1},
{805306368,0},
{286331153,286331153},
{19088743,-1985229329},
{286331153,286331153},
{0,0},
{-19088744,1985229328},
{2090930245,1243246167},
{20044129,-1648281746},
{127996734,1242244742},
{944334668,637677982},
{79238586,1140766134},
{18069872,-46861618},
{24179061,1183823334},
{1126793133,954430462},
{128390000,1171925526},
{73961732,-1023591633},
{936405941,382432582},
{520627725,448939614},
{1480598372,448422262},
{39327254,1177137159},
{1232682684,2041783695},
{1336958485,363557799},
{1240030573,1285695935},
{25366748,1083909846},
{475561756,328355823},
{16843009,16843009},
{522133279,235802126},
{-520167170,-234950146},
{0,0},
{-1,-1},
{19088743,-1985229329},
{-19088744,1985229328}
}; // DES.str:376
  public int[] PC1 = {57,49,41,33,25,17,9,1,58,50,42,34,26,18,10,2,59,51,43,35,27,19,11,3,60,52,44,36,63,55,47,39,31,23,15,7,62,54,46,38,30,22,14,6,61,53,45,37,29,21,13,5,28,20,12,4}; // DES.str:412
  public int[] PC2 = {14,17,11,24,1,5,3,28,15,6,21,10,23,19,12,4,26,8,16,7,27,20,13,2,41,52,31,37,47,55,30,40,51,45,33,48,44,49,39,56,34,53,46,42,50,36,29,32}; // DES.str:422
  public int[] RT = {1,1,2,2,2,2,2,2,1,2,2,2,2,2,2,1}; // DES.str:432
  public int[] IP = {58,50,42,34,26,18,10,2,60,52,44,36,28,20,12,4,62,54,46,38,30,22,14,6,64,56,48,40,32,24,16,8,57,49,41,33,25,17,9,1,59,51,43,35,27,19,11,3,61,53,45,37,29,21,13,5,63,55,47,39,31,23,15,7}; // DES.str:438
  public int[] E = {32,1,2,3,4,5,4,5,6,7,8,9,8,9,10,11,12,13,12,13,14,15,16,17,16,17,18,19,20,21,20,21,22,23,24,25,24,25,26,27,28,29,28,29,30,31,32,1}; // DES.str:448
  public int[] P = {16,7,20,21,29,12,28,17,1,15,23,26,5,18,31,10,2,8,24,14,32,27,3,9,19,13,30,6,22,11,4,25}; // DES.str:458
  public int[] IPm1 = {40,8,48,16,56,24,64,32,39,7,47,15,55,23,63,31,38,6,46,14,54,22,62,30,37,5,45,13,53,21,61,29,36,4,44,12,52,20,60,28,35,3,43,11,51,19,59,27,34,2,42,10,50,18,58,26,33,1,41,9,49,17,57,25}; // DES.str:468
  public int[][] S1 = {{14,4,13,1,2,15,11,8,3,10,6,12,5,9,0,7},
{0,15,7,4,14,2,13,1,10,6,12,11,9,5,3,8},
{4,1,14,8,13,6,2,11,15,12,9,7,3,10,5,0},
{15,12,8,2,4,9,1,7,5,11,3,14,10,0,6,13}
}; // DES.str:478
  public int[][] S2 = {{15,1,8,14,6,11,3,4,9,7,2,13,12,0,5,10},
{3,13,4,7,15,2,8,14,12,0,1,10,6,9,11,5},
{0,14,7,11,10,4,13,1,5,8,12,6,9,3,2,15},
{13,8,10,1,3,15,4,2,11,6,7,12,0,5,14,9}
}; // DES.str:483
  public int[][] S3 = {{10,0,9,14,6,3,15,5,1,13,12,7,11,4,2,8},
{13,7,0,9,3,4,6,10,2,8,5,14,12,11,15,1},
{13,6,4,9,8,15,3,0,11,1,2,12,5,10,14,7},
{1,10,13,0,6,9,8,7,4,15,14,3,11,5,2,12}
}; // DES.str:488
  public int[][] S4 = {{7,13,14,3,0,6,9,10,1,2,8,5,11,12,4,15},
{13,8,11,5,6,15,0,3,4,7,2,12,1,10,14,9},
{10,6,9,0,12,11,7,13,15,1,3,14,5,2,8,4},
{3,15,0,6,10,1,13,8,9,4,5,11,12,7,2,14}
}; // DES.str:493
  public int[][] S5 = {{2,12,4,1,7,10,11,6,8,5,3,15,13,0,14,9},
{14,11,2,12,4,7,13,1,5,0,15,10,3,9,8,6},
{4,2,1,11,10,13,7,8,15,9,12,5,6,3,0,14},
{11,8,12,7,1,14,2,13,6,15,0,9,10,4,5,3}
}; // DES.str:498
  public int[][] S6 = {{12,1,10,15,9,2,6,8,0,13,3,4,14,7,5,11},
{10,15,4,2,7,12,9,5,6,1,13,14,0,11,3,8},
{9,14,15,5,2,8,12,3,7,0,4,10,1,13,11,6},
{4,3,2,12,9,5,15,10,11,14,1,7,6,0,8,13}
}; // DES.str:503
  public int[][] S7 = {{4,11,2,14,15,0,8,13,3,12,9,7,5,10,6,1},
{13,0,11,7,4,9,1,10,14,3,5,12,2,15,8,6},
{1,4,11,13,12,3,7,14,10,15,6,8,0,5,9,2},
{6,11,13,8,1,4,10,7,9,5,0,15,14,2,3,12}
}; // DES.str:508
  public int[][] S8 = {{13,2,8,4,6,15,11,1,10,9,3,14,5,0,12,7},
{1,15,13,8,10,3,7,4,12,5,6,11,0,14,9,2},
{7,11,4,1,9,12,14,2,0,6,10,13,15,3,5,8},
{2,1,14,7,4,10,8,13,15,12,9,0,3,5,6,11}
}; // DES.str:513
  public void init() { // DES.str:518
    PRINTINFO = false; // DES.str:367
    PLAINTEXT = 0; // DES.str:368
    USERKEY = 1; // DES.str:369
    CIPHERTEXT = 2; // DES.str:370
    MAXROUNDS = 16; // DES.str:373
  }
}
class PlainTextSource extends Pipeline // DES.str:527
{
  private int __param_vector;
  public static PlainTextSource __construct(int vector)
  {
    PlainTextSource __obj = new PlainTextSource();
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector);
  }
  public void init(final int vector) { // DES.str:528
    int[][] TEXT = {{0,0},
{-1,-1},
{268435456,1},
{286331153,286331153},
{286331153,286331153},
{19088743,-1985229329},
{0,0},
{19088743,-1985229329},
{27383504,964126530},
{1557482664,1039095770},
{38327352,116814194},
{1363495768,769606666},
{1123894320,1498906530},
{94068232,1372525626},
{123132128,2001166802},
{1982141624,700401770},
{1004343696,1228351490},
{647323496,900685978},
{374169152,1327977010},
{1795517976,1973378250},
{4970223,152526946},
{1208826112,1860657906},
{1131757768,1770994938},
{120406944,1996968594},
{50222455,-2129137366},
{496852048,418851010},
{810889768,1836001626},
{19088743,-1985229329},
{19088743,-1985229329},
{19088743,-1985229329},
{-1,-1},
{0,0},
{0,0},
{-1,-1}
}; // DES.str:529
    add(RandomSource.__construct()); // DES.str:565
    add(IntoBits.__construct()); // DES.str:566
    if (TheGlobal.__get_instance().PRINTINFO) { // DES.str:568
      add(AnonFilter_a1.__construct(TheGlobal.__get_instance().PLAINTEXT)); // DES.str:569
    } // DES.str:568
  }
}
class RandomSource extends Filter // DES.str:578
{
  public static RandomSource __construct()
  {
    RandomSource __obj = new RandomSource();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int seed; // DES.str:579
  public void work() { // DES.str:580
    outputChannel.pushInt(seed); // DES.str:581
    seed = (((65793 * seed) + 4282663) % 8388608); // DES.str:582
  }
  public void init() { // DES.str:578
    seed = 0; // DES.str:579
    setIOTypes(Void.TYPE, Integer.TYPE); // DES.str:578
    addSteadyPhase(0, 0, 1, "work"); // DES.str:580
  }
}
class F_PlainTextSource extends Filter // DES.str:586
{
  private int __param__param_vector;
  public static F_PlainTextSource __construct(int _param_vector)
  {
    F_PlainTextSource __obj = new F_PlainTextSource();
    __obj.__param__param_vector = _param_vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_vector);
  }
  int[][] TEXT = {{0,0},
{-1,-1},
{268435456,1},
{286331153,286331153},
{286331153,286331153},
{19088743,-1985229329},
{0,0},
{19088743,-1985229329},
{27383504,964126530},
{1557482664,1039095770},
{38327352,116814194},
{1363495768,769606666},
{1123894320,1498906530},
{94068232,1372525626},
{123132128,2001166802},
{1982141624,700401770},
{1004343696,1228351490},
{647323496,900685978},
{374169152,1327977010},
{1795517976,1973378250},
{4970223,152526946},
{1208826112,1860657906},
{1131757768,1770994938},
{120406944,1996968594},
{50222455,-2129137366},
{496852048,418851010},
{810889768,1836001626},
{19088743,-1985229329},
{19088743,-1985229329},
{19088743,-1985229329},
{-1,-1},
{0,0},
{0,0},
{-1,-1}
}; // DES.str:587
  int vector; // DES.str:586
  public void work() { // DES.str:623
    outputChannel.pushInt(TEXT[vector][1]); // DES.str:624
    outputChannel.pushInt(TEXT[vector][0]); // DES.str:625
  }
  public void init(final int _param_vector) { // DES.str:586
    vector = _param_vector; // DES.str:586
    setIOTypes(Void.TYPE, Integer.TYPE); // DES.str:586
    addSteadyPhase(0, 0, 2, "work"); // DES.str:623
  }
}
class KeySchedule extends Pipeline // DES.str:636
{
  private int __param_vector;
  private int __param_round;
  public static KeySchedule __construct(int vector, int round)
  {
    KeySchedule __obj = new KeySchedule();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:637
    add(f_KeySchedule.__construct(vector, round)); // DES.str:638
    if ((TheGlobal.__get_instance().PRINTINFO && (round == 0))) { // DES.str:639
      add(SP_KeySchedule.__construct(vector, round)); // DES.str:640
    } // DES.str:639
  }
}
class SP_KeySchedule extends SplitJoin // DES.str:645
{
  private int __param_vector;
  private int __param_round;
  public static SP_KeySchedule __construct(int vector, int round)
  {
    SP_KeySchedule __obj = new SP_KeySchedule();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:645
    setSplitter(DUPLICATE()); // DES.str:646
    add(new Identity(Integer.TYPE)); // DES.str:647
    add(AnonFilter_a3.__construct(TheGlobal.__get_instance().USERKEY, 34, 2, TheGlobal.__get_instance().USERKEYS, vector)); // DES.str:648
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 0)); // DES.str:660
  }
}
class f_KeySchedule extends Filter // DES.str:663
{
  private int __param__param_vector;
  private int __param__param_round;
  public static f_KeySchedule __construct(int _param_vector, int _param_round)
  {
    f_KeySchedule __obj = new f_KeySchedule();
    __obj.__param__param_vector = _param_vector;
    __obj.__param__param_round = _param_round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_vector, __param__param_round);
  }
  int[][] keys; // DES.str:664
  int vector; // DES.str:663
  int round; // DES.str:663
  public void work() { // DES.str:717
    int i = 0; // DES.str:718
    for (i = 0; (i < 32); i++) { // DES.str:720
      inputChannel.popInt(); // DES.str:720
    }; // DES.str:719
    for (i = 0; (i < 48); i++) { // DES.str:721
      outputChannel.pushInt(keys[round][i]); // DES.str:722
    }; // DES.str:721
  }
  public void init(final int _param_vector, final int _param_round) { // DES.str:667
    vector = _param_vector; // DES.str:667
    round = _param_round; // DES.str:667
    keys = new int[TheGlobal.__get_instance().MAXROUNDS][48]; // DES.str:664
    for (int __sa8 = 0; (__sa8 < TheGlobal.__get_instance().MAXROUNDS); __sa8++) { // DES.str:664
    }; // DES.str:664
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:663
    addSteadyPhase(32, 32, 48, "work"); // DES.str:717
    int[] k64 = new int[64]; // DES.str:668
    for (int w = 1; (w >= 0); w--) { // DES.str:670
      int v; // DES.str:671
      v = TheGlobal.__get_instance().USERKEYS[vector][w]; // DES.str:671
      int m; // DES.str:672
      m = 1; // DES.str:672
      for (int i = 0; (i < 32); i++) { // DES.str:673
        if ((((v & m) >> i) != 0)) { // DES.str:675
          k64[(((1 - w) * 32) + i)] = 1; // DES.str:675
        } else { // DES.str:677
          k64[(((1 - w) * 32) + i)] = 0; // DES.str:677
        } // DES.str:674
        m = (m << 1); // DES.str:678
      }; // DES.str:673
    }; // DES.str:670
    int[] k56 = new int[56]; // DES.str:683
    for (int i = 0; (i < 56); i++) { // DES.str:684
      k56[i] = k64[(64 - TheGlobal.__get_instance().PC1[i])]; // DES.str:691
    }; // DES.str:684
    for (int r = 0; (r < TheGlobal.__get_instance().MAXROUNDS); r++) { // DES.str:694
      int[] bits; // DES.str:697
      bits = new int[56]; // DES.str:697
      for (int i = 0; (i < 28); i++) { // DES.str:699
        bits[i] = k56[((i + TheGlobal.__get_instance().RT[r]) % 28)]; // DES.str:699
      }; // DES.str:698
      for (int i = 28; (i < 56); i++) { // DES.str:701
        bits[i] = k56[(28 + ((i + TheGlobal.__get_instance().RT[r]) % 28))]; // DES.str:701
      }; // DES.str:700
      for (int i = 0; (i < 56); i++) { // DES.str:703
        k56[i] = bits[i]; // DES.str:703
      }; // DES.str:702
      for (int i = 47; (i >= 0); i--) { // DES.str:706
        keys[r][(47 - i)] = k56[(TheGlobal.__get_instance().PC2[i] - 1)]; // DES.str:712
      }; // DES.str:706
    }; // DES.str:694
  }
}
class slowKeySchedule extends Pipeline // DES.str:729
{
  private int __param_vector;
  private int __param_round;
  public static slowKeySchedule __construct(int vector, int round)
  {
    slowKeySchedule __obj = new slowKeySchedule();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // DES.str:730
    add(F_slowKeySchedule.__construct(vector, round)); // DES.str:731
    add(IntoBits.__construct()); // DES.str:733
    add(doPC1.__construct()); // DES.str:735
    for (int i = 0; (i < (round + 1)); i++) { // DES.str:737
      add(AnonFilter_a4.__construct(i)); // DES.str:738
    }; // DES.str:737
    add(doPC2.__construct()); // DES.str:748
    if ((TheGlobal.__get_instance().PRINTINFO && (round == 0))) { // DES.str:750
      add(AnonFilter_a7.__construct(TheGlobal.__get_instance().USERKEY, 34, 2, TheGlobal.__get_instance().USERKEYS, vector)); // DES.str:751
    } // DES.str:750
  }
}
class F_slowKeySchedule extends Filter // DES.str:771
{
  private int __param__param_vector;
  private int __param__param_round;
  public static F_slowKeySchedule __construct(int _param_vector, int _param_round)
  {
    F_slowKeySchedule __obj = new F_slowKeySchedule();
    __obj.__param__param_vector = _param_vector;
    __obj.__param__param_round = _param_round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_vector, __param__param_round);
  }
  int vector; // DES.str:771
  int round; // DES.str:771
  public void work() { // DES.str:772
    outputChannel.pushInt(TheGlobal.__get_instance().USERKEYS[vector][1]); // DES.str:773
    outputChannel.pushInt(TheGlobal.__get_instance().USERKEYS[vector][0]); // DES.str:774
  }
  public void init(final int _param_vector, final int _param_round) { // DES.str:771
    vector = _param_vector; // DES.str:771
    round = _param_round; // DES.str:771
    setIOTypes(Void.TYPE, Integer.TYPE); // DES.str:771
    addSteadyPhase(0, 0, 2, "work"); // DES.str:772
  }
}
class LRotate extends Filter // DES.str:781
{
  private int __param__param_round;
  public static LRotate __construct(int _param_round)
  {
    LRotate __obj = new LRotate();
    __obj.__param__param_round = _param_round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_round);
  }
  int n; // DES.str:783
  int x; // DES.str:784
  int round; // DES.str:781
  public void work() { // DES.str:786
    for (int i = 0; (i < n); i++) { // DES.str:787
      outputChannel.pushInt(inputChannel.peekInt(((i + x) % n))); // DES.str:788
    }; // DES.str:787
    for (int i = 0; (i < n); i++) { // DES.str:790
      inputChannel.popInt(); // DES.str:791
    }; // DES.str:790
  }
  public void init(final int _param_round) { // DES.str:781
    round = _param_round; // DES.str:781
    n = 28; // DES.str:783
    x = TheGlobal.__get_instance().RT[round]; // DES.str:784
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:781
    addSteadyPhase(n, n, n, "work"); // DES.str:786
  }
}
class doPC1 extends Filter // DES.str:796
{
  public static doPC1 __construct()
  {
    doPC1 __obj = new doPC1();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:798
    for (int i = 0; (i < 56); i++) { // DES.str:799
      outputChannel.pushInt(inputChannel.peekInt((64 - TheGlobal.__get_instance().PC1[i]))); // DES.str:806
    }; // DES.str:799
    for (int i = 0; (i < 64); i++) { // DES.str:808
      inputChannel.popInt(); // DES.str:809
    }; // DES.str:808
  }
  public void init() { // DES.str:796
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:796
    addSteadyPhase(64, 64, 56, "work"); // DES.str:798
  }
}
class doPC2 extends Filter // DES.str:814
{
  public static doPC2 __construct()
  {
    doPC2 __obj = new doPC2();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:816
    for (int i = 47; (i >= 0); i--) { // DES.str:822
      outputChannel.pushInt(inputChannel.peekInt((TheGlobal.__get_instance().PC2[i] - 1))); // DES.str:823
    }; // DES.str:822
    for (int i = 0; (i < 56); i++) { // DES.str:825
      inputChannel.popInt(); // DES.str:826
    }; // DES.str:825
  }
  public void init() { // DES.str:814
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:814
    addSteadyPhase(56, 56, 48, "work"); // DES.str:816
  }
}
class Sboxes extends SplitJoin // DES.str:834
{
  public static Sboxes __construct()
  {
    Sboxes __obj = new Sboxes();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // DES.str:835
    setSplitter(ROUND_ROBIN(6)); // DES.str:836
    for (int i = 1; (i <= 8); i++) { // DES.str:838
      if ((i == 1)) { // DES.str:839
        add(Sbox.__construct(TheGlobal.__get_instance().S8)); // DES.str:839
      } // DES.str:839
      if ((i == 2)) { // DES.str:840
        add(Sbox.__construct(TheGlobal.__get_instance().S7)); // DES.str:840
      } // DES.str:840
      if ((i == 3)) { // DES.str:841
        add(Sbox.__construct(TheGlobal.__get_instance().S6)); // DES.str:841
      } // DES.str:841
      if ((i == 4)) { // DES.str:842
        add(Sbox.__construct(TheGlobal.__get_instance().S5)); // DES.str:842
      } // DES.str:842
      if ((i == 5)) { // DES.str:843
        add(Sbox.__construct(TheGlobal.__get_instance().S4)); // DES.str:843
      } // DES.str:843
      if ((i == 6)) { // DES.str:844
        add(Sbox.__construct(TheGlobal.__get_instance().S3)); // DES.str:844
      } // DES.str:844
      if ((i == 7)) { // DES.str:845
        add(Sbox.__construct(TheGlobal.__get_instance().S2)); // DES.str:845
      } // DES.str:845
      if ((i == 8)) { // DES.str:846
        add(Sbox.__construct(TheGlobal.__get_instance().S1)); // DES.str:846
      } // DES.str:846
    }; // DES.str:838
    setJoiner(ROUND_ROBIN(4)); // DES.str:849
  }
}
class Sbox extends Filter // DES.str:852
{
  private int[][] __param__param_table;
  public static Sbox __construct(int[][] _param_table)
  {
    Sbox __obj = new Sbox();
    __obj.__param__param_table = _param_table;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_table);
  }
  int[][] table; // DES.str:852
  public void work() { // DES.str:854
    int r = inputChannel.popInt(); // DES.str:855
    int c = inputChannel.popInt(); // DES.str:856
    c = ((inputChannel.popInt() << 1) | c); // DES.str:857
    c = ((inputChannel.popInt() << 2) | c); // DES.str:858
    c = ((inputChannel.popInt() << 3) | c); // DES.str:859
    r = ((inputChannel.popInt() << 1) | r); // DES.str:860
    int out = table[r][c]; // DES.str:862
    outputChannel.pushInt(((int)(((out & 1) >> 0)))); // DES.str:863
    outputChannel.pushInt(((int)(((out & 2) >> 1)))); // DES.str:864
    outputChannel.pushInt(((int)(((out & 4) >> 2)))); // DES.str:865
    outputChannel.pushInt(((int)(((out & 8) >> 3)))); // DES.str:866
  }
  public void init(final int[][] _param_table) { // DES.str:852
    table = _param_table; // DES.str:852
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:852
    addSteadyPhase(6, 6, 4, "work"); // DES.str:854
  }
}
class AnonFilter_a0 extends Filter // DES.str:60
{
  public static AnonFilter_a0 __construct()
  {
    AnonFilter_a0 __obj = new AnonFilter_a0();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DES.str:61
    int i = 0; // DES.str:62
    for (i = 0; (i < 32); i++) { // DES.str:64
      outputChannel.pushInt(inputChannel.popInt()); // DES.str:64
    }; // DES.str:63
    for (i = 0; (i < 32); i++) { // DES.str:65
      inputChannel.popInt(); // DES.str:66
    }; // DES.str:65
  }
  public void init() { // DES.str:60
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:60
    addSteadyPhase(64, 64, 32, "work"); // DES.str:61
  }
}
class AnonFilter_a1 extends SplitJoin // DES.str:569
{
  private int __param_PLAINTEXT;
  public static AnonFilter_a1 __construct(int PLAINTEXT)
  {
    AnonFilter_a1 __obj = new AnonFilter_a1();
    __obj.__param_PLAINTEXT = PLAINTEXT;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_PLAINTEXT);
  }
  public void init(final int PLAINTEXT) { // DES.str:569
    setSplitter(DUPLICATE()); // DES.str:570
    add(new Identity(Integer.TYPE)); // DES.str:571
    add(HexPrinter.__construct(PLAINTEXT, 64)); // DES.str:572
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 0)); // DES.str:573
  }
}
class AnonFilter_a2 extends Filter // DES.str:649
{
  private int __param__param__len_d0_USERKEYS;
  private int __param__param__len_d1_USERKEYS;
  private int[][] __param__param_USERKEYS;
  private int __param__param_vector;
  public static AnonFilter_a2 __construct(int _param__len_d0_USERKEYS, int _param__len_d1_USERKEYS, int[][] _param_USERKEYS, int _param_vector)
  {
    AnonFilter_a2 __obj = new AnonFilter_a2();
    __obj.__param__param__len_d0_USERKEYS = _param__len_d0_USERKEYS;
    __obj.__param__param__len_d1_USERKEYS = _param__len_d1_USERKEYS;
    __obj.__param__param_USERKEYS = _param_USERKEYS;
    __obj.__param__param_vector = _param_vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param__len_d0_USERKEYS, __param__param__len_d1_USERKEYS, __param__param_USERKEYS, __param__param_vector);
  }
  int _len_d0_USERKEYS; // DES.str:649
  int _len_d1_USERKEYS; // DES.str:649
  int[][] USERKEYS; // DES.str:649
  int vector; // DES.str:649
  public void work() { // DES.str:650
    for (int i = 0; (i < 48); i++) { // DES.str:651
      inputChannel.popInt(); // DES.str:651
    }; // DES.str:651
    outputChannel.pushInt(USERKEYS[vector][1]); // DES.str:653
    outputChannel.pushInt(USERKEYS[vector][0]); // DES.str:654
  }
  public void init(final int _param__len_d0_USERKEYS, final int _param__len_d1_USERKEYS, final int[][] _param_USERKEYS, final int _param_vector) { // DES.str:649
    _len_d0_USERKEYS = _param__len_d0_USERKEYS; // DES.str:649
    _len_d1_USERKEYS = _param__len_d1_USERKEYS; // DES.str:649
    USERKEYS = _param_USERKEYS; // DES.str:649
    vector = _param_vector; // DES.str:649
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:649
    addSteadyPhase(48, 48, 2, "work"); // DES.str:650
  }
}
class AnonFilter_a3 extends Pipeline // DES.str:648
{
  private int __param_USERKEY;
  private int __param__len_d0_USERKEYS;
  private int __param__len_d1_USERKEYS;
  private int[][] __param_USERKEYS;
  private int __param_vector;
  public static AnonFilter_a3 __construct(int USERKEY, int _len_d0_USERKEYS, int _len_d1_USERKEYS, int[][] USERKEYS, int vector)
  {
    AnonFilter_a3 __obj = new AnonFilter_a3();
    __obj.__param_USERKEY = USERKEY;
    __obj.__param__len_d0_USERKEYS = _len_d0_USERKEYS;
    __obj.__param__len_d1_USERKEYS = _len_d1_USERKEYS;
    __obj.__param_USERKEYS = USERKEYS;
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_USERKEY, __param__len_d0_USERKEYS, __param__len_d1_USERKEYS, __param_USERKEYS, __param_vector);
  }
  public void init(final int USERKEY, final int _len_d0_USERKEYS, final int _len_d1_USERKEYS, final int[][] USERKEYS, final int vector) { // DES.str:648
    add(AnonFilter_a2.__construct(34, 2, USERKEYS, vector)); // DES.str:649
    add(IntoBits.__construct()); // DES.str:657
    add(HexPrinter.__construct(USERKEY, 64)); // DES.str:658
  }
}
class AnonFilter_a4 extends SplitJoin // DES.str:738
{
  private int __param_i;
  public static AnonFilter_a4 __construct(int i)
  {
    AnonFilter_a4 __obj = new AnonFilter_a4();
    __obj.__param_i = i;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_i);
  }
  public void init(final int i) { // DES.str:738
    setSplitter(WEIGHTED_ROUND_ROBIN(28, 28)); // DES.str:739
    add(LRotate.__construct(i)); // DES.str:740
    add(LRotate.__construct(i)); // DES.str:741
    setJoiner(WEIGHTED_ROUND_ROBIN(28, 28)); // DES.str:742
  }
}
class AnonFilter_a5 extends Filter // DES.str:755
{
  private int __param__param__len_d0_USERKEYS;
  private int __param__param__len_d1_USERKEYS;
  private int[][] __param__param_USERKEYS;
  private int __param__param_vector;
  public static AnonFilter_a5 __construct(int _param__len_d0_USERKEYS, int _param__len_d1_USERKEYS, int[][] _param_USERKEYS, int _param_vector)
  {
    AnonFilter_a5 __obj = new AnonFilter_a5();
    __obj.__param__param__len_d0_USERKEYS = _param__len_d0_USERKEYS;
    __obj.__param__param__len_d1_USERKEYS = _param__len_d1_USERKEYS;
    __obj.__param__param_USERKEYS = _param_USERKEYS;
    __obj.__param__param_vector = _param_vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param__len_d0_USERKEYS, __param__param__len_d1_USERKEYS, __param__param_USERKEYS, __param__param_vector);
  }
  int _len_d0_USERKEYS; // DES.str:755
  int _len_d1_USERKEYS; // DES.str:755
  int[][] USERKEYS; // DES.str:755
  int vector; // DES.str:755
  public void work() { // DES.str:756
    for (int i = 0; (i < 48); i++) { // DES.str:757
      inputChannel.popInt(); // DES.str:757
    }; // DES.str:757
    outputChannel.pushInt(USERKEYS[vector][1]); // DES.str:759
    outputChannel.pushInt(USERKEYS[vector][0]); // DES.str:760
  }
  public void init(final int _param__len_d0_USERKEYS, final int _param__len_d1_USERKEYS, final int[][] _param_USERKEYS, final int _param_vector) { // DES.str:755
    _len_d0_USERKEYS = _param__len_d0_USERKEYS; // DES.str:755
    _len_d1_USERKEYS = _param__len_d1_USERKEYS; // DES.str:755
    USERKEYS = _param_USERKEYS; // DES.str:755
    vector = _param_vector; // DES.str:755
    setIOTypes(Integer.TYPE, Integer.TYPE); // DES.str:755
    addSteadyPhase(48, 48, 2, "work"); // DES.str:756
  }
}
class AnonFilter_a6 extends Pipeline // DES.str:754
{
  private int __param_USERKEY;
  private int __param__len_d0_USERKEYS;
  private int __param__len_d1_USERKEYS;
  private int[][] __param_USERKEYS;
  private int __param_vector;
  public static AnonFilter_a6 __construct(int USERKEY, int _len_d0_USERKEYS, int _len_d1_USERKEYS, int[][] USERKEYS, int vector)
  {
    AnonFilter_a6 __obj = new AnonFilter_a6();
    __obj.__param_USERKEY = USERKEY;
    __obj.__param__len_d0_USERKEYS = _len_d0_USERKEYS;
    __obj.__param__len_d1_USERKEYS = _len_d1_USERKEYS;
    __obj.__param_USERKEYS = USERKEYS;
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_USERKEY, __param__len_d0_USERKEYS, __param__len_d1_USERKEYS, __param_USERKEYS, __param_vector);
  }
  public void init(final int USERKEY, final int _len_d0_USERKEYS, final int _len_d1_USERKEYS, final int[][] USERKEYS, final int vector) { // DES.str:754
    add(AnonFilter_a5.__construct(34, 2, USERKEYS, vector)); // DES.str:755
    add(IntoBits.__construct()); // DES.str:763
    add(HexPrinter.__construct(USERKEY, 64)); // DES.str:764
  }
}
class AnonFilter_a7 extends SplitJoin // DES.str:751
{
  private int __param_USERKEY;
  private int __param__len_d0_USERKEYS;
  private int __param__len_d1_USERKEYS;
  private int[][] __param_USERKEYS;
  private int __param_vector;
  public static AnonFilter_a7 __construct(int USERKEY, int _len_d0_USERKEYS, int _len_d1_USERKEYS, int[][] USERKEYS, int vector)
  {
    AnonFilter_a7 __obj = new AnonFilter_a7();
    __obj.__param_USERKEY = USERKEY;
    __obj.__param__len_d0_USERKEYS = _len_d0_USERKEYS;
    __obj.__param__len_d1_USERKEYS = _len_d1_USERKEYS;
    __obj.__param_USERKEYS = USERKEYS;
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_USERKEY, __param__len_d0_USERKEYS, __param__len_d1_USERKEYS, __param_USERKEYS, __param_vector);
  }
  public void init(final int USERKEY, final int _len_d0_USERKEYS, final int _len_d1_USERKEYS, final int[][] USERKEYS, final int vector) { // DES.str:751
    setSplitter(DUPLICATE()); // DES.str:752
    add(new Identity(Integer.TYPE)); // DES.str:753
    add(AnonFilter_a6.__construct(USERKEY, 34, 2, USERKEYS, vector)); // DES.str:754
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 0)); // DES.str:766
  }
}
public class DES extends StreamItPipeline // DES.str:4
{
  public void init() { // DES.str:5
    int testvector = 7; // DES.str:6
    add(PlainTextSource.__construct(testvector)); // DES.str:8
    add(DEScoder.__construct(testvector)); // DES.str:9
    add(HexPrinter.__construct(TheGlobal.__get_instance().CIPHERTEXT, 64)); // DES.str:10
  }
  public static void main(String[] args) {
    DES program = new DES();
    program.run(args);
    FileWriter.closeAll();
  }
}
