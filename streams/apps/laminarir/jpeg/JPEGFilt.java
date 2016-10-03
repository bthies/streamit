import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RGBtoYUV extends Filter // JPEGFilt.str:13
{
  public static RGBtoYUV __construct()
  {
    RGBtoYUV __obj = new RGBtoYUV();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:14
    int R = inputChannel.popInt(); // JPEGFilt.str:15
    int G = inputChannel.popInt(); // JPEGFilt.str:16
    int B = inputChannel.popInt(); // JPEGFilt.str:17
    float Y_float = (((0.299f * R) + (0.587f * G)) + (0.114f * B)); // JPEGFilt.str:18
    float U_float = (0.492f * (B - Y_float)); // JPEGFilt.str:19
    float V_float = (0.877f * (R - Y_float)); // JPEGFilt.str:20
    U_float = ((U_float + 111.18f) * (1 / 0.872f)); // JPEGFilt.str:24
    V_float = ((V_float + 131.325f) * (1 / 1.23f)); // JPEGFilt.str:25
    outputChannel.pushFloat((Y_float * 255)); // JPEGFilt.str:27
    outputChannel.pushFloat((U_float * 255)); // JPEGFilt.str:28
    outputChannel.pushFloat((V_float * 255)); // JPEGFilt.str:29
  }
  public void init() { // JPEGFilt.str:13
    setIOTypes(Integer.TYPE, Float.TYPE); // JPEGFilt.str:13
    addSteadyPhase(3, 3, 3, "work"); // JPEGFilt.str:14
  }
}
class YUVtoRGB extends Filter // JPEGFilt.str:35
{
  public static YUVtoRGB __construct()
  {
    YUVtoRGB __obj = new YUVtoRGB();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:36
    float Y = (inputChannel.popFloat() / 255); // JPEGFilt.str:37
    float U = (inputChannel.popFloat() / 255); // JPEGFilt.str:38
    float V = (inputChannel.popFloat() / 255); // JPEGFilt.str:39
    float Y_float = Y; // JPEGFilt.str:40
    float U_float = ((U * 0.872f) - 111.18f); // JPEGFilt.str:41
    float V_float = ((V * 1.23f) - 131.325f); // JPEGFilt.str:42
    float R_float = (Y_float + (1.14f * V_float)); // JPEGFilt.str:43
    float G_float = ((Y_float - (0.394f * U_float)) - (0.581f * V_float)); // JPEGFilt.str:44
    float B_float = (Y_float + (2.032f * U_float)); // JPEGFilt.str:45
    int R = ((int)(R_float)); // JPEGFilt.str:46
    int G = ((int)(G_float)); // JPEGFilt.str:47
    int B = ((int)(B_float)); // JPEGFilt.str:48
    outputChannel.pushInt(R); // JPEGFilt.str:49
    outputChannel.pushInt(G); // JPEGFilt.str:50
    outputChannel.pushInt(B); // JPEGFilt.str:51
  }
  public void init() { // JPEGFilt.str:35
    setIOTypes(Float.TYPE, Integer.TYPE); // JPEGFilt.str:35
    addSteadyPhase(3, 3, 3, "work"); // JPEGFilt.str:36
  }
}
class DCT_1D extends Filter // JPEGFilt.str:65
{
  private int __param__param_size;
  public static DCT_1D __construct(int _param_size)
  {
    DCT_1D __obj = new DCT_1D();
    __obj.__param__param_size = _param_size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_size);
  }
  int size; // JPEGFilt.str:65
  public void work() { // JPEGFilt.str:66
    float Cu; // JPEGFilt.str:67
    for (int u = 0; (u < size); u++) { // JPEGFilt.str:68
      if ((u == 0)) { // JPEGFilt.str:70
        Cu = (1 / (float)Math.sqrt(2)); // JPEGFilt.str:70
      } else { // JPEGFilt.str:72
        Cu = 1; // JPEGFilt.str:72
      } // JPEGFilt.str:69
      float tempsum; // JPEGFilt.str:73
      tempsum = 0; // JPEGFilt.str:73
      for (int x = 0; (x < size); x++) { // JPEGFilt.str:74
        tempsum += (inputChannel.peekFloat(x) * (float)Math.cos((((u * 3.141592653589793f) * ((2.0f * x) + 1)) / (2.0f * size)))); // JPEGFilt.str:75
      }; // JPEGFilt.str:74
      outputChannel.pushFloat((((1.0f / 2.0f) * Cu) * tempsum)); // JPEGFilt.str:77
    }; // JPEGFilt.str:68
    for (int x = 0; (x < size); x++) { // JPEGFilt.str:79
      inputChannel.popFloat(); // JPEGFilt.str:80
    }; // JPEGFilt.str:79
  }
  public void init(final int _param_size) { // JPEGFilt.str:65
    size = _param_size; // JPEGFilt.str:65
    setIOTypes(Float.TYPE, Float.TYPE); // JPEGFilt.str:65
    addSteadyPhase(size, size, size, "work"); // JPEGFilt.str:66
  }
}
class DCT_2D extends Pipeline // JPEGFilt.str:90
{
  private int __param_size;
  public static DCT_2D __construct(int size)
  {
    DCT_2D __obj = new DCT_2D();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // JPEGFilt.str:90
    add(helper_Parallel_8_DCT_1D_X.__construct(size)); // JPEGFilt.str:91
    add(helper_Parallel_8_DCT_1D_Y.__construct(size)); // JPEGFilt.str:92
  }
}
class iDCT_1D extends Filter // JPEGFilt.str:100
{
  private int __param__param_size;
  public static iDCT_1D __construct(int _param_size)
  {
    iDCT_1D __obj = new iDCT_1D();
    __obj.__param__param_size = _param_size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_size);
  }
  int size; // JPEGFilt.str:100
  public void work() { // JPEGFilt.str:101
    float Cu; // JPEGFilt.str:102
    for (int x = 0; (x < size); x++) { // JPEGFilt.str:103
      float tempsum; // JPEGFilt.str:104
      tempsum = 0; // JPEGFilt.str:104
      for (int u = 0; (u < size); u++) { // JPEGFilt.str:105
        if ((u == 0)) { // JPEGFilt.str:107
          Cu = (1 / (float)Math.sqrt(2)); // JPEGFilt.str:107
        } else { // JPEGFilt.str:109
          Cu = 1; // JPEGFilt.str:109
        } // JPEGFilt.str:106
        tempsum += ((Cu * inputChannel.peekFloat(u)) * (float)Math.cos((((u * 3.141592653589793f) * ((2.0f * x) + 1)) / (2.0f * size)))); // JPEGFilt.str:110
      }; // JPEGFilt.str:105
      outputChannel.pushFloat(((1.0f / 2.0f) * tempsum)); // JPEGFilt.str:112
    }; // JPEGFilt.str:103
    for (int x = 0; (x < size); x++) { // JPEGFilt.str:114
      inputChannel.popFloat(); // JPEGFilt.str:115
    }; // JPEGFilt.str:114
  }
  public void init(final int _param_size) { // JPEGFilt.str:100
    size = _param_size; // JPEGFilt.str:100
    setIOTypes(Float.TYPE, Float.TYPE); // JPEGFilt.str:100
    addSteadyPhase(size, size, size, "work"); // JPEGFilt.str:101
  }
}
class iDCT_2D extends Pipeline // JPEGFilt.str:125
{
  private int __param_size;
  public static iDCT_2D __construct(int size)
  {
    iDCT_2D __obj = new iDCT_2D();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // JPEGFilt.str:125
    add(helper_Parallel_8_iDCT_1D_X.__construct(size)); // JPEGFilt.str:126
    add(helper_Parallel_8_iDCT_1D_Y.__construct(size)); // JPEGFilt.str:127
  }
}
class helper_Parallel_8_DCT_1D_X extends SplitJoin // JPEGFilt.str:131
{
  private int __param_size;
  public static helper_Parallel_8_DCT_1D_X __construct(int size)
  {
    helper_Parallel_8_DCT_1D_X __obj = new helper_Parallel_8_DCT_1D_X();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // JPEGFilt.str:131
    setSplitter(ROUND_ROBIN(size)); // JPEGFilt.str:132
    for (int i = 0; (i < size); i++) { // JPEGFilt.str:133
      add(DCT_1D.__construct(size)); // JPEGFilt.str:134
    }; // JPEGFilt.str:133
    setJoiner(ROUND_ROBIN(size)); // JPEGFilt.str:136
  }
}
class helper_Parallel_8_DCT_1D_Y extends SplitJoin // JPEGFilt.str:140
{
  private int __param_size;
  public static helper_Parallel_8_DCT_1D_Y __construct(int size)
  {
    helper_Parallel_8_DCT_1D_Y __obj = new helper_Parallel_8_DCT_1D_Y();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // JPEGFilt.str:140
    setSplitter(ROUND_ROBIN(1)); // JPEGFilt.str:141
    for (int i = 0; (i < size); i++) { // JPEGFilt.str:142
      add(DCT_1D.__construct(size)); // JPEGFilt.str:143
    }; // JPEGFilt.str:142
    setJoiner(ROUND_ROBIN(1)); // JPEGFilt.str:145
  }
}
class helper_Parallel_8_iDCT_1D_X extends SplitJoin // JPEGFilt.str:149
{
  private int __param_size;
  public static helper_Parallel_8_iDCT_1D_X __construct(int size)
  {
    helper_Parallel_8_iDCT_1D_X __obj = new helper_Parallel_8_iDCT_1D_X();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // JPEGFilt.str:149
    setSplitter(ROUND_ROBIN(size)); // JPEGFilt.str:150
    for (int i = 0; (i < size); i++) { // JPEGFilt.str:151
      add(iDCT_1D.__construct(size)); // JPEGFilt.str:152
    }; // JPEGFilt.str:151
    setJoiner(ROUND_ROBIN(size)); // JPEGFilt.str:154
  }
}
class helper_Parallel_8_iDCT_1D_Y extends SplitJoin // JPEGFilt.str:158
{
  private int __param_size;
  public static helper_Parallel_8_iDCT_1D_Y __construct(int size)
  {
    helper_Parallel_8_iDCT_1D_Y __obj = new helper_Parallel_8_iDCT_1D_Y();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // JPEGFilt.str:158
    setSplitter(ROUND_ROBIN(1)); // JPEGFilt.str:159
    for (int i = 0; (i < size); i++) { // JPEGFilt.str:160
      add(iDCT_1D.__construct(size)); // JPEGFilt.str:161
    }; // JPEGFilt.str:160
    setJoiner(ROUND_ROBIN(1)); // JPEGFilt.str:163
  }
}
class staticQuantization extends Filter // JPEGFilt.str:171
{
  private int[] __param__param_quantizationTable;
  public static staticQuantization __construct(int[] _param_quantizationTable)
  {
    staticQuantization __obj = new staticQuantization();
    __obj.__param__param_quantizationTable = _param_quantizationTable;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_quantizationTable);
  }
  int[] quantizationTable; // JPEGFilt.str:171
  public void work() { // JPEGFilt.str:172
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:173
      float val; // JPEGFilt.str:174
      val = (inputChannel.popFloat() / quantizationTable[i]); // JPEGFilt.str:174
      outputChannel.pushInt(((int)((float)Math.round(val)))); // JPEGFilt.str:175
    }; // JPEGFilt.str:173
  }
  public void init(final int[] _param_quantizationTable) { // JPEGFilt.str:171
    quantizationTable = _param_quantizationTable; // JPEGFilt.str:171
    setIOTypes(Float.TYPE, Integer.TYPE); // JPEGFilt.str:171
    addSteadyPhase(64, 64, 64, "work"); // JPEGFilt.str:172
  }
}
class staticDeQuantization extends Filter // JPEGFilt.str:185
{
  private int[] __param__param_quantizationTable;
  public static staticDeQuantization __construct(int[] _param_quantizationTable)
  {
    staticDeQuantization __obj = new staticDeQuantization();
    __obj.__param__param_quantizationTable = _param_quantizationTable;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_quantizationTable);
  }
  int[] quantizationTable; // JPEGFilt.str:185
  public void work() { // JPEGFilt.str:186
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:187
      int val; // JPEGFilt.str:188
      val = (inputChannel.popInt() * quantizationTable[i]); // JPEGFilt.str:188
      outputChannel.pushFloat(((float)(val))); // JPEGFilt.str:189
    }; // JPEGFilt.str:187
  }
  public void init(final int[] _param_quantizationTable) { // JPEGFilt.str:185
    quantizationTable = _param_quantizationTable; // JPEGFilt.str:185
    setIOTypes(Integer.TYPE, Float.TYPE); // JPEGFilt.str:185
    addSteadyPhase(64, 64, 64, "work"); // JPEGFilt.str:186
  }
}
class staticExampleLuminanceQauntization extends Pipeline // JPEGFilt.str:194
{
  public static staticExampleLuminanceQauntization __construct()
  {
    staticExampleLuminanceQauntization __obj = new staticExampleLuminanceQauntization();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // JPEGFilt.str:194
    int[] table_ExampleLuminanceQuantizationTable = {16,11,10,16,24,40,51,61,12,12,14,19,26,58,60,55,14,13,16,24,40,57,69,56,14,17,22,29,51,87,80,62,18,22,37,56,68,109,103,77,24,35,55,64,81,104,113,92,49,64,78,87,103,121,120,101,72,92,95,98,112,100,103,99}; // JPEGFilt.str:197
    add(staticQuantization.__construct(table_ExampleLuminanceQuantizationTable)); // JPEGFilt.str:205
  }
}
class staticExampleLuminanceDeQauntization extends Pipeline // JPEGFilt.str:208
{
  public static staticExampleLuminanceDeQauntization __construct()
  {
    staticExampleLuminanceDeQauntization __obj = new staticExampleLuminanceDeQauntization();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // JPEGFilt.str:208
    int[] table_ExampleLuminanceQuantizationTable = {16,11,10,16,24,40,51,61,12,12,14,19,26,58,60,55,14,13,16,24,40,57,69,56,14,17,22,29,51,87,80,62,18,22,37,56,68,109,103,77,24,35,55,64,81,104,113,92,49,64,78,87,103,121,120,101,72,92,95,98,112,100,103,99}; // JPEGFilt.str:211
    add(staticDeQuantization.__construct(table_ExampleLuminanceQuantizationTable)); // JPEGFilt.str:219
  }
}
class BlockDCDifferenceEncoder extends SplitJoin // JPEGFilt.str:226
{
  public static BlockDCDifferenceEncoder __construct()
  {
    BlockDCDifferenceEncoder __obj = new BlockDCDifferenceEncoder();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // JPEGFilt.str:226
    setSplitter(WEIGHTED_ROUND_ROBIN(1, 63)); // JPEGFilt.str:227
    add(IntegerDifferenceEncoder.__construct()); // JPEGFilt.str:228
    add(new Identity(Integer.TYPE)); // JPEGFilt.str:229
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 63)); // JPEGFilt.str:230
  }
}
class IntegerDifferenceDecoder extends Filter // JPEGFilt.str:234
{
  public static IntegerDifferenceDecoder __construct()
  {
    IntegerDifferenceDecoder __obj = new IntegerDifferenceDecoder();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  boolean first; // JPEGFilt.str:235
  int previous; // JPEGFilt.str:236
  public void work() { // JPEGFilt.str:240
    int temp = inputChannel.popInt(); // JPEGFilt.str:241
    if (first) { // JPEGFilt.str:242
      first = false; // JPEGFilt.str:243
      outputChannel.pushInt(temp); // JPEGFilt.str:244
    } else { // JPEGFilt.str:245
      outputChannel.pushInt((temp + previous)); // JPEGFilt.str:246
    } // JPEGFilt.str:242
    previous = temp; // JPEGFilt.str:248
  }
  public void init() { // JPEGFilt.str:237
    setIOTypes(Integer.TYPE, Integer.TYPE); // JPEGFilt.str:234
    addSteadyPhase(1, 1, 1, "work"); // JPEGFilt.str:240
    first = true; // JPEGFilt.str:238
  }
}
class BlockDCDifferenceDecoder extends SplitJoin // JPEGFilt.str:253
{
  public static BlockDCDifferenceDecoder __construct()
  {
    BlockDCDifferenceDecoder __obj = new BlockDCDifferenceDecoder();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // JPEGFilt.str:253
    setSplitter(WEIGHTED_ROUND_ROBIN(1, 63)); // JPEGFilt.str:254
    add(IntegerDifferenceDecoder.__construct()); // JPEGFilt.str:255
    add(new Identity(Integer.TYPE)); // JPEGFilt.str:256
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 63)); // JPEGFilt.str:257
  }
}
class IntegerDifferenceEncoder extends Filter // JPEGFilt.str:264
{
  public static IntegerDifferenceEncoder __construct()
  {
    IntegerDifferenceEncoder __obj = new IntegerDifferenceEncoder();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  boolean first; // JPEGFilt.str:265
  int previous; // JPEGFilt.str:266
  public void work() { // JPEGFilt.str:270
    int temp = inputChannel.popInt(); // JPEGFilt.str:271
    if (first) { // JPEGFilt.str:272
      first = false; // JPEGFilt.str:273
      outputChannel.pushInt(temp); // JPEGFilt.str:274
    } else { // JPEGFilt.str:275
      outputChannel.pushInt((temp - previous)); // JPEGFilt.str:276
    } // JPEGFilt.str:272
    previous = temp; // JPEGFilt.str:278
  }
  public void init() { // JPEGFilt.str:267
    setIOTypes(Integer.TYPE, Integer.TYPE); // JPEGFilt.str:264
    addSteadyPhase(1, 1, 1, "work"); // JPEGFilt.str:270
    first = true; // JPEGFilt.str:268
  }
}
class ZigZagOrdering extends Filter // JPEGFilt.str:386
{
  public static ZigZagOrdering __construct()
  {
    ZigZagOrdering __obj = new ZigZagOrdering();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:387
    int[] Ordering = {0,1,8,16,9,2,3,10,17,24,32,25,18,11,4,5,12,19,26,33,40,48,41,34,27,20,13,6,7,14,21,28,35,42,49,56,57,50,43,36,29,22,15,23,30,37,44,51,58,59,52,45,38,31,39,46,53,60,61,54,47,55,62,63}; // JPEGFilt.str:388
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:397
      outputChannel.pushInt(inputChannel.peekInt(Ordering[i])); // JPEGFilt.str:397
    }; // JPEGFilt.str:396
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:399
      inputChannel.popInt(); // JPEGFilt.str:399
    }; // JPEGFilt.str:398
  }
  public void init() { // JPEGFilt.str:386
    setIOTypes(Integer.TYPE, Integer.TYPE); // JPEGFilt.str:386
    addSteadyPhase(64, 64, 64, "work"); // JPEGFilt.str:387
  }
}
class ZigZagUnordering extends Filter // JPEGFilt.str:405
{
  public static ZigZagUnordering __construct()
  {
    ZigZagUnordering __obj = new ZigZagUnordering();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:406
    int[] Ordering = {0,1,5,6,14,15,27,28,2,4,7,13,16,26,29,42,3,8,12,17,25,30,41,43,9,11,18,24,31,40,44,53,10,19,23,32,39,45,52,54,20,22,33,38,46,51,55,60,21,34,37,47,50,56,59,61,35,36,48,49,57,58,62,63}; // JPEGFilt.str:407
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:416
      outputChannel.pushInt(inputChannel.peekInt(Ordering[i])); // JPEGFilt.str:416
    }; // JPEGFilt.str:415
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:418
      inputChannel.popInt(); // JPEGFilt.str:418
    }; // JPEGFilt.str:417
  }
  public void init() { // JPEGFilt.str:405
    setIOTypes(Integer.TYPE, Integer.TYPE); // JPEGFilt.str:405
    addSteadyPhase(64, 64, 64, "work"); // JPEGFilt.str:406
  }
}
class RunLengthEncoder extends Filter // JPEGFilt.str:430
{
  public static RunLengthEncoder __construct()
  {
    RunLengthEncoder __obj = new RunLengthEncoder();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:431
    int i; // JPEGFilt.str:432
    int lastInt = 0; // JPEGFilt.str:433
    int count = 0; // JPEGFilt.str:434
    int push_count = 0; // JPEGFilt.str:435
    boolean starting = true; // JPEGFilt.str:436
    for (i = 0; (i < 64); i++) { // JPEGFilt.str:437
      int curPosition; // JPEGFilt.str:438
      curPosition = i; // JPEGFilt.str:438
      int curInt; // JPEGFilt.str:439
      curInt = inputChannel.peekInt(i); // JPEGFilt.str:439
      if (starting) { // JPEGFilt.str:440
        lastInt = curInt; // JPEGFilt.str:441
        count = 1; // JPEGFilt.str:442
        starting = false; // JPEGFilt.str:443
      } else { // JPEGFilt.str:444
        if ((curInt == lastInt)) { // JPEGFilt.str:445
          count++; // JPEGFilt.str:446
        } else { // JPEGFilt.str:447
          outputChannel.pushInt(count); // JPEGFilt.str:448
          push_count++; // JPEGFilt.str:449
          outputChannel.pushInt(lastInt); // JPEGFilt.str:450
          push_count++; // JPEGFilt.str:451
          lastInt = curInt; // JPEGFilt.str:452
          count = 1; // JPEGFilt.str:453
        } // JPEGFilt.str:445
      } // JPEGFilt.str:440
    }; // JPEGFilt.str:437
    outputChannel.pushInt(count); // JPEGFilt.str:457
    push_count++; // JPEGFilt.str:458
    outputChannel.pushInt(lastInt); // JPEGFilt.str:459
    push_count++; // JPEGFilt.str:460
    for (i = 0; (i < ((64 - push_count) + 1)); i++) { // JPEGFilt.str:462
      outputChannel.pushInt(0); // JPEGFilt.str:463
    }; // JPEGFilt.str:462
    for (i = 0; (i < 64); i++) { // JPEGFilt.str:466
      inputChannel.popInt(); // JPEGFilt.str:467
    }; // JPEGFilt.str:466
  }
  public void init() { // JPEGFilt.str:430
    setIOTypes(Integer.TYPE, Integer.TYPE); // JPEGFilt.str:430
    addSteadyPhase(64, 64, 64, "work"); // JPEGFilt.str:431
  }
}
class RunLengthDecoder extends Filter // JPEGFilt.str:478
{
  public static RunLengthDecoder __construct()
  {
    RunLengthDecoder __obj = new RunLengthDecoder();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:479
    int count = inputChannel.popInt(); // JPEGFilt.str:480
    int curInt = inputChannel.popInt(); // JPEGFilt.str:481
    for (int i = 0; (i < count); i++) { // JPEGFilt.str:482
      outputChannel.pushInt(curInt); // JPEGFilt.str:483
    }; // JPEGFilt.str:482
  }
  public void init() { // JPEGFilt.str:478
    setIOTypes(Integer.TYPE, Integer.TYPE); // JPEGFilt.str:478
    addSteadyPhase(2, 2, 2, "work"); // JPEGFilt.str:479
  }
}
class Int2Float extends Filter // JPEGFilt.str:537
{
  public static Int2Float __construct()
  {
    Int2Float __obj = new Int2Float();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:538
    outputChannel.pushFloat(((float)(inputChannel.popInt()))); // JPEGFilt.str:539
  }
  public void init() { // JPEGFilt.str:537
    setIOTypes(Integer.TYPE, Float.TYPE); // JPEGFilt.str:537
    addSteadyPhase(1, 1, 1, "work"); // JPEGFilt.str:538
  }
}
class Float2Int extends Filter // JPEGFilt.str:543
{
  public static Float2Int __construct()
  {
    Float2Int __obj = new Float2Int();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:544
    outputChannel.pushInt(((int)((float)Math.round(inputChannel.popFloat())))); // JPEGFilt.str:545
  }
  public void init() { // JPEGFilt.str:543
    setIOTypes(Float.TYPE, Integer.TYPE); // JPEGFilt.str:543
    addSteadyPhase(1, 1, 1, "work"); // JPEGFilt.str:544
  }
}
class Add extends Filter // JPEGFilt.str:549
{
  private float __param__param_n;
  public static Add __construct(float _param_n)
  {
    Add __obj = new Add();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  float n; // JPEGFilt.str:549
  public void work() { // JPEGFilt.str:550
    outputChannel.pushFloat((inputChannel.popFloat() + n)); // JPEGFilt.str:551
  }
  public void init(final float _param_n) { // JPEGFilt.str:549
    n = _param_n; // JPEGFilt.str:549
    setIOTypes(Float.TYPE, Float.TYPE); // JPEGFilt.str:549
    addSteadyPhase(1, 1, 1, "work"); // JPEGFilt.str:550
  }
}
class RandomSource extends Filter // JPEGFilt.str:555
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
  int seed; // JPEGFilt.str:556
  public void work() { // JPEGFilt.str:557
    outputChannel.pushInt(seed); // JPEGFilt.str:558
    seed = (((65793 * seed) + 4282663) % 8388608); // JPEGFilt.str:559
  }
  public void init() { // JPEGFilt.str:555
    seed = 0; // JPEGFilt.str:556
    setIOTypes(Void.TYPE, Integer.TYPE); // JPEGFilt.str:555
    addSteadyPhase(0, 0, 1, "work"); // JPEGFilt.str:557
  }
}
class Display8BitIntBlock extends Filter // JPEGFilt.str:587
{
  public static Display8BitIntBlock __construct()
  {
    Display8BitIntBlock __obj = new Display8BitIntBlock();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:588
    int[] x = new int[8]; // JPEGFilt.str:589
    x[0] = inputChannel.popInt(); // JPEGFilt.str:590
    x[1] = inputChannel.popInt(); // JPEGFilt.str:591
    x[2] = inputChannel.popInt(); // JPEGFilt.str:592
    x[3] = inputChannel.popInt(); // JPEGFilt.str:593
    x[4] = inputChannel.popInt(); // JPEGFilt.str:594
    x[5] = inputChannel.popInt(); // JPEGFilt.str:595
    x[6] = inputChannel.popInt(); // JPEGFilt.str:596
    x[7] = inputChannel.popInt(); // JPEGFilt.str:597
    System.out.println("NEWLINE"); // JPEGFilt.str:598
    for (int i = 0; (i < 8); i++) { // JPEGFilt.str:599
      System.out.println(x[i]); // JPEGFilt.str:600
    }; // JPEGFilt.str:599
  }
  public void init() { // JPEGFilt.str:587
    setIOTypes(Integer.TYPE, Void.TYPE); // JPEGFilt.str:587
    addSteadyPhase(8, 8, 0, "work"); // JPEGFilt.str:588
  }
}
class Display8BitFloatBlock extends Filter // JPEGFilt.str:605
{
  public static Display8BitFloatBlock __construct()
  {
    Display8BitFloatBlock __obj = new Display8BitFloatBlock();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:606
    float[] x = new float[8]; // JPEGFilt.str:607
    x[0] = inputChannel.popFloat(); // JPEGFilt.str:608
    x[1] = inputChannel.popFloat(); // JPEGFilt.str:609
    x[2] = inputChannel.popFloat(); // JPEGFilt.str:610
    x[3] = inputChannel.popFloat(); // JPEGFilt.str:611
    x[4] = inputChannel.popFloat(); // JPEGFilt.str:612
    x[5] = inputChannel.popFloat(); // JPEGFilt.str:613
    x[6] = inputChannel.popFloat(); // JPEGFilt.str:614
    x[7] = inputChannel.popFloat(); // JPEGFilt.str:615
    System.out.println("NEWLINE"); // JPEGFilt.str:616
    for (int i = 0; (i < 8); i++) { // JPEGFilt.str:617
      if (((x[i] < 1.0E-4f) && (x[i] > -1.0E-4f))) { // JPEGFilt.str:619
        System.out.println("0"); // JPEGFilt.str:619
      } else { // JPEGFilt.str:621
        System.out.println(x[i]); // JPEGFilt.str:621
      } // JPEGFilt.str:618
    }; // JPEGFilt.str:617
  }
  public void init() { // JPEGFilt.str:605
    setIOTypes(Float.TYPE, Void.TYPE); // JPEGFilt.str:605
    addSteadyPhase(8, 8, 0, "work"); // JPEGFilt.str:606
  }
}
class OutputFloat extends Filter // JPEGFilt.str:626
{
  public static OutputFloat __construct()
  {
    OutputFloat __obj = new OutputFloat();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // JPEGFilt.str:627
    float x = inputChannel.popFloat(); // JPEGFilt.str:628
    if (((x < 1.0E-4f) && (x > -1.0E-4f))) { // JPEGFilt.str:630
      System.out.println("0"); // JPEGFilt.str:630
    } else { // JPEGFilt.str:632
      System.out.println(x); // JPEGFilt.str:632
    } // JPEGFilt.str:629
  }
  public void init() { // JPEGFilt.str:626
    setIOTypes(Float.TYPE, Void.TYPE); // JPEGFilt.str:626
    addSteadyPhase(1, 1, 0, "work"); // JPEGFilt.str:627
  }
}
class ACMSample8BitBlock extends Filter // JPEGFilt.str:638
{
  public static ACMSample8BitBlock __construct()
  {
    ACMSample8BitBlock __obj = new ACMSample8BitBlock();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int[] out = {139,144,149,153,155,155,155,155,144,151,153,156,149,156,156,156,150,155,160,163,158,156,156,156,159,161,162,160,160,159,159,159,159,160,161,162,162,155,155,155,161,161,161,161,160,157,157,157,162,162,161,163,162,157,157,157,162,162,161,161,163,158,158,158}; // JPEGFilt.str:639
  public void work() { // JPEGFilt.str:647
    for (int i = 0; (i < 64); i++) { // JPEGFilt.str:648
      outputChannel.pushInt(out[i]); // JPEGFilt.str:649
    }; // JPEGFilt.str:648
  }
  public void init() { // JPEGFilt.str:638
    setIOTypes(Void.TYPE, Integer.TYPE); // JPEGFilt.str:638
    addSteadyPhase(0, 0, 64, "work"); // JPEGFilt.str:647
  }
}
public class JPEGFilt extends StreamItPipeline // JPEGFilt.str:565
{
  public void init() { // JPEGFilt.str:565
    add(RandomSource.__construct()); // JPEGFilt.str:567
    add(Int2Float.__construct()); // JPEGFilt.str:568
    add(Add.__construct(-128)); // JPEGFilt.str:569
    add(DCT_2D.__construct(8)); // JPEGFilt.str:570
    add(staticExampleLuminanceQauntization.__construct()); // JPEGFilt.str:571
    add(BlockDCDifferenceEncoder.__construct()); // JPEGFilt.str:572
    add(ZigZagOrdering.__construct()); // JPEGFilt.str:573
    add(ZigZagUnordering.__construct()); // JPEGFilt.str:577
    add(BlockDCDifferenceDecoder.__construct()); // JPEGFilt.str:578
    add(staticExampleLuminanceDeQauntization.__construct()); // JPEGFilt.str:579
    add(iDCT_2D.__construct(8)); // JPEGFilt.str:580
    add(Add.__construct(128)); // JPEGFilt.str:581
    add(Float2Int.__construct()); // JPEGFilt.str:582
    add(Display8BitIntBlock.__construct()); // JPEGFilt.str:584
  }
  public static void main(String[] args) {
    JPEGFilt program = new JPEGFilt();
    program.run(args);
    FileWriter.closeAll();
  }
}
