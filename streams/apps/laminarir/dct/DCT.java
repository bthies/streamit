import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class Source extends Filter // DCT.str:50
{
  public static Source __construct()
  {
    Source __obj = new Source();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DCT.str:52
    for (int i = 0; (i < 16); i++) { // DCT.str:54
      outputChannel.pushInt(i); // DCT.str:54
    }; // DCT.str:53
  }
  public void init() { // DCT.str:50
    setIOTypes(Void.TYPE, Integer.TYPE); // DCT.str:50
    addSteadyPhase(0, 0, 16, "work"); // DCT.str:52
  }
}
class FloatPrinter extends Filter // DCT.str:59
{
  public static FloatPrinter __construct()
  {
    FloatPrinter __obj = new FloatPrinter();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DCT.str:61
    int x; // DCT.str:62
    for (int i = 0; (i < 16); i++) { // DCT.str:63
      x = inputChannel.popInt(); // DCT.str:64
      System.out.println(x); // DCT.str:65
    }; // DCT.str:63
  }
  public void init() { // DCT.str:59
    setIOTypes(Integer.TYPE, Void.TYPE); // DCT.str:59
    addSteadyPhase(16, 16, 0, "work"); // DCT.str:61
  }
}
class RandomSource extends Filter // DCT.str:72
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
  int seed; // DCT.str:73
  public void work() { // DCT.str:74
    outputChannel.pushInt(seed); // DCT.str:75
    seed = (((65793 * seed) + 4282663) % 8388608); // DCT.str:76
  }
  public void init() { // DCT.str:72
    seed = 0; // DCT.str:73
    setIOTypes(Void.TYPE, Integer.TYPE); // DCT.str:72
    addSteadyPhase(0, 0, 1, "work"); // DCT.str:74
  }
}
class iDCT8x8_ieee extends Pipeline // DCT.str:101
{
  private int __param_mode;
  public static iDCT8x8_ieee __construct(int mode)
  {
    iDCT8x8_ieee __obj = new iDCT8x8_ieee();
    __obj.__param_mode = mode;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_mode);
  }
  public void init(final int mode) { // DCT.str:101
    if ((mode == 0)) { // DCT.str:108
      add(iDCT_2D_reference_coarse.__construct(8)); // DCT.str:108
    } else { // DCT.str:109
      if ((mode == 1)) { // DCT.str:110
        add(iDCT_2D_reference_fine.__construct(8)); // DCT.str:110
      } else { // DCT.str:111
        if ((mode == 2)) { // DCT.str:112
          add(iDCT8x8_2D_fast_coarse.__construct()); // DCT.str:112
        } else { // DCT.str:114
          add(iDCT8x8_2D_fast_fine.__construct()); // DCT.str:114
        } // DCT.str:111
      } // DCT.str:109
    } // DCT.str:107
  }
}
class DCT8x8_ieee extends Pipeline // DCT.str:130
{
  private int __param_mode;
  public static DCT8x8_ieee __construct(int mode)
  {
    DCT8x8_ieee __obj = new DCT8x8_ieee();
    __obj.__param_mode = mode;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_mode);
  }
  public void init(final int mode) { // DCT.str:130
    if ((mode == 0)) { // DCT.str:135
      add(DCT_2D_reference_coarse.__construct(8)); // DCT.str:135
    } else { // DCT.str:137
      add(DCT_2D_reference_fine.__construct(8)); // DCT.str:137
    } // DCT.str:134
  }
}
class iDCT_2D_reference_fine extends Pipeline // DCT.str:150
{
  private int __param_size;
  public static iDCT_2D_reference_fine __construct(int size)
  {
    iDCT_2D_reference_fine __obj = new iDCT_2D_reference_fine();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // DCT.str:150
    add(AnonFilter_a0.__construct()); // DCT.str:151
    add(iDCT_1D_Y_reference_fine.__construct(size)); // DCT.str:156
    add(iDCT_1D_X_reference_fine.__construct(size)); // DCT.str:157
    add(AnonFilter_a1.__construct()); // DCT.str:158
  }
}
class iDCT8x8_2D_fast_coarse extends Pipeline // DCT.str:174
{
  public static iDCT8x8_2D_fast_coarse __construct()
  {
    iDCT8x8_2D_fast_coarse __obj = new iDCT8x8_2D_fast_coarse();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // DCT.str:174
    add(iDCT8x8_1D_row_fast.__construct()); // DCT.str:175
    add(iDCT8x8_1D_col_fast.__construct()); // DCT.str:176
  }
}
class iDCT8x8_2D_fast_fine extends Pipeline // DCT.str:188
{
  public static iDCT8x8_2D_fast_fine __construct()
  {
    iDCT8x8_2D_fast_fine __obj = new iDCT8x8_2D_fast_fine();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // DCT.str:188
    add(iDCT8x8_1D_X_fast_fine.__construct()); // DCT.str:189
    add(iDCT8x8_1D_Y_fast_fine.__construct()); // DCT.str:190
  }
}
class DCT_2D_reference_fine extends Pipeline // DCT.str:203
{
  private int __param_size;
  public static DCT_2D_reference_fine __construct(int size)
  {
    DCT_2D_reference_fine __obj = new DCT_2D_reference_fine();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // DCT.str:203
    add(AnonFilter_a2.__construct()); // DCT.str:204
    add(DCT_1D_X_reference_fine.__construct(size)); // DCT.str:210
    add(DCT_1D_Y_reference_fine.__construct(size)); // DCT.str:211
    add(AnonFilter_a3.__construct()); // DCT.str:212
  }
}
class iDCT_1D_X_reference_fine extends SplitJoin // DCT.str:223
{
  private int __param_size;
  public static iDCT_1D_X_reference_fine __construct(int size)
  {
    iDCT_1D_X_reference_fine __obj = new iDCT_1D_X_reference_fine();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // DCT.str:223
    setSplitter(ROUND_ROBIN(size)); // DCT.str:224
    for (int i = 0; (i < size); i++) { // DCT.str:225
      add(iDCT_1D_reference_fine.__construct(size)); // DCT.str:226
    }; // DCT.str:225
    setJoiner(ROUND_ROBIN(size)); // DCT.str:228
  }
}
class iDCT_1D_Y_reference_fine extends SplitJoin // DCT.str:234
{
  private int __param_size;
  public static iDCT_1D_Y_reference_fine __construct(int size)
  {
    iDCT_1D_Y_reference_fine __obj = new iDCT_1D_Y_reference_fine();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // DCT.str:234
    setSplitter(ROUND_ROBIN(size)); // DCT.str:235
    for (int i = 0; (i < size); i++) { // DCT.str:236
      add(iDCT_1D_reference_fine.__construct(size)); // DCT.str:237
    }; // DCT.str:236
    setJoiner(ROUND_ROBIN(size)); // DCT.str:239
  }
}
class iDCT8x8_1D_X_fast_fine extends SplitJoin // DCT.str:245
{
  public static iDCT8x8_1D_X_fast_fine __construct()
  {
    iDCT8x8_1D_X_fast_fine __obj = new iDCT8x8_1D_X_fast_fine();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // DCT.str:245
    setSplitter(ROUND_ROBIN(8)); // DCT.str:246
    for (int i = 0; (i < 8); i++) { // DCT.str:247
      add(iDCT8x8_1D_row_fast.__construct()); // DCT.str:248
    }; // DCT.str:247
    setJoiner(ROUND_ROBIN(8)); // DCT.str:250
  }
}
class iDCT8x8_1D_Y_fast_fine extends SplitJoin // DCT.str:256
{
  public static iDCT8x8_1D_Y_fast_fine __construct()
  {
    iDCT8x8_1D_Y_fast_fine __obj = new iDCT8x8_1D_Y_fast_fine();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // DCT.str:256
    setSplitter(ROUND_ROBIN(1)); // DCT.str:257
    for (int i = 0; (i < 8); i++) { // DCT.str:258
      add(iDCT8x8_1D_col_fast_fine.__construct()); // DCT.str:259
    }; // DCT.str:258
    setJoiner(ROUND_ROBIN(1)); // DCT.str:261
  }
}
class DCT_1D_X_reference_fine extends SplitJoin // DCT.str:267
{
  private int __param_size;
  public static DCT_1D_X_reference_fine __construct(int size)
  {
    DCT_1D_X_reference_fine __obj = new DCT_1D_X_reference_fine();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // DCT.str:267
    setSplitter(ROUND_ROBIN(size)); // DCT.str:268
    for (int i = 0; (i < size); i++) { // DCT.str:269
      add(DCT_1D_reference_fine.__construct(size)); // DCT.str:270
    }; // DCT.str:269
    setJoiner(ROUND_ROBIN(size)); // DCT.str:272
  }
}
class DCT_1D_Y_reference_fine extends SplitJoin // DCT.str:278
{
  private int __param_size;
  public static DCT_1D_Y_reference_fine __construct(int size)
  {
    DCT_1D_Y_reference_fine __obj = new DCT_1D_Y_reference_fine();
    __obj.__param_size = size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_size);
  }
  public void init(final int size) { // DCT.str:278
    setSplitter(ROUND_ROBIN(1)); // DCT.str:279
    for (int i = 0; (i < size); i++) { // DCT.str:280
      add(DCT_1D_reference_fine.__construct(size)); // DCT.str:281
    }; // DCT.str:280
    setJoiner(ROUND_ROBIN(1)); // DCT.str:283
  }
}
class iDCT_2D_reference_coarse extends Filter // DCT.str:290
{
  private int __param__param_size;
  public static iDCT_2D_reference_coarse __construct(int _param_size)
  {
    iDCT_2D_reference_coarse __obj = new iDCT_2D_reference_coarse();
    __obj.__param__param_size = _param_size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_size);
  }
  float[][] coeff; // DCT.str:291
  int size; // DCT.str:290
  public void work() { // DCT.str:301
    float[][] block_x = new float[size][size]; // DCT.str:302
    for (int __sa4 = 0; (__sa4 < size); __sa4++) { // DCT.str:302
    }; // DCT.str:302
    int i, j, k; // DCT.str:303
    for (i = 0; (i < size); i++) { // DCT.str:306
      for (j = 0; (j < size); j++) { // DCT.str:306
        block_x[i][j] = 0; // DCT.str:307
        for (k = 0; (k < size); k++) { // DCT.str:308
          block_x[i][j] += (coeff[k][j] * inputChannel.peekInt(((size * i) + k))); // DCT.str:309
        }; // DCT.str:308
      }; // DCT.str:306
    }; // DCT.str:305
    for (i = 0; (i < size); i++) { // DCT.str:313
      for (j = 0; (j < size); j++) { // DCT.str:314
        float block_y; // DCT.str:315
        block_y = 0.0f; // DCT.str:315
        for (k = 0; (k < size); k++) { // DCT.str:316
          block_y += (coeff[k][i] * block_x[k][j]); // DCT.str:317
        }; // DCT.str:316
        block_y = (float)Math.floor((block_y + 0.5f)); // DCT.str:319
        outputChannel.pushInt(((int)(block_y))); // DCT.str:320
      }; // DCT.str:314
    }; // DCT.str:313
    for (i = 0; (i < (size * size)); i++) { // DCT.str:324
      inputChannel.popInt(); // DCT.str:324
    }; // DCT.str:324
  }
  public void init(final int _param_size) { // DCT.str:293
    size = _param_size; // DCT.str:293
    coeff = new float[size][size]; // DCT.str:291
    for (int __sa5 = 0; (__sa5 < size); __sa5++) { // DCT.str:291
    }; // DCT.str:291
    setIOTypes(Integer.TYPE, Integer.TYPE); // DCT.str:290
    addSteadyPhase((size * size), (size * size), (size * size), "work"); // DCT.str:301
    for (int freq = 0; (freq < size); freq++) { // DCT.str:294
      float scale; // DCT.str:295
      scale = ((freq == 0) ? (float)Math.sqrt(0.125f) : 0.5f); // DCT.str:295
      for (int time = 0; (time < size); time++) { // DCT.str:297
        coeff[freq][time] = (scale * (float)Math.cos((((3.141592653589793f / ((float)(size))) * freq) * (time + 0.5f)))); // DCT.str:297
      }; // DCT.str:296
    }; // DCT.str:294
  }
}
class DCT_2D_reference_coarse extends Filter // DCT.str:338
{
  private int __param__param_size;
  public static DCT_2D_reference_coarse __construct(int _param_size)
  {
    DCT_2D_reference_coarse __obj = new DCT_2D_reference_coarse();
    __obj.__param__param_size = _param_size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_size);
  }
  float[][] coeff; // DCT.str:339
  int size; // DCT.str:338
  public void work() { // DCT.str:349
    float[][] block_x = new float[size][size]; // DCT.str:350
    for (int __sa6 = 0; (__sa6 < size); __sa6++) { // DCT.str:350
    }; // DCT.str:350
    int i, j, k; // DCT.str:351
    for (i = 0; (i < size); i++) { // DCT.str:355
      for (j = 0; (j < size); j++) { // DCT.str:355
        block_x[i][j] = 0.0f; // DCT.str:356
        for (k = 0; (k < size); k++) { // DCT.str:357
          block_x[i][j] += (coeff[j][k] * inputChannel.peekInt(((size * i) + k))); // DCT.str:358
        }; // DCT.str:357
      }; // DCT.str:355
    }; // DCT.str:354
    for (i = 0; (i < size); i++) { // DCT.str:362
      for (j = 0; (j < size); j++) { // DCT.str:363
        float block_y; // DCT.str:364
        block_y = 0.0f; // DCT.str:364
        for (k = 0; (k < size); k++) { // DCT.str:365
          block_y += (coeff[i][k] * block_x[k][j]); // DCT.str:366
        }; // DCT.str:365
        block_y = (float)Math.floor((block_y + 0.5f)); // DCT.str:368
        outputChannel.pushInt(((int)(block_y))); // DCT.str:369
      }; // DCT.str:363
    }; // DCT.str:362
    for (i = 0; (i < (size * size)); i++) { // DCT.str:373
      inputChannel.popInt(); // DCT.str:373
    }; // DCT.str:373
  }
  public void init(final int _param_size) { // DCT.str:341
    size = _param_size; // DCT.str:341
    coeff = new float[size][size]; // DCT.str:339
    for (int __sa7 = 0; (__sa7 < size); __sa7++) { // DCT.str:339
    }; // DCT.str:339
    setIOTypes(Integer.TYPE, Integer.TYPE); // DCT.str:338
    addSteadyPhase((size * size), (size * size), (size * size), "work"); // DCT.str:349
    for (int i = 0; (i < size); i++) { // DCT.str:342
      float s; // DCT.str:343
      s = ((i == 0) ? (float)Math.sqrt(0.125f) : 0.5f); // DCT.str:343
      for (int j = 0; (j < size); j++) { // DCT.str:345
        coeff[i][j] = (s * (float)Math.cos((((3.141592653589793f / size) * i) * (j + 0.5f)))); // DCT.str:345
      }; // DCT.str:344
    }; // DCT.str:342
  }
}
class iDCT_1D_reference_fine extends Filter // DCT.str:387
{
  private int __param__param_size;
  public static iDCT_1D_reference_fine __construct(int _param_size)
  {
    iDCT_1D_reference_fine __obj = new iDCT_1D_reference_fine();
    __obj.__param__param_size = _param_size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_size);
  }
  float[][] coeff; // DCT.str:388
  int size; // DCT.str:387
  public void work() { // DCT.str:400
    for (int x = 0; (x < size); x++) { // DCT.str:401
      float tempsum; // DCT.str:402
      tempsum = 0; // DCT.str:402
      for (int u = 0; (u < size); u++) { // DCT.str:403
        tempsum += (coeff[x][u] * inputChannel.peekFloat(u)); // DCT.str:404
      }; // DCT.str:403
      outputChannel.pushFloat(tempsum); // DCT.str:406
    }; // DCT.str:401
    for (int u = 0; (u < size); u++) { // DCT.str:408
      inputChannel.popFloat(); // DCT.str:408
    }; // DCT.str:408
  }
  public void init(final int _param_size) { // DCT.str:390
    size = _param_size; // DCT.str:390
    coeff = new float[size][size]; // DCT.str:388
    for (int __sa8 = 0; (__sa8 < size); __sa8++) { // DCT.str:388
    }; // DCT.str:388
    setIOTypes(Float.TYPE, Float.TYPE); // DCT.str:387
    addSteadyPhase(size, size, size, "work"); // DCT.str:400
    for (int x = 0; (x < size); x++) { // DCT.str:391
      for (int u = 0; (u < size); u++) { // DCT.str:392
        float Cu; // DCT.str:393
        Cu = 1; // DCT.str:393
        if ((u == 0)) { // DCT.str:394
          Cu = (1 / (float)Math.sqrt(2)); // DCT.str:394
        } // DCT.str:394
        coeff[x][u] = ((0.5f * Cu) * (float)Math.cos((((u * 3.141592653589793f) * ((2.0f * x) + 1)) / (2.0f * size)))); // DCT.str:395
      }; // DCT.str:392
    }; // DCT.str:391
  }
}
class iDCT8x8_1D_row_fast extends Filter // DCT.str:421
{
  public static iDCT8x8_1D_row_fast __construct()
  {
    iDCT8x8_1D_row_fast __obj = new iDCT8x8_1D_row_fast();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int size; // DCT.str:422
  int W1; // DCT.str:424
  int W2; // DCT.str:425
  int W3; // DCT.str:426
  int W5; // DCT.str:427
  int W6; // DCT.str:428
  int W7; // DCT.str:429
  public void work() { // DCT.str:431
    int x0 = inputChannel.peekInt(0); // DCT.str:432
    int x1 = (inputChannel.peekInt(4) << 11); // DCT.str:433
    int x2 = inputChannel.peekInt(6); // DCT.str:434
    int x3 = inputChannel.peekInt(2); // DCT.str:435
    int x4 = inputChannel.peekInt(1); // DCT.str:436
    int x5 = inputChannel.peekInt(7); // DCT.str:437
    int x6 = inputChannel.peekInt(5); // DCT.str:438
    int x7 = inputChannel.peekInt(3); // DCT.str:439
    int x8; // DCT.str:440
    if ((((((((x1 == 0) && (x2 == 0)) && (x3 == 0)) && (x4 == 0)) && (x5 == 0)) && (x6 == 0)) && (x7 == 0))) { // DCT.str:444
      x0 = (x0 << 3); // DCT.str:445
      for (int i = 0; (i < size); i++) { // DCT.str:446
        outputChannel.pushInt(x0); // DCT.str:447
      }; // DCT.str:446
    } else { // DCT.str:450
      x0 = ((x0 << 11) + 128); // DCT.str:452
      x8 = (W7 * (x4 + x5)); // DCT.str:455
      x4 = (x8 + ((W1 - W7) * x4)); // DCT.str:456
      x5 = (x8 - ((W1 + W7) * x5)); // DCT.str:457
      x8 = (W3 * (x6 + x7)); // DCT.str:458
      x6 = (x8 - ((W3 - W5) * x6)); // DCT.str:459
      x7 = (x8 - ((W3 + W5) * x7)); // DCT.str:460
      x8 = (x0 + x1); // DCT.str:463
      x0 = (x0 - x1); // DCT.str:464
      x1 = (W6 * (x3 + x2)); // DCT.str:465
      x2 = (x1 - ((W2 + W6) * x2)); // DCT.str:466
      x3 = (x1 + ((W2 - W6) * x3)); // DCT.str:467
      x1 = (x4 + x6); // DCT.str:468
      x4 = (x4 - x6); // DCT.str:469
      x6 = (x5 + x7); // DCT.str:470
      x5 = (x5 - x7); // DCT.str:471
      x7 = (x8 + x3); // DCT.str:474
      x8 = (x8 - x3); // DCT.str:475
      x3 = (x0 + x2); // DCT.str:476
      x0 = (x0 - x2); // DCT.str:477
      x2 = (((181 * (x4 + x5)) + 128) >> 8); // DCT.str:478
      x4 = (((181 * (x4 - x5)) + 128) >> 8); // DCT.str:479
      outputChannel.pushInt(((x7 + x1) >> 8)); // DCT.str:482
      outputChannel.pushInt(((x3 + x2) >> 8)); // DCT.str:483
      outputChannel.pushInt(((x0 + x4) >> 8)); // DCT.str:484
      outputChannel.pushInt(((x8 + x6) >> 8)); // DCT.str:485
      outputChannel.pushInt(((x8 - x6) >> 8)); // DCT.str:486
      outputChannel.pushInt(((x0 - x4) >> 8)); // DCT.str:487
      outputChannel.pushInt(((x3 - x2) >> 8)); // DCT.str:488
      outputChannel.pushInt(((x7 - x1) >> 8)); // DCT.str:489
    } // DCT.str:443
    for (int i = 0; (i < size); i++) { // DCT.str:491
      inputChannel.popInt(); // DCT.str:491
    }; // DCT.str:491
  }
  public void init() { // DCT.str:421
    size = 8; // DCT.str:422
    W1 = 2841; // DCT.str:424
    W2 = 2676; // DCT.str:425
    W3 = 2408; // DCT.str:426
    W5 = 1609; // DCT.str:427
    W6 = 1108; // DCT.str:428
    W7 = 565; // DCT.str:429
    setIOTypes(Integer.TYPE, Integer.TYPE); // DCT.str:421
    addSteadyPhase(size, size, size, "work"); // DCT.str:431
  }
}
class iDCT8x8_1D_col_fast extends Filter // DCT.str:505
{
  public static iDCT8x8_1D_col_fast __construct()
  {
    iDCT8x8_1D_col_fast __obj = new iDCT8x8_1D_col_fast();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int size; // DCT.str:506
  int[] buffer; // DCT.str:507
  int W1; // DCT.str:509
  int W2; // DCT.str:510
  int W3; // DCT.str:511
  int W5; // DCT.str:512
  int W6; // DCT.str:513
  int W7; // DCT.str:514
  public void work() { // DCT.str:516
    for (int c = 0; (c < size); c++) { // DCT.str:517
      int x0; // DCT.str:518
      x0 = inputChannel.peekInt((c + (size * 0))); // DCT.str:518
      int x1; // DCT.str:519
      x1 = (inputChannel.peekInt((c + (size * 4))) << 8); // DCT.str:519
      int x2; // DCT.str:520
      x2 = inputChannel.peekInt((c + (size * 6))); // DCT.str:520
      int x3; // DCT.str:521
      x3 = inputChannel.peekInt((c + (size * 2))); // DCT.str:521
      int x4; // DCT.str:522
      x4 = inputChannel.peekInt((c + (size * 1))); // DCT.str:522
      int x5; // DCT.str:523
      x5 = inputChannel.peekInt((c + (size * 7))); // DCT.str:523
      int x6; // DCT.str:524
      x6 = inputChannel.peekInt((c + (size * 5))); // DCT.str:524
      int x7; // DCT.str:525
      x7 = inputChannel.peekInt((c + (size * 3))); // DCT.str:525
      int x8; // DCT.str:526
      if ((((((((x1 == 0) && (x2 == 0)) && (x3 == 0)) && (x4 == 0)) && (x5 == 0)) && (x6 == 0)) && (x7 == 0))) { // DCT.str:530
        x0 = ((x0 + 32) >> 6); // DCT.str:531
        for (int i = 0; (i < size); i++) { // DCT.str:532
          buffer[(c + (size * i))] = x0; // DCT.str:533
        }; // DCT.str:532
      } else { // DCT.str:536
        x0 = ((x0 << 8) + 8192); // DCT.str:538
        x8 = ((W7 * (x4 + x5)) + 4); // DCT.str:541
        x4 = ((x8 + ((W1 - W7) * x4)) >> 3); // DCT.str:542
        x5 = ((x8 - ((W1 + W7) * x5)) >> 3); // DCT.str:543
        x8 = ((W3 * (x6 + x7)) + 4); // DCT.str:544
        x6 = ((x8 - ((W3 - W5) * x6)) >> 3); // DCT.str:545
        x7 = ((x8 - ((W3 + W5) * x7)) >> 3); // DCT.str:546
        x8 = (x0 + x1); // DCT.str:549
        x0 = (x0 - x1); // DCT.str:550
        x1 = ((W6 * (x3 + x2)) + 4); // DCT.str:551
        x2 = ((x1 - ((W2 + W6) * x2)) >> 3); // DCT.str:552
        x3 = ((x1 + ((W2 - W6) * x3)) >> 3); // DCT.str:553
        x1 = (x4 + x6); // DCT.str:554
        x4 = (x4 - x6); // DCT.str:555
        x6 = (x5 + x7); // DCT.str:556
        x5 = (x5 - x7); // DCT.str:557
        x7 = (x8 + x3); // DCT.str:560
        x8 = (x8 - x3); // DCT.str:561
        x3 = (x0 + x2); // DCT.str:562
        x0 = (x0 - x2); // DCT.str:563
        x2 = (((181 * (x4 + x5)) + 128) >> 8); // DCT.str:564
        x4 = (((181 * (x4 - x5)) + 128) >> 8); // DCT.str:565
        buffer[(c + (size * 0))] = ((x7 + x1) >> 14); // DCT.str:568
        buffer[(c + (size * 1))] = ((x3 + x2) >> 14); // DCT.str:569
        buffer[(c + (size * 2))] = ((x0 + x4) >> 14); // DCT.str:570
        buffer[(c + (size * 3))] = ((x8 + x6) >> 14); // DCT.str:571
        buffer[(c + (size * 4))] = ((x8 - x6) >> 14); // DCT.str:572
        buffer[(c + (size * 5))] = ((x0 - x4) >> 14); // DCT.str:573
        buffer[(c + (size * 6))] = ((x3 - x2) >> 14); // DCT.str:574
        buffer[(c + (size * 7))] = ((x7 - x1) >> 14); // DCT.str:575
      } // DCT.str:529
    }; // DCT.str:517
    for (int i = 0; (i < (size * size)); i++) { // DCT.str:578
      inputChannel.popInt(); // DCT.str:579
      outputChannel.pushInt(buffer[i]); // DCT.str:580
    }; // DCT.str:578
  }
  public void init() { // DCT.str:505
    buffer = new int[(8 * 8)]; // DCT.str:507
    size = 8; // DCT.str:506
    W1 = 2841; // DCT.str:509
    W2 = 2676; // DCT.str:510
    W3 = 2408; // DCT.str:511
    W5 = 1609; // DCT.str:512
    W6 = 1108; // DCT.str:513
    W7 = 565; // DCT.str:514
    setIOTypes(Integer.TYPE, Integer.TYPE); // DCT.str:505
    addSteadyPhase((size * size), (size * size), (size * size), "work"); // DCT.str:516
  }
}
class iDCT8x8_1D_col_fast_fine extends Filter // DCT.str:595
{
  public static iDCT8x8_1D_col_fast_fine __construct()
  {
    iDCT8x8_1D_col_fast_fine __obj = new iDCT8x8_1D_col_fast_fine();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int size; // DCT.str:596
  int W1; // DCT.str:598
  int W2; // DCT.str:599
  int W3; // DCT.str:600
  int W5; // DCT.str:601
  int W6; // DCT.str:602
  int W7; // DCT.str:603
  public void work() { // DCT.str:605
    int x0 = inputChannel.peekInt(0); // DCT.str:606
    int x1 = (inputChannel.peekInt(4) << 8); // DCT.str:607
    int x2 = inputChannel.peekInt(6); // DCT.str:608
    int x3 = inputChannel.peekInt(2); // DCT.str:609
    int x4 = inputChannel.peekInt(1); // DCT.str:610
    int x5 = inputChannel.peekInt(7); // DCT.str:611
    int x6 = inputChannel.peekInt(5); // DCT.str:612
    int x7 = inputChannel.peekInt(3); // DCT.str:613
    int x8; // DCT.str:614
    if ((((((((x1 == 0) && (x2 == 0)) && (x3 == 0)) && (x4 == 0)) && (x5 == 0)) && (x6 == 0)) && (x7 == 0))) { // DCT.str:618
      x0 = ((x0 + 32) >> 6); // DCT.str:619
      for (int i = 0; (i < size); i++) { // DCT.str:620
        outputChannel.pushInt(x0); // DCT.str:621
      }; // DCT.str:620
    } else { // DCT.str:624
      x0 = ((x0 << 8) + 8192); // DCT.str:626
      x8 = ((W7 * (x4 + x5)) + 4); // DCT.str:629
      x4 = ((x8 + ((W1 - W7) * x4)) >> 3); // DCT.str:630
      x5 = ((x8 - ((W1 + W7) * x5)) >> 3); // DCT.str:631
      x8 = ((W3 * (x6 + x7)) + 4); // DCT.str:632
      x6 = ((x8 - ((W3 - W5) * x6)) >> 3); // DCT.str:633
      x7 = ((x8 - ((W3 + W5) * x7)) >> 3); // DCT.str:634
      x8 = (x0 + x1); // DCT.str:637
      x0 = (x0 - x1); // DCT.str:638
      x1 = ((W6 * (x3 + x2)) + 4); // DCT.str:639
      x2 = ((x1 - ((W2 + W6) * x2)) >> 3); // DCT.str:640
      x3 = ((x1 + ((W2 - W6) * x3)) >> 3); // DCT.str:641
      x1 = (x4 + x6); // DCT.str:642
      x4 = (x4 - x6); // DCT.str:643
      x6 = (x5 + x7); // DCT.str:644
      x5 = (x5 - x7); // DCT.str:645
      x7 = (x8 + x3); // DCT.str:648
      x8 = (x8 - x3); // DCT.str:649
      x3 = (x0 + x2); // DCT.str:650
      x0 = (x0 - x2); // DCT.str:651
      x2 = (((181 * (x4 + x5)) + 128) >> 8); // DCT.str:652
      x4 = (((181 * (x4 - x5)) + 128) >> 8); // DCT.str:653
      outputChannel.pushInt(((x7 + x1) >> 14)); // DCT.str:656
      outputChannel.pushInt(((x3 + x2) >> 14)); // DCT.str:657
      outputChannel.pushInt(((x0 + x4) >> 14)); // DCT.str:658
      outputChannel.pushInt(((x8 + x6) >> 14)); // DCT.str:659
      outputChannel.pushInt(((x8 - x6) >> 14)); // DCT.str:660
      outputChannel.pushInt(((x0 - x4) >> 14)); // DCT.str:661
      outputChannel.pushInt(((x3 - x2) >> 14)); // DCT.str:662
      outputChannel.pushInt(((x7 - x1) >> 14)); // DCT.str:663
    } // DCT.str:617
    for (int i = 0; (i < size); i++) { // DCT.str:665
      inputChannel.popInt(); // DCT.str:665
    }; // DCT.str:665
  }
  public void init() { // DCT.str:595
    size = 8; // DCT.str:596
    W1 = 2841; // DCT.str:598
    W2 = 2676; // DCT.str:599
    W3 = 2408; // DCT.str:600
    W5 = 1609; // DCT.str:601
    W6 = 1108; // DCT.str:602
    W7 = 565; // DCT.str:603
    setIOTypes(Integer.TYPE, Integer.TYPE); // DCT.str:595
    addSteadyPhase(size, size, size, "work"); // DCT.str:605
  }
}
class DCT_1D_reference_fine extends Filter // DCT.str:680
{
  private int __param__param_size;
  public static DCT_1D_reference_fine __construct(int _param_size)
  {
    DCT_1D_reference_fine __obj = new DCT_1D_reference_fine();
    __obj.__param__param_size = _param_size;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_size);
  }
  float[][] coeff; // DCT.str:681
  int size; // DCT.str:680
  public void work() { // DCT.str:694
    for (int u = 0; (u < size); u++) { // DCT.str:695
      float tempsum; // DCT.str:696
      tempsum = 0; // DCT.str:696
      for (int x = 0; (x < size); x++) { // DCT.str:697
        tempsum += (inputChannel.peekFloat(x) * coeff[u][x]); // DCT.str:698
      }; // DCT.str:697
      outputChannel.pushFloat(tempsum); // DCT.str:700
    }; // DCT.str:695
  }
  public void init(final int _param_size) { // DCT.str:683
    size = _param_size; // DCT.str:683
    coeff = new float[size][size]; // DCT.str:681
    for (int __sa9 = 0; (__sa9 < size); __sa9++) { // DCT.str:681
    }; // DCT.str:681
    setIOTypes(Float.TYPE, Float.TYPE); // DCT.str:680
    addSteadyPhase(size, size, size, "work"); // DCT.str:694
    for (int u = 0; (u < size); u++) { // DCT.str:684
      float Cu; // DCT.str:685
      Cu = 1; // DCT.str:685
      if ((u == 0)) { // DCT.str:686
        Cu = (1 / (float)Math.sqrt(2)); // DCT.str:686
      } // DCT.str:686
      for (int x = 0; (x < size); x++) { // DCT.str:688
        coeff[u][x] = ((0.5f * Cu) * (float)Math.cos((((u * 3.141592653589793f) * ((2.0f * x) + 1)) / (2.0f * size)))); // DCT.str:689
      }; // DCT.str:688
    }; // DCT.str:684
  }
}
class AnonFilter_a0 extends Filter // DCT.str:151
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
  public void work() { // DCT.str:152
    outputChannel.pushFloat(((float)(inputChannel.popInt()))); // DCT.str:153
  }
  public void init() { // DCT.str:151
    setIOTypes(Integer.TYPE, Float.TYPE); // DCT.str:151
    addSteadyPhase(1, 1, 1, "work"); // DCT.str:152
  }
}
class AnonFilter_a1 extends Filter // DCT.str:158
{
  public static AnonFilter_a1 __construct()
  {
    AnonFilter_a1 __obj = new AnonFilter_a1();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DCT.str:159
    outputChannel.pushInt(((int)((float)Math.floor((inputChannel.popFloat() + 0.5f))))); // DCT.str:160
  }
  public void init() { // DCT.str:158
    setIOTypes(Float.TYPE, Integer.TYPE); // DCT.str:158
    addSteadyPhase(1, 1, 1, "work"); // DCT.str:159
  }
}
class AnonFilter_a2 extends Filter // DCT.str:204
{
  public static AnonFilter_a2 __construct()
  {
    AnonFilter_a2 __obj = new AnonFilter_a2();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DCT.str:205
    int v = inputChannel.popInt(); // DCT.str:206
    outputChannel.pushFloat(((float)(v))); // DCT.str:207
  }
  public void init() { // DCT.str:204
    setIOTypes(Integer.TYPE, Float.TYPE); // DCT.str:204
    addSteadyPhase(1, 1, 1, "work"); // DCT.str:205
  }
}
class AnonFilter_a3 extends Filter // DCT.str:212
{
  public static AnonFilter_a3 __construct()
  {
    AnonFilter_a3 __obj = new AnonFilter_a3();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // DCT.str:213
    float v = (float)Math.floor((inputChannel.popFloat() + 0.5f)); // DCT.str:214
    outputChannel.pushInt(((int)(v))); // DCT.str:215
  }
  public void init() { // DCT.str:212
    setIOTypes(Float.TYPE, Integer.TYPE); // DCT.str:212
    addSteadyPhase(1, 1, 1, "work"); // DCT.str:213
  }
}
public class DCT extends StreamItPipeline // DCT.str:80
{
  public void init() { // DCT.str:80
    add(RandomSource.__construct()); // DCT.str:82
    add(iDCT8x8_ieee.__construct(0)); // DCT.str:97
    add(FloatPrinter.__construct()); // DCT.str:98
  }
  public static void main(String[] args) {
    DCT program = new DCT();
    program.run(args);
    FileWriter.closeAll();
  }
}
