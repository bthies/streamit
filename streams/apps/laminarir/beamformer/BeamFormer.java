import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // BeamFormer.str:8
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
  int seed; // BeamFormer.str:9
  public void work() { // BeamFormer.str:10
    outputChannel.pushInt(seed); // BeamFormer.str:11
    seed = (((65793 * seed) + 4282663) % 8388608); // BeamFormer.str:12
  }
  public void init() { // BeamFormer.str:8
    seed = 0; // BeamFormer.str:9
    setIOTypes(Void.TYPE, Integer.TYPE); // BeamFormer.str:8
    addSteadyPhase(0, 0, 1, "work"); // BeamFormer.str:10
  }
}
class IntSource extends Filter // BeamFormer.str:16
{
  public static IntSource __construct()
  {
    IntSource __obj = new IntSource();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int x; // BeamFormer.str:17
  public void work() { // BeamFormer.str:19
    outputChannel.pushInt(x++); // BeamFormer.str:20
  }
  public void init() { // BeamFormer.str:18
    setIOTypes(Void.TYPE, Integer.TYPE); // BeamFormer.str:16
    addSteadyPhase(0, 0, 1, "work"); // BeamFormer.str:19
    x = 0; // BeamFormer.str:18
  }
}
class IntPrinter extends Filter // BeamFormer.str:24
{
  public static IntPrinter __construct()
  {
    IntPrinter __obj = new IntPrinter();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int x; // BeamFormer.str:25
  public void work() { // BeamFormer.str:26
    x = inputChannel.popInt(); // BeamFormer.str:27
    System.out.println(x); // BeamFormer.str:28
  }
  public void init() { // BeamFormer.str:24
    setIOTypes(Integer.TYPE, Void.TYPE); // BeamFormer.str:24
    addSteadyPhase(1, 1, 0, "work"); // BeamFormer.str:26
  }
}
class BeamFormerMain extends Pipeline // BeamFormer.str:37
{
  public static BeamFormerMain __construct()
  {
    BeamFormerMain __obj = new BeamFormerMain();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // BeamFormer.str:37
    int numChannels = 12; // BeamFormer.str:38
    int numSamples = 1024; // BeamFormer.str:39
    int numBeams = 4; // BeamFormer.str:40
    int numCoarseFilterTaps = 64; // BeamFormer.str:41
    int numFineFilterTaps = 64; // BeamFormer.str:42
    int coarseDecimationRatio = 2; // BeamFormer.str:45
    int fineDecimationRatio = 3; // BeamFormer.str:46
    int numSegments = 1; // BeamFormer.str:47
    int numPostDec1 = (numSamples / coarseDecimationRatio); // BeamFormer.str:48
    int numPostDec2 = (numPostDec1 / fineDecimationRatio); // BeamFormer.str:49
    int mfSize = (numSegments * numPostDec2); // BeamFormer.str:50
    int pulseSize = (numPostDec2 / 2); // BeamFormer.str:51
    int predecPulseSize = ((pulseSize * coarseDecimationRatio) * fineDecimationRatio); // BeamFormer.str:52
    int targetBeam = (numBeams / 4); // BeamFormer.str:53
    int targetSample = (numSamples / 4); // BeamFormer.str:54
    int targetSamplePostDec = ((targetSample / coarseDecimationRatio) / fineDecimationRatio); // BeamFormer.str:57
    float dOverLambda = 0.5f; // BeamFormer.str:58
    float cfarThreshold = (((0.95f * dOverLambda) * numChannels) * (0.5f * pulseSize)); // BeamFormer.str:59
    add(AnonFilter_a1.__construct(cfarThreshold, coarseDecimationRatio, fineDecimationRatio, numChannels, numCoarseFilterTaps, numFineFilterTaps, numPostDec1, numSamples, targetBeam, targetSample)); // BeamFormer.str:61
    add(AnonFilter_a3.__construct(cfarThreshold, mfSize, numBeams, numChannels, numPostDec2, targetBeam, targetSamplePostDec)); // BeamFormer.str:81
  }
}
class InputGenerate extends Filter // BeamFormer.str:106
{
  private int __param__param_myChannel;
  private int __param__param_numberOfSamples;
  private int __param__param_tarBeam;
  private int __param__param_targetSample;
  private float __param__param_thresh;
  public static InputGenerate __construct(int _param_myChannel, int _param_numberOfSamples, int _param_tarBeam, int _param_targetSample, float _param_thresh)
  {
    InputGenerate __obj = new InputGenerate();
    __obj.__param__param_myChannel = _param_myChannel;
    __obj.__param__param_numberOfSamples = _param_numberOfSamples;
    __obj.__param__param_tarBeam = _param_tarBeam;
    __obj.__param__param_targetSample = _param_targetSample;
    __obj.__param__param_thresh = _param_thresh;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_myChannel, __param__param_numberOfSamples, __param__param_tarBeam, __param__param_targetSample, __param__param_thresh);
  }
  int curSample; // BeamFormer.str:107
  boolean holdsTarget; // BeamFormer.str:108
  int myChannel; // BeamFormer.str:106
  int numberOfSamples; // BeamFormer.str:106
  int tarBeam; // BeamFormer.str:106
  int targetSample; // BeamFormer.str:106
  float thresh; // BeamFormer.str:106
  public void work() { // BeamFormer.str:115
    inputChannel.popInt(); // BeamFormer.str:116
    if ((holdsTarget && (curSample == targetSample))) { // BeamFormer.str:117
      outputChannel.pushFloat((float)Math.sqrt((curSample * myChannel))); // BeamFormer.str:120
      outputChannel.pushFloat(((float)Math.sqrt((curSample * myChannel)) + 1)); // BeamFormer.str:121
    } else { // BeamFormer.str:124
      outputChannel.pushFloat(-(float)Math.sqrt((curSample * myChannel))); // BeamFormer.str:127
      outputChannel.pushFloat(-((float)Math.sqrt((curSample * myChannel)) + 1)); // BeamFormer.str:128
    } // BeamFormer.str:117
    curSample++; // BeamFormer.str:134
    if ((curSample >= numberOfSamples)) { // BeamFormer.str:136
      curSample = 0; // BeamFormer.str:137
    } // BeamFormer.str:136
  }
  public void init(final int _param_myChannel, final int _param_numberOfSamples, final int _param_tarBeam, final int _param_targetSample, final float _param_thresh) { // BeamFormer.str:110
    myChannel = _param_myChannel; // BeamFormer.str:110
    numberOfSamples = _param_numberOfSamples; // BeamFormer.str:110
    tarBeam = _param_tarBeam; // BeamFormer.str:110
    targetSample = _param_targetSample; // BeamFormer.str:110
    thresh = _param_thresh; // BeamFormer.str:110
    setIOTypes(Integer.TYPE, Float.TYPE); // BeamFormer.str:106
    addSteadyPhase(1, 1, 2, "work"); // BeamFormer.str:115
    curSample = 0; // BeamFormer.str:111
    holdsTarget = (tarBeam == myChannel); // BeamFormer.str:112
  }
}
class FloatPrinter extends Filter // BeamFormer.str:142
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
  float x; // BeamFormer.str:143
  public void work() { // BeamFormer.str:144
    x = inputChannel.popFloat(); // BeamFormer.str:145
    System.out.println(x); // BeamFormer.str:146
  }
  public void init() { // BeamFormer.str:142
    setIOTypes(Float.TYPE, Void.TYPE); // BeamFormer.str:142
    addSteadyPhase(1, 1, 0, "work"); // BeamFormer.str:144
  }
}
class BeamFirFilter extends Filter // BeamFormer.str:150
{
  private int __param__param_numTaps;
  private int __param__param_inputLength;
  private int __param__param_decimationRatio;
  public static BeamFirFilter __construct(int _param_numTaps, int _param_inputLength, int _param_decimationRatio)
  {
    BeamFirFilter __obj = new BeamFirFilter();
    __obj.__param__param_numTaps = _param_numTaps;
    __obj.__param__param_inputLength = _param_inputLength;
    __obj.__param__param_decimationRatio = _param_decimationRatio;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_numTaps, __param__param_inputLength, __param__param_decimationRatio);
  }
  float[] real_weight; // BeamFormer.str:151
  float[] imag_weight; // BeamFormer.str:152
  int numTapsMinusOne; // BeamFormer.str:153
  float[] realBuffer; // BeamFormer.str:154
  float[] imagBuffer; // BeamFormer.str:155
  int count; // BeamFormer.str:157
  int pos; // BeamFormer.str:159
  int numTaps; // BeamFormer.str:150
  int inputLength; // BeamFormer.str:150
  int decimationRatio; // BeamFormer.str:150
  public void work() { // BeamFormer.str:176
    float real_curr = 0; // BeamFormer.str:177
    float imag_curr = 0; // BeamFormer.str:178
    int i; // BeamFormer.str:179
    int modPos; // BeamFormer.str:180
    realBuffer[(numTapsMinusOne - pos)] = inputChannel.popFloat(); // BeamFormer.str:183
    imagBuffer[(numTapsMinusOne - pos)] = inputChannel.popFloat(); // BeamFormer.str:185
    modPos = (numTapsMinusOne - pos); // BeamFormer.str:188
    for (i = 0; (i < numTaps); i++) { // BeamFormer.str:189
      real_curr += ((realBuffer[modPos] * real_weight[i]) + (imagBuffer[modPos] * imag_weight[i])); // BeamFormer.str:190
      imag_curr += ((imagBuffer[modPos] * real_weight[i]) + (realBuffer[modPos] * imag_weight[i])); // BeamFormer.str:192
      modPos = ((modPos + 1) & numTapsMinusOne); // BeamFormer.str:195
    }; // BeamFormer.str:189
    pos = ((pos + 1) & numTapsMinusOne); // BeamFormer.str:199
    outputChannel.pushFloat(real_curr); // BeamFormer.str:202
    outputChannel.pushFloat(imag_curr); // BeamFormer.str:203
    for (i = 2; (i < (2 * decimationRatio)); i++) { // BeamFormer.str:206
      inputChannel.popFloat(); // BeamFormer.str:207
    }; // BeamFormer.str:206
    count += decimationRatio; // BeamFormer.str:211
    if ((count == inputLength)) { // BeamFormer.str:214
      count = 0; // BeamFormer.str:215
      pos = 0; // BeamFormer.str:216
      for (i = 0; (i < numTaps); i++) { // BeamFormer.str:217
        realBuffer[i] = 0; // BeamFormer.str:218
        imagBuffer[i] = 0; // BeamFormer.str:219
      }; // BeamFormer.str:217
    } // BeamFormer.str:214
  }
  public void init(final int _param_numTaps, final int _param_inputLength, final int _param_decimationRatio) { // BeamFormer.str:161
    numTaps = _param_numTaps; // BeamFormer.str:161
    inputLength = _param_inputLength; // BeamFormer.str:161
    decimationRatio = _param_decimationRatio; // BeamFormer.str:161
    real_weight = new float[numTaps]; // BeamFormer.str:151
    imag_weight = new float[numTaps]; // BeamFormer.str:152
    realBuffer = new float[numTaps]; // BeamFormer.str:154
    imagBuffer = new float[numTaps]; // BeamFormer.str:155
    setIOTypes(Float.TYPE, Float.TYPE); // BeamFormer.str:150
    addSteadyPhase((2 * decimationRatio), (2 * decimationRatio), 2, "work"); // BeamFormer.str:176
    int i; // BeamFormer.str:162
    numTapsMinusOne = (numTaps - 1); // BeamFormer.str:163
    pos = 0; // BeamFormer.str:164
    for (int j = 0; (j < numTaps); j++) { // BeamFormer.str:166
      int idx; // BeamFormer.str:167
      idx = (j + 1); // BeamFormer.str:167
      real_weight[j] = ((float)Math.sin(idx) / ((float)(idx))); // BeamFormer.str:169
      imag_weight[j] = ((float)Math.cos(idx) / ((float)(idx))); // BeamFormer.str:170
    }; // BeamFormer.str:166
  }
}
class Decimator extends Filter // BeamFormer.str:225
{
  private int __param__param_decimationFactor;
  public static Decimator __construct(int _param_decimationFactor)
  {
    Decimator __obj = new Decimator();
    __obj.__param__param_decimationFactor = _param_decimationFactor;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_decimationFactor);
  }
  int decimationFactor; // BeamFormer.str:225
  public void work() { // BeamFormer.str:227
    outputChannel.pushFloat(inputChannel.popFloat()); // BeamFormer.str:228
    outputChannel.pushFloat(inputChannel.popFloat()); // BeamFormer.str:229
    for (int i = 1; (i < decimationFactor); i++) { // BeamFormer.str:230
      inputChannel.popFloat(); // BeamFormer.str:231
      inputChannel.popFloat(); // BeamFormer.str:232
    }; // BeamFormer.str:230
  }
  public void init(final int _param_decimationFactor) { // BeamFormer.str:226
    decimationFactor = _param_decimationFactor; // BeamFormer.str:226
    setIOTypes(Float.TYPE, Float.TYPE); // BeamFormer.str:225
    addSteadyPhase((2 * decimationFactor), (2 * decimationFactor), 2, "work"); // BeamFormer.str:227
  }
}
class CoarseBeamFirFilter extends Filter // BeamFormer.str:237
{
  private int __param__param_numTaps;
  private int __param__param_inputLength;
  private int __param__param_decimationRatio;
  public static CoarseBeamFirFilter __construct(int _param_numTaps, int _param_inputLength, int _param_decimationRatio)
  {
    CoarseBeamFirFilter __obj = new CoarseBeamFirFilter();
    __obj.__param__param_numTaps = _param_numTaps;
    __obj.__param__param_inputLength = _param_inputLength;
    __obj.__param__param_decimationRatio = _param_decimationRatio;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_numTaps, __param__param_inputLength, __param__param_decimationRatio);
  }
  float[] real_weight; // BeamFormer.str:238
  float[] imag_weight; // BeamFormer.str:239
  int numTaps; // BeamFormer.str:237
  int inputLength; // BeamFormer.str:237
  int decimationRatio; // BeamFormer.str:237
  public void work() { // BeamFormer.str:257
    int min; // BeamFormer.str:259
    if ((numTaps < inputLength)) { // BeamFormer.str:260
      min = numTaps; // BeamFormer.str:261
    } else { // BeamFormer.str:262
      min = inputLength; // BeamFormer.str:263
    } // BeamFormer.str:260
    for (int i = 1; (i <= min); i++) { // BeamFormer.str:265
      float real_curr; // BeamFormer.str:266
      real_curr = 0; // BeamFormer.str:266
      float imag_curr; // BeamFormer.str:267
      imag_curr = 0; // BeamFormer.str:267
      for (int j = 0; (j < i); j++) { // BeamFormer.str:268
        int realIndex; // BeamFormer.str:269
        realIndex = (2 * ((i - j) - 1)); // BeamFormer.str:269
        int imagIndex; // BeamFormer.str:270
        imagIndex = (realIndex + 1); // BeamFormer.str:270
        real_curr += ((real_weight[j] * inputChannel.peekFloat(realIndex)) + (imag_weight[j] * inputChannel.peekFloat(imagIndex))); // BeamFormer.str:271
        imag_curr += ((real_weight[j] * inputChannel.peekFloat(imagIndex)) + (imag_weight[j] * inputChannel.peekFloat(realIndex))); // BeamFormer.str:272
      }; // BeamFormer.str:268
      outputChannel.pushFloat(real_curr); // BeamFormer.str:278
      outputChannel.pushFloat(imag_curr); // BeamFormer.str:279
    }; // BeamFormer.str:265
    for (int i = 0; (i < (inputLength - numTaps)); i++) { // BeamFormer.str:283
      inputChannel.popFloat(); // BeamFormer.str:285
      inputChannel.popFloat(); // BeamFormer.str:286
      float real_curr; // BeamFormer.str:287
      real_curr = 0; // BeamFormer.str:287
      float imag_curr; // BeamFormer.str:288
      imag_curr = 0; // BeamFormer.str:288
      for (int j = 0; (j < numTaps); j++) { // BeamFormer.str:289
        int realIndex; // BeamFormer.str:290
        realIndex = (2 * ((numTaps - j) - 1)); // BeamFormer.str:290
        int imagIndex; // BeamFormer.str:291
        imagIndex = (realIndex + 1); // BeamFormer.str:291
        real_curr += ((real_weight[j] * inputChannel.peekFloat(realIndex)) + (imag_weight[j] * inputChannel.peekFloat(imagIndex))); // BeamFormer.str:292
        imag_curr += ((real_weight[j] * inputChannel.peekFloat(imagIndex)) + (imag_weight[j] * inputChannel.peekFloat(realIndex))); // BeamFormer.str:293
      }; // BeamFormer.str:289
      outputChannel.pushFloat(real_curr); // BeamFormer.str:295
      outputChannel.pushFloat(imag_curr); // BeamFormer.str:296
    }; // BeamFormer.str:283
    for (int i = 0; (i < min); i++) { // BeamFormer.str:301
      inputChannel.popFloat(); // BeamFormer.str:302
      inputChannel.popFloat(); // BeamFormer.str:303
    }; // BeamFormer.str:301
  }
  public void init(final int _param_numTaps, final int _param_inputLength, final int _param_decimationRatio) { // BeamFormer.str:241
    numTaps = _param_numTaps; // BeamFormer.str:241
    inputLength = _param_inputLength; // BeamFormer.str:241
    decimationRatio = _param_decimationRatio; // BeamFormer.str:241
    real_weight = new float[numTaps]; // BeamFormer.str:238
    imag_weight = new float[numTaps]; // BeamFormer.str:239
    setIOTypes(Float.TYPE, Float.TYPE); // BeamFormer.str:237
    addSteadyPhase((2 * inputLength), (2 * inputLength), (2 * inputLength), "work"); // BeamFormer.str:257
    int i; // BeamFormer.str:242
    for (int j = 0; (j < numTaps); j++) { // BeamFormer.str:247
      int idx; // BeamFormer.str:248
      idx = (j + 1); // BeamFormer.str:248
      real_weight[j] = ((float)Math.sin(idx) / ((float)(idx))); // BeamFormer.str:250
      imag_weight[j] = ((float)Math.cos(idx) / ((float)(idx))); // BeamFormer.str:251
    }; // BeamFormer.str:247
  }
}
class BeamForm extends Filter // BeamFormer.str:308
{
  private int __param__param_myBeamId;
  private int __param__param_numChannels;
  public static BeamForm __construct(int _param_myBeamId, int _param_numChannels)
  {
    BeamForm __obj = new BeamForm();
    __obj.__param__param_myBeamId = _param_myBeamId;
    __obj.__param__param_numChannels = _param_numChannels;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_myBeamId, __param__param_numChannels);
  }
  float[] real_weight; // BeamFormer.str:309
  float[] imag_weight; // BeamFormer.str:310
  int myBeamId; // BeamFormer.str:308
  int numChannels; // BeamFormer.str:308
  public void work() { // BeamFormer.str:326
    float real_curr = 0; // BeamFormer.str:327
    float imag_curr = 0; // BeamFormer.str:328
    for (int i = 0; (i < numChannels); i++) { // BeamFormer.str:329
      float real_pop; // BeamFormer.str:330
      real_pop = inputChannel.popFloat(); // BeamFormer.str:330
      float imag_pop; // BeamFormer.str:331
      imag_pop = inputChannel.popFloat(); // BeamFormer.str:331
      real_curr += ((real_weight[i] * real_pop) - (imag_weight[i] * imag_pop)); // BeamFormer.str:333
      imag_curr += ((real_weight[i] * imag_pop) + (imag_weight[i] * real_pop)); // BeamFormer.str:335
    }; // BeamFormer.str:329
    outputChannel.pushFloat(real_curr); // BeamFormer.str:338
    outputChannel.pushFloat(imag_curr); // BeamFormer.str:339
  }
  public void init(final int _param_myBeamId, final int _param_numChannels) { // BeamFormer.str:312
    myBeamId = _param_myBeamId; // BeamFormer.str:312
    numChannels = _param_numChannels; // BeamFormer.str:312
    real_weight = new float[numChannels]; // BeamFormer.str:309
    imag_weight = new float[numChannels]; // BeamFormer.str:310
    setIOTypes(Float.TYPE, Float.TYPE); // BeamFormer.str:308
    addSteadyPhase((2 * numChannels), (2 * numChannels), 2, "work"); // BeamFormer.str:326
    for (int j = 0; (j < numChannels); j++) { // BeamFormer.str:316
      int idx; // BeamFormer.str:317
      idx = (j + 1); // BeamFormer.str:317
      real_weight[j] = ((float)Math.sin(idx) / ((float)((myBeamId + idx)))); // BeamFormer.str:319
      imag_weight[j] = ((float)Math.cos(idx) / ((float)((myBeamId + idx)))); // BeamFormer.str:320
    }; // BeamFormer.str:316
  }
}
class Magnitude extends Filter // BeamFormer.str:343
{
  public static Magnitude __construct()
  {
    Magnitude __obj = new Magnitude();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // BeamFormer.str:345
    float f1 = inputChannel.popFloat(); // BeamFormer.str:346
    float f2 = inputChannel.popFloat(); // BeamFormer.str:347
    outputChannel.pushFloat(mag(f1, f2)); // BeamFormer.str:348
  }
  public float mag(float real, float imag) { // BeamFormer.str:354
    return ((float)((float)Math.sqrt(((real * real) + (imag * imag))))); // BeamFormer.str:355
  }
  public void init() { // BeamFormer.str:344
    setIOTypes(Float.TYPE, Float.TYPE); // BeamFormer.str:343
    addSteadyPhase(2, 2, 1, "work"); // BeamFormer.str:345
  }
}
class Detector extends Filter // BeamFormer.str:361
{
  private int __param__param__myBeam;
  private int __param__param_numSamples;
  private int __param__param_targetBeam;
  private int __param__param_targetSample;
  private float __param__param_cfarThreshold;
  public static Detector __construct(int _param__myBeam, int _param_numSamples, int _param_targetBeam, int _param_targetSample, float _param_cfarThreshold)
  {
    Detector __obj = new Detector();
    __obj.__param__param__myBeam = _param__myBeam;
    __obj.__param__param_numSamples = _param_numSamples;
    __obj.__param__param_targetBeam = _param_targetBeam;
    __obj.__param__param_targetSample = _param_targetSample;
    __obj.__param__param_cfarThreshold = _param_cfarThreshold;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param__myBeam, __param__param_numSamples, __param__param_targetBeam, __param__param_targetSample, __param__param_cfarThreshold);
  }
  int curSample; // BeamFormer.str:364
  int myBeam; // BeamFormer.str:365
  boolean holdsTarget; // BeamFormer.str:366
  float thresh; // BeamFormer.str:367
  int _myBeam; // BeamFormer.str:361
  int numSamples; // BeamFormer.str:361
  int targetBeam; // BeamFormer.str:361
  int targetSample; // BeamFormer.str:361
  float cfarThreshold; // BeamFormer.str:361
  public void work() { // BeamFormer.str:378
    float inputVal = inputChannel.popFloat(); // BeamFormer.str:379
    float outputVal; // BeamFormer.str:380
    if ((holdsTarget && (targetSample == curSample))) { // BeamFormer.str:381
      if (!(inputVal >= thresh)) { // BeamFormer.str:382
        outputVal = 0; // BeamFormer.str:383
      } else { // BeamFormer.str:384
        outputVal = myBeam; // BeamFormer.str:385
      } // BeamFormer.str:382
    } else { // BeamFormer.str:387
      if (!(inputVal >= thresh)) { // BeamFormer.str:388
        outputVal = 0; // BeamFormer.str:389
      } else { // BeamFormer.str:390
        outputVal = -myBeam; // BeamFormer.str:391
      } // BeamFormer.str:388
    } // BeamFormer.str:381
    outputVal = inputVal; // BeamFormer.str:395
    System.out.println(outputVal); // BeamFormer.str:399
    curSample++; // BeamFormer.str:402
    if ((curSample >= numSamples)) { // BeamFormer.str:405
      curSample = 0; // BeamFormer.str:405
    } // BeamFormer.str:404
    outputChannel.pushFloat(outputVal); // BeamFormer.str:406
  }
  public void init(final int _param__myBeam, final int _param_numSamples, final int _param_targetBeam, final int _param_targetSample, final float _param_cfarThreshold) { // BeamFormer.str:369
    _myBeam = _param__myBeam; // BeamFormer.str:369
    numSamples = _param_numSamples; // BeamFormer.str:369
    targetBeam = _param_targetBeam; // BeamFormer.str:369
    targetSample = _param_targetSample; // BeamFormer.str:369
    cfarThreshold = _param_cfarThreshold; // BeamFormer.str:369
    setIOTypes(Float.TYPE, Float.TYPE); // BeamFormer.str:361
    addSteadyPhase(1, 1, 1, "work"); // BeamFormer.str:376
    curSample = 0; // BeamFormer.str:370
    holdsTarget = (_myBeam == targetBeam); // BeamFormer.str:371
    myBeam = (_myBeam + 1); // BeamFormer.str:372
    thresh = 0.1f; // BeamFormer.str:373
  }
}
class AnonFilter_a0 extends Pipeline // BeamFormer.str:65
{
  private float __param_cfarThreshold;
  private int __param_coarseDecimationRatio;
  private int __param_fineDecimationRatio;
  private int __param_i;
  private int __param_numCoarseFilterTaps;
  private int __param_numFineFilterTaps;
  private int __param_numPostDec1;
  private int __param_numSamples;
  private int __param_targetBeam;
  private int __param_targetSample;
  public static AnonFilter_a0 __construct(float cfarThreshold, int coarseDecimationRatio, int fineDecimationRatio, int i, int numCoarseFilterTaps, int numFineFilterTaps, int numPostDec1, int numSamples, int targetBeam, int targetSample)
  {
    AnonFilter_a0 __obj = new AnonFilter_a0();
    __obj.__param_cfarThreshold = cfarThreshold;
    __obj.__param_coarseDecimationRatio = coarseDecimationRatio;
    __obj.__param_fineDecimationRatio = fineDecimationRatio;
    __obj.__param_i = i;
    __obj.__param_numCoarseFilterTaps = numCoarseFilterTaps;
    __obj.__param_numFineFilterTaps = numFineFilterTaps;
    __obj.__param_numPostDec1 = numPostDec1;
    __obj.__param_numSamples = numSamples;
    __obj.__param_targetBeam = targetBeam;
    __obj.__param_targetSample = targetSample;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_cfarThreshold, __param_coarseDecimationRatio, __param_fineDecimationRatio, __param_i, __param_numCoarseFilterTaps, __param_numFineFilterTaps, __param_numPostDec1, __param_numSamples, __param_targetBeam, __param_targetSample);
  }
  public void init(final float cfarThreshold, final int coarseDecimationRatio, final int fineDecimationRatio, final int i, final int numCoarseFilterTaps, final int numFineFilterTaps, final int numPostDec1, final int numSamples, final int targetBeam, final int targetSample) { // BeamFormer.str:65
    add(InputGenerate.__construct(i, numSamples, targetBeam, targetSample, cfarThreshold)); // BeamFormer.str:66
    add(BeamFirFilter.__construct(numCoarseFilterTaps, numSamples, coarseDecimationRatio)); // BeamFormer.str:69
    add(BeamFirFilter.__construct(numFineFilterTaps, numPostDec1, fineDecimationRatio)); // BeamFormer.str:74
  }
}
class AnonFilter_a1 extends SplitJoin // BeamFormer.str:61
{
  private float __param_cfarThreshold;
  private int __param_coarseDecimationRatio;
  private int __param_fineDecimationRatio;
  private int __param_numChannels;
  private int __param_numCoarseFilterTaps;
  private int __param_numFineFilterTaps;
  private int __param_numPostDec1;
  private int __param_numSamples;
  private int __param_targetBeam;
  private int __param_targetSample;
  public static AnonFilter_a1 __construct(float cfarThreshold, int coarseDecimationRatio, int fineDecimationRatio, int numChannels, int numCoarseFilterTaps, int numFineFilterTaps, int numPostDec1, int numSamples, int targetBeam, int targetSample)
  {
    AnonFilter_a1 __obj = new AnonFilter_a1();
    __obj.__param_cfarThreshold = cfarThreshold;
    __obj.__param_coarseDecimationRatio = coarseDecimationRatio;
    __obj.__param_fineDecimationRatio = fineDecimationRatio;
    __obj.__param_numChannels = numChannels;
    __obj.__param_numCoarseFilterTaps = numCoarseFilterTaps;
    __obj.__param_numFineFilterTaps = numFineFilterTaps;
    __obj.__param_numPostDec1 = numPostDec1;
    __obj.__param_numSamples = numSamples;
    __obj.__param_targetBeam = targetBeam;
    __obj.__param_targetSample = targetSample;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_cfarThreshold, __param_coarseDecimationRatio, __param_fineDecimationRatio, __param_numChannels, __param_numCoarseFilterTaps, __param_numFineFilterTaps, __param_numPostDec1, __param_numSamples, __param_targetBeam, __param_targetSample);
  }
  public void init(final float cfarThreshold, final int coarseDecimationRatio, final int fineDecimationRatio, final int numChannels, final int numCoarseFilterTaps, final int numFineFilterTaps, final int numPostDec1, final int numSamples, final int targetBeam, final int targetSample) { // BeamFormer.str:61
    setSplitter(ROUND_ROBIN(1)); // BeamFormer.str:63
    for (int i = 0; (i < numChannels); i++) { // BeamFormer.str:64
      add(AnonFilter_a0.__construct(cfarThreshold, coarseDecimationRatio, fineDecimationRatio, i, numCoarseFilterTaps, numFineFilterTaps, numPostDec1, numSamples, targetBeam, targetSample)); // BeamFormer.str:65
    }; // BeamFormer.str:64
    setJoiner(ROUND_ROBIN(2)); // BeamFormer.str:78
  }
}
class AnonFilter_a2 extends Pipeline // BeamFormer.str:84
{
  private float __param_cfarThreshold;
  private int __param_i;
  private int __param_mfSize;
  private int __param_numChannels;
  private int __param_numPostDec2;
  private int __param_targetBeam;
  private int __param_targetSamplePostDec;
  public static AnonFilter_a2 __construct(float cfarThreshold, int i, int mfSize, int numChannels, int numPostDec2, int targetBeam, int targetSamplePostDec)
  {
    AnonFilter_a2 __obj = new AnonFilter_a2();
    __obj.__param_cfarThreshold = cfarThreshold;
    __obj.__param_i = i;
    __obj.__param_mfSize = mfSize;
    __obj.__param_numChannels = numChannels;
    __obj.__param_numPostDec2 = numPostDec2;
    __obj.__param_targetBeam = targetBeam;
    __obj.__param_targetSamplePostDec = targetSamplePostDec;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_cfarThreshold, __param_i, __param_mfSize, __param_numChannels, __param_numPostDec2, __param_targetBeam, __param_targetSamplePostDec);
  }
  public void init(final float cfarThreshold, final int i, final int mfSize, final int numChannels, final int numPostDec2, final int targetBeam, final int targetSamplePostDec) { // BeamFormer.str:84
    add(BeamForm.__construct(i, numChannels)); // BeamFormer.str:85
    add(BeamFirFilter.__construct(mfSize, numPostDec2, 1)); // BeamFormer.str:89
    add(Magnitude.__construct()); // BeamFormer.str:91
    add(Detector.__construct(i, numPostDec2, targetBeam, targetSamplePostDec, cfarThreshold)); // BeamFormer.str:95
  }
}
class AnonFilter_a3 extends SplitJoin // BeamFormer.str:81
{
  private float __param_cfarThreshold;
  private int __param_mfSize;
  private int __param_numBeams;
  private int __param_numChannels;
  private int __param_numPostDec2;
  private int __param_targetBeam;
  private int __param_targetSamplePostDec;
  public static AnonFilter_a3 __construct(float cfarThreshold, int mfSize, int numBeams, int numChannels, int numPostDec2, int targetBeam, int targetSamplePostDec)
  {
    AnonFilter_a3 __obj = new AnonFilter_a3();
    __obj.__param_cfarThreshold = cfarThreshold;
    __obj.__param_mfSize = mfSize;
    __obj.__param_numBeams = numBeams;
    __obj.__param_numChannels = numChannels;
    __obj.__param_numPostDec2 = numPostDec2;
    __obj.__param_targetBeam = targetBeam;
    __obj.__param_targetSamplePostDec = targetSamplePostDec;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_cfarThreshold, __param_mfSize, __param_numBeams, __param_numChannels, __param_numPostDec2, __param_targetBeam, __param_targetSamplePostDec);
  }
  public void init(final float cfarThreshold, final int mfSize, final int numBeams, final int numChannels, final int numPostDec2, final int targetBeam, final int targetSamplePostDec) { // BeamFormer.str:81
    setSplitter(DUPLICATE()); // BeamFormer.str:82
    for (int i = 0; (i < numBeams); i++) { // BeamFormer.str:83
      add(AnonFilter_a2.__construct(cfarThreshold, i, mfSize, numChannels, numPostDec2, targetBeam, targetSamplePostDec)); // BeamFormer.str:84
    }; // BeamFormer.str:83
    setJoiner(ROUND_ROBIN(1)); // BeamFormer.str:101
  }
}
public class BeamFormer extends StreamItPipeline // BeamFormer.str:1
{
  public void init() { // BeamFormer.str:1
    add(RandomSource.__construct()); // BeamFormer.str:3
    add(BeamFormerMain.__construct()); // BeamFormer.str:4
    add(FloatPrinter.__construct()); // BeamFormer.str:5
  }
  public static void main(String[] args) {
    BeamFormer program = new BeamFormer();
    program.run(args);
    FileWriter.closeAll();
  }
}
