import streamit.library.*;
import streamit.library.io.*;
class Complex extends Structure {
  float real;
  float imag;
}
class FloatNAdder extends Filter
{
  int count; // FMTest.str:52
  public void work() { // FMTest.str:54
    float sum = 0.0f; // FMTest.str:56
    for (int i = 0; (i < count); i++) { // FMTest.str:59
      float __temp_var_0; // FMTest.str:59
      __temp_var_0 = input.popFloat(); // FMTest.str:59
      sum += __temp_var_0; // FMTest.str:59
    }; // FMTest.str:58
    output.pushFloat(sum); // FMTest.str:61
  }
  public FloatNAdder(int count) { // FMTest.str:52
    super(count); // FMTest.str:52
  }
  public void init(final int _param_count) { // FMTest.str:52
    count = _param_count; // FMTest.str:52
    input = new Channel(Float.TYPE, count); // FMTest.str:54
    output = new Channel(Float.TYPE, 1); // FMTest.str:54
  }
}
class FloatDiff extends Filter
{
  public void work() { // FMTest.str:68
    float __temp_var_1 = input.peekFloat(0); // FMTest.str:70
    float __temp_var_2 = input.peekFloat(1); // FMTest.str:70
    output.pushFloat((__temp_var_1 - __temp_var_2)); // FMTest.str:70
    float __temp_var_3 = input.popFloat(); // FMTest.str:71
    float __temp_var_4 = input.popFloat(); // FMTest.str:72
  }
  public void init() { // FMTest.str:66
    input = new Channel(Float.TYPE, 2); // FMTest.str:68
    output = new Channel(Float.TYPE, 1); // FMTest.str:68
  }
}
class FloatDup extends Filter
{
  public void work() { // FMTest.str:79
    float val; // FMTest.str:81
    float __temp_var_5 = input.popFloat(); // FMTest.str:81
    val = __temp_var_5; // FMTest.str:81
    output.pushFloat(val); // FMTest.str:82
    output.pushFloat(val); // FMTest.str:83
  }
  public void init() { // FMTest.str:77
    input = new Channel(Float.TYPE, 1); // FMTest.str:79
    output = new Channel(Float.TYPE, 2); // FMTest.str:79
  }
}
class EqualizerInnerPipeline extends Pipeline
{
  public EqualizerInnerPipeline(float rate, float freq) { // FMTest.str:88
    super(rate, freq); // FMTest.str:88
  }
  public void init(final float rate, final float freq) { // FMTest.str:88
    add(new LowPassFilter(rate, freq, 64, 0)); // FMTest.str:90
    add(new FloatDup()); // FMTest.str:91
  }
}
class EqualizerInnerSplitJoin extends SplitJoin
{
  public EqualizerInnerSplitJoin(float rate, float low, float high, int bands) { // FMTest.str:95
    super(rate, low, high, bands); // FMTest.str:95
  }
  public void init(final float rate, final float low, final float high, final int bands) { // FMTest.str:95
    setSplitter(DUPLICATE()); // FMTest.str:97
    for (int i = 0; (i < (bands - 1)); i++) { // FMTest.str:99
      add(new EqualizerInnerPipeline(rate, ((float)((float)Math.exp(((((i + 1) * ((float)Math.log(high) - (float)Math.log(low))) / bands) + (float)Math.log(low))))))); // FMTest.str:99
    }; // FMTest.str:98
    setJoiner(ROUND_ROBIN(2)); // FMTest.str:100
  }
}
class EqualizerSplitJoin extends SplitJoin
{
  public EqualizerSplitJoin(float rate, float low, float high, int bands) { // FMTest.str:104
    super(rate, low, high, bands); // FMTest.str:104
  }
  public void init(final float rate, final float low, final float high, final int bands) { // FMTest.str:104
    setSplitter(DUPLICATE()); // FMTest.str:106
    add(new LowPassFilter(rate, high, 64, 0)); // FMTest.str:107
    add(new EqualizerInnerSplitJoin(rate, low, high, bands)); // FMTest.str:108
    add(new LowPassFilter(rate, low, 64, 0)); // FMTest.str:109
    setJoiner(WEIGHTED_ROUND_ROBIN(1, ((bands - 1) * 2), 1)); // FMTest.str:110
  }
}
class Equalizer extends Pipeline
{
  public Equalizer(float rate) { // FMTest.str:114
    super(rate); // FMTest.str:114
  }
  public void init(final float rate) { // FMTest.str:114
    int bands = 10; // FMTest.str:116
    float low = 55; // FMTest.str:117
    float high = 1760; // FMTest.str:118
    add(new EqualizerSplitJoin(rate, low, high, bands)); // FMTest.str:120
    add(new FloatDiff()); // FMTest.str:121
    add(new FloatNAdder(bands)); // FMTest.str:122
  }
}
class LowPassFilter extends Filter
{
  float[] COEFF; // FMTest.str:128
  float tapTotal; // FMTest.str:129
  float sampleRate; // FMTest.str:126
  float cutFreq; // FMTest.str:126
  int numTaps; // FMTest.str:126
  int decimation; // FMTest.str:126
  public void work() { // FMTest.str:169
    float sum = 0.0f; // FMTest.str:170
    for (int i = 0; (i < numTaps); i++) { // FMTest.str:171
      float __temp_var_6; // FMTest.str:172
      __temp_var_6 = input.peekFloat(i); // FMTest.str:172
      sum += (__temp_var_6 * COEFF[i]); // FMTest.str:172
    }; // FMTest.str:171
    float __temp_var_7 = input.popFloat(); // FMTest.str:174
    for (int i = 0; (i < decimation); i++) { // FMTest.str:176
      float __temp_var_8; // FMTest.str:176
      __temp_var_8 = input.popFloat(); // FMTest.str:176
    }; // FMTest.str:175
    output.pushFloat(sum); // FMTest.str:178
  }
  public LowPassFilter(float sampleRate, float cutFreq, int numTaps, int decimation) { // FMTest.str:126
    super(sampleRate, cutFreq, numTaps, decimation); // FMTest.str:126
  }
  public void init(final float _param_sampleRate, final float _param_cutFreq, final int _param_numTaps, final int _param_decimation) { // FMTest.str:131
    sampleRate = _param_sampleRate; // FMTest.str:131
    cutFreq = _param_cutFreq; // FMTest.str:131
    numTaps = _param_numTaps; // FMTest.str:131
    decimation = _param_decimation; // FMTest.str:131
    COEFF = new float[numTaps]; // FMTest.str:128
    input = new Channel(Float.TYPE, (decimation + 1), numTaps); // FMTest.str:169
    output = new Channel(Float.TYPE, 1); // FMTest.str:169
    float m = (numTaps - 1); // FMTest.str:132
    if ((cutFreq == 0.0f)) { // FMTest.str:135
      tapTotal = 0; // FMTest.str:138
      for (int i = 0; (i < numTaps); i++) { // FMTest.str:140
        COEFF[i] = ((float)((0.54f - (0.46f * (float)Math.cos(((2 * 3.141592653589793f) * (i / m))))))); // FMTest.str:141
        tapTotal = (tapTotal + COEFF[i]); // FMTest.str:142
      }; // FMTest.str:140
      for (int i = 0; (i < numTaps); i++) { // FMTest.str:146
        COEFF[i] = (COEFF[i] / tapTotal); // FMTest.str:147
      }; // FMTest.str:146
    } else { // FMTest.str:150
      float w; // FMTest.str:155
      w = (((2 * 3.141592653589793f) * cutFreq) / sampleRate); // FMTest.str:155
      for (int i = 0; (i < numTaps); i++) { // FMTest.str:157
        if (((i - (m / 2)) == 0)) { // FMTest.str:160
          COEFF[i] = (w / 3.141592653589793f); // FMTest.str:160
        } else { // FMTest.str:162
          COEFF[i] = ((float)(((((float)Math.sin((w * (i - (m / 2)))) / 3.141592653589793f) / (i - (m / 2))) * (0.54f - (0.46f * (float)Math.cos(((2 * 3.141592653589793f) * (i / m)))))))); // FMTest.str:162
        }; // FMTest.str:159
      }; // FMTest.str:157
    }; // FMTest.str:135
  }
}
class FMDemodulator extends Filter
{
  float mGain; // FMTest.str:186
  float sampRate; // FMTest.str:184
  float max; // FMTest.str:184
  float bandwidth; // FMTest.str:184
  public void work() { // FMTest.str:192
    float temp = 0; // FMTest.str:193
    float __temp_var_9 = input.peekFloat(0); // FMTest.str:195
    float __temp_var_10 = input.peekFloat(1); // FMTest.str:195
    temp = ((float)((__temp_var_9 * __temp_var_10))); // FMTest.str:195
    temp = ((float)((mGain * (float)Math.atan(temp)))); // FMTest.str:197
    float __temp_var_11 = input.popFloat(); // FMTest.str:199
    output.pushFloat(temp); // FMTest.str:200
  }
  public FMDemodulator(float sampRate, float max, float bandwidth) { // FMTest.str:184
    super(sampRate, max, bandwidth); // FMTest.str:184
  }
  public void init(final float _param_sampRate, final float _param_max, final float _param_bandwidth) { // FMTest.str:188
    sampRate = _param_sampRate; // FMTest.str:188
    max = _param_max; // FMTest.str:188
    bandwidth = _param_bandwidth; // FMTest.str:188
    input = new Channel(Float.TYPE, 1, 2); // FMTest.str:192
    output = new Channel(Float.TYPE, 1); // FMTest.str:192
    mGain = (max * (sampRate / (bandwidth * 3.141592653589793f))); // FMTest.str:189
  }
}
class FloatOneSource extends Filter
{
  float x; // FMTest.str:207
  public void work() { // FMTest.str:213
    output.pushFloat(x); // FMTest.str:214
    x = (x + 1); // FMTest.str:214
  }
  public void init() { // FMTest.str:209
    output = new Channel(Float.TYPE, 1); // FMTest.str:213
    x = 0; // FMTest.str:210
  }
}
class FloatPrinter extends Filter
{
  public void work() { // FMTest.str:221
    float __temp_var_12 = input.popFloat(); // FMTest.str:222
    System.out.println(__temp_var_12); // FMTest.str:222
  }
  public void init() { // FMTest.str:219
    input = new Channel(Float.TYPE, 1); // FMTest.str:221
  }
}
class FMRadio extends Pipeline
{
  public void init() { // FMTest.str:232
    float samplingRate = 250000000; // FMTest.str:235
    float cutoffFrequency = 108000000; // FMTest.str:236
    int numberOfTaps = 64; // FMTest.str:237
    float maxAmplitude = 27000; // FMTest.str:238
    float bandwidth = 10000; // FMTest.str:239
    add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps, 4)); // FMTest.str:241
    add(new FMDemodulator(samplingRate, maxAmplitude, bandwidth)); // FMTest.str:242
    add(new Equalizer(samplingRate)); // FMTest.str:243
  }
}
public class FMTest extends StreamItPipeline
{
  public static void main(String[] args) {
    FMTest program = new FMTest();
    program.run(args);
  }
  public void init() { // FMTest.str:247
    add(new FloatOneSource()); // FMTest.str:249
    add(new FMRadio()); // FMTest.str:250
    add(new FloatPrinter()); // FMTest.str:251
  }
}
