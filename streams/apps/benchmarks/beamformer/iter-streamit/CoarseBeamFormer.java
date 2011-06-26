import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class float2 extends Structure implements Serializable {
  float x;
  float y;
}
class float3 extends Structure implements Serializable {
  float x;
  float y;
  float z;
}
class float4 extends Structure implements Serializable {
  float x;
  float y;
  float z;
  float w;
}
class StreamItVectorLib {
  public static native float2 add2(float2 a, float2 b);
  public static native float3 add3(float3 a, float3 b);
  public static native float4 add4(float4 a, float4 b);
  public static native float2 sub2(float2 a, float2 b);
  public static native float3 sub3(float3 a, float3 b);
  public static native float4 sub4(float4 a, float4 b);
  public static native float2 mul2(float2 a, float2 b);
  public static native float3 mul3(float3 a, float3 b);
  public static native float4 mul4(float4 a, float4 b);
  public static native float2 div2(float2 a, float2 b);
  public static native float3 div3(float3 a, float3 b);
  public static native float4 div4(float4 a, float4 b);
  public static native float2 addScalar2(float2 a, float b);
  public static native float3 addScalar3(float3 a, float b);
  public static native float4 addScalar4(float4 a, float b);
  public static native float2 subScalar2(float2 a, float b);
  public static native float3 subScalar3(float3 a, float b);
  public static native float4 subScalar4(float4 a, float b);
  public static native float2 scale2(float2 a, float b);
  public static native float3 scale3(float3 a, float b);
  public static native float4 scale4(float4 a, float b);
  public static native float2 scaleInv2(float2 a, float b);
  public static native float3 scaleInv3(float3 a, float b);
  public static native float4 scaleInv4(float4 a, float b);
  public static native float sqrtDist2(float2 a, float2 b);
  public static native float sqrtDist3(float3 a, float3 b);
  public static native float sqrtDist4(float4 a, float4 b);
  public static native float dot3(float3 a, float3 b);
  public static native float3 cross3(float3 a, float3 b);
  public static native float2 max2(float2 a, float2 b);
  public static native float3 max3(float3 a, float3 b);
  public static native float2 min2(float2 a, float2 b);
  public static native float3 min3(float3 a, float3 b);
  public static native float2 neg2(float2 a);
  public static native float3 neg3(float3 a);
  public static native float4 neg4(float4 a);
  public static native float2 floor2(float2 a);
  public static native float3 floor3(float3 a);
  public static native float4 floor4(float4 a);
  public static native float2 normalize2(float2 a);
  public static native float3 normalize3(float3 a);
  public static native float4 normalize4(float4 a);
  public static native boolean greaterThan3(float3 a, float3 b);
  public static native boolean lessThan3(float3 a, float3 b);
  public static native boolean equals3(float3 a, float3 b);
}
class InputGenerate extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
{
  public InputGenerate(int _param_myChannel, int _param_numberOfSamples, int _param_tarBeam, int _param_targetSample, float _param_thresh)
  {
  }
  int curSample; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:47
  boolean holdsTarget; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:48
  int myChannel; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
  int numberOfSamples; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
  int tarBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
  int targetSample; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
  float thresh; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:53
    if ((holdsTarget && (curSample == targetSample))) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:54
      outputChannel.pushFloat((float)Math.sqrt((curSample * myChannel))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:55
      outputChannel.pushFloat(((float)Math.sqrt((curSample * myChannel)) + 1)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:56
    } else { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:57
      outputChannel.pushFloat(-(float)Math.sqrt((curSample * myChannel))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:58
      outputChannel.pushFloat(-((float)Math.sqrt((curSample * myChannel)) + 1)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:59
    } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:54
    curSample++; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:61
    if ((curSample >= numberOfSamples)) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:62
      curSample = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:63
    } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:62
  }
  public void init(final int _param_myChannel, final int _param_numberOfSamples, final int _param_tarBeam, final int _param_targetSample, final float _param_thresh) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:49
    myChannel = _param_myChannel; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:49
    numberOfSamples = _param_numberOfSamples; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:49
    tarBeam = _param_tarBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:49
    targetSample = _param_targetSample; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:49
    thresh = _param_thresh; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:49
    setIOTypes(Void.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:46
    addSteadyPhase(0, 0, 2, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:53
    curSample = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:50
    holdsTarget = (tarBeam == myChannel); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:51
  }
}
class FloatPrinter extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:67
{
  public FloatPrinter()
  {
  }
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:68
    System.out.println(inputChannel.popFloat()); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:68
  }
  public void init() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:67
    setIOTypes(Float.TYPE, Void.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:67
    addSteadyPhase(1, 1, 0, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:68
  }
}
class BeamFirFilter extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:70
{
  public BeamFirFilter(int _param_numTaps, int _param_inputLength, int _param_decimationRatio)
  {
  }
  float[numTaps] real_weight; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:71
  float[numTaps] imag_weight; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:72
  int numTapsMinusOne; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:73
  float[numTaps] realBuffer; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:74
  float[numTaps] imagBuffer; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:75
  int count; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:76
  int pos; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:77
  int numTaps; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:70
  int inputLength; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:70
  int decimationRatio; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:70
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:88
    float real_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:89
    float imag_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:90
    int i; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:91
    int modPos; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:92
    realBuffer[(numTapsMinusOne - pos)] = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:93
    imagBuffer[(numTapsMinusOne - pos)] = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:94
    modPos = (numTapsMinusOne - pos); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:95
    for (i = 0; (i < numTaps); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:96
      real_curr += ((realBuffer[modPos] * real_weight[i]) + (imagBuffer[modPos] * imag_weight[i])); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:97
      imag_curr += ((imagBuffer[modPos] * real_weight[i]) + (realBuffer[modPos] * imag_weight[i])); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:99
      modPos = ((modPos + 1) & numTapsMinusOne); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:101
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:96
    pos = ((pos + 1) & numTapsMinusOne); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:103
    outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:104
    outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:105
    for (i = 2; (i < (2 * decimationRatio)); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:106
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:107
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:106
    count += decimationRatio; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:109
    if ((count == inputLength)) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:110
      count = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:111
      pos = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:112
      for (i = 0; (i < numTaps); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:113
        realBuffer[i] = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:114
        imagBuffer[i] = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:115
      }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:113
    } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:110
  }
  public void init(final int _param_numTaps, final int _param_inputLength, final int _param_decimationRatio) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:78
    numTaps = _param_numTaps; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:78
    inputLength = _param_inputLength; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:78
    decimationRatio = _param_decimationRatio; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:78
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:70
    addSteadyPhase((2 * decimationRatio), (2 * decimationRatio), 2, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:88
    int i; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:79
    numTapsMinusOne = (numTaps - 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:80
    pos = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:81
    for (int j = 0; (j < numTaps); j++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:82
      int idx; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:83
      idx = (j + 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:83
      real_weight[j] = ((float)Math.sin(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:84
      imag_weight[j] = ((float)Math.cos(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:85
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:82
  }
}
class Decimator extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:120
{
  public Decimator(int _param_decimationFactor)
  {
  }
  int decimationFactor; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:120
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:122
    outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:123
    outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:124
    for (int i = 1; (i < decimationFactor); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:125
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:126
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:127
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:125
  }
  public void init(final int _param_decimationFactor) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:121
    decimationFactor = _param_decimationFactor; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:121
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:120
    addSteadyPhase((2 * decimationFactor), (2 * decimationFactor), 2, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:122
  }
}
class CoarseBeamFirFilter extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:131
{
  public CoarseBeamFirFilter(int _param_numTaps, int _param_inputLength, int _param_decimationRatio)
  {
  }
  float[numTaps] real_weight; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:132
  float[numTaps] imag_weight; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:133
  int numTaps; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:131
  int inputLength; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:131
  int decimationRatio; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:131
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:142
    int min; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:143
    if ((numTaps < inputLength)) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:144
      min = numTaps; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:145
    } else { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:146
      min = inputLength; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:147
    } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:144
    for (int i = 1; (i <= min); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:149
      float real_curr; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:150
      real_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:150
      float imag_curr; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:151
      imag_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:151
      for (int j = 0; (j < i); j++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:152
        int realIndex; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:153
        realIndex = (2 * ((i - j) - 1)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:153
        int imagIndex; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:154
        imagIndex = (realIndex + 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:154
        real_curr += ((real_weight[j] * inputChannel.peekFloat(realIndex)) + (imag_weight[j] * inputChannel.peekFloat(imagIndex))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:155
        imag_curr += ((real_weight[j] * inputChannel.peekFloat(imagIndex)) + (imag_weight[j] * inputChannel.peekFloat(realIndex))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:156
      }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:152
      outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:158
      outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:159
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:149
    for (int i = 0; (i < (inputLength - numTaps)); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:161
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:162
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:163
      float real_curr; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:164
      real_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:164
      float imag_curr; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:165
      imag_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:165
      for (int j = 0; (j < numTaps); j++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:166
        int realIndex; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:167
        realIndex = (2 * ((numTaps - j) - 1)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:167
        int imagIndex; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:168
        imagIndex = (realIndex + 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:168
        real_curr += ((real_weight[j] * inputChannel.peekFloat(realIndex)) + (imag_weight[j] * inputChannel.peekFloat(imagIndex))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:169
        imag_curr += ((real_weight[j] * inputChannel.peekFloat(imagIndex)) + (imag_weight[j] * inputChannel.peekFloat(realIndex))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:170
      }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:166
      outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:172
      outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:173
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:161
    for (int i = 0; (i < min); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:175
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:176
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:177
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:175
  }
  public void init(final int _param_numTaps, final int _param_inputLength, final int _param_decimationRatio) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:134
    numTaps = _param_numTaps; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:134
    inputLength = _param_inputLength; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:134
    decimationRatio = _param_decimationRatio; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:134
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:131
    addSteadyPhase((2 * inputLength), (2 * inputLength), (2 * inputLength), "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:142
    int i; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:135
    for (int j = 0; (j < numTaps); j++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:136
      int idx; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:137
      idx = (j + 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:137
      real_weight[j] = ((float)Math.sin(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:138
      imag_weight[j] = ((float)Math.cos(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:139
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:136
  }
}
class BeamForm extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:181
{
  public BeamForm(int _param_myBeamId, int _param_numChannels)
  {
  }
  float[numChannels] real_weight; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:182
  float[numChannels] imag_weight; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:183
  int myBeamId; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:181
  int numChannels; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:181
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:191
    float real_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:192
    float imag_curr = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:193
    for (int i = 0; (i < numChannels); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:194
      float real_pop; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:195
      real_pop = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:195
      float imag_pop; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:196
      imag_pop = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:196
      real_curr += ((real_weight[i] * real_pop) - (imag_weight[i] * imag_pop)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:197
      imag_curr += ((real_weight[i] * imag_pop) + (imag_weight[i] * real_pop)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:199
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:194
    outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:202
    outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:203
  }
  public void init(final int _param_myBeamId, final int _param_numChannels) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:184
    myBeamId = _param_myBeamId; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:184
    numChannels = _param_numChannels; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:184
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:181
    addSteadyPhase((2 * numChannels), (2 * numChannels), 2, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:191
    for (int j = 0; (j < numChannels); j++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:185
      int idx; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:186
      idx = (j + 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:186
      real_weight[j] = ((float)Math.sin(idx) / ((float)((myBeamId + idx)))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:187
      imag_weight[j] = ((float)Math.cos(idx) / ((float)((myBeamId + idx)))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:188
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:185
  }
}
class Magnitude extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:206
{
  public Magnitude()
  {
  }
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:208
    float f1 = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:209
    float f2 = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:210
    outputChannel.pushFloat(mag(f1, f2)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:211
  }
  public float mag(float real, float imag) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:213
    return ((float)((float)Math.sqrt(((real * real) + (imag * imag))))); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:214
  }
  public void init() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:207
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:206
    addSteadyPhase(2, 2, 1, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:208
  }
}
class Detector extends Filter // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
{
  public Detector(int _param__myBeam, int _param_numSamples, int _param_targetBeam, int _param_targetSample, float _param_cfarThreshold)
  {
  }
  int curSample; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:219
  int myBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:220
  boolean holdsTarget; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:221
  float thresh; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:222
  int _myBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
  int numSamples; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
  int targetBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
  int targetSample; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
  float cfarThreshold; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
  public void work() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:230
    float inputVal = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:231
    float outputVal; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:232
    if ((holdsTarget && (targetSample == curSample))) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:233
      if (!(inputVal >= thresh)) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:234
        outputVal = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:235
      } else { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:236
        outputVal = myBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:237
      } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:234
    } else { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:239
      if (!(inputVal >= thresh)) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:240
        outputVal = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:241
      } else { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:242
        outputVal = -myBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:243
      } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:240
    } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:233
    outputVal = inputVal; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:246
    System.out.println(outputVal); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:247
    curSample++; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:248
    if ((curSample >= numSamples)) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:250
      curSample = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:250
    } // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:249
  }
  public void init(final int _param__myBeam, final int _param_numSamples, final int _param_targetBeam, final int _param_targetSample, final float _param_cfarThreshold) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:223
    _myBeam = _param__myBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:223
    numSamples = _param_numSamples; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:223
    targetBeam = _param_targetBeam; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:223
    targetSample = _param_targetSample; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:223
    cfarThreshold = _param_cfarThreshold; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:223
    setIOTypes(Float.TYPE, Void.TYPE); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:217
    addSteadyPhase(1, 1, 0, "work"); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:229
    curSample = 0; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:224
    holdsTarget = (_myBeam == targetBeam); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:225
    myBeam = (_myBeam + 1); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:226
    thresh = 0.1f; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:227
  }
}
class AnonFilter_a0 extends Pipeline // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:23
{
  public AnonFilter_a0(float cfarThreshold, int coarseDecimationRatio, int fineDecimationRatio, int i, int numCoarseFilterTaps, int numFineFilterTaps, int numPostDec1, int numPostDec2, int numSamples, int targetBeam, int targetSample)
  {
  }
  public void init(final float cfarThreshold, final int coarseDecimationRatio, final int fineDecimationRatio, final int i, final int numCoarseFilterTaps, final int numFineFilterTaps, final int numPostDec1, final int numPostDec2, final int numSamples, final int targetBeam, final int targetSample) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:23
    add(new InputGenerate(i, numSamples, targetBeam, targetSample, cfarThreshold)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:24
    add(new Decimator(coarseDecimationRatio)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:25
    add(new CoarseBeamFirFilter(numCoarseFilterTaps, numPostDec1, coarseDecimationRatio)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:26
    add(new Decimator(fineDecimationRatio)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:27
    add(new CoarseBeamFirFilter(numFineFilterTaps, numPostDec2, fineDecimationRatio)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:28
  }
}
class AnonFilter_a1 extends SplitJoin // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:20
{
  public AnonFilter_a1(float cfarThreshold, int coarseDecimationRatio, int fineDecimationRatio, int numChannels, int numCoarseFilterTaps, int numFineFilterTaps, int numPostDec1, int numPostDec2, int numSamples, int targetBeam, int targetSample)
  {
  }
  public void init(final float cfarThreshold, final int coarseDecimationRatio, final int fineDecimationRatio, final int numChannels, final int numCoarseFilterTaps, final int numFineFilterTaps, final int numPostDec1, final int numPostDec2, final int numSamples, final int targetBeam, final int targetSample) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:20
    setSplitter(ROUND_ROBIN(0)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:21
    for (int i = 0; (i < numChannels); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:22
      add(new AnonFilter_a0(cfarThreshold, coarseDecimationRatio, fineDecimationRatio, i, numCoarseFilterTaps, numFineFilterTaps, numPostDec1, numPostDec2, numSamples, targetBeam, targetSample)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:23
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:22
    setJoiner(ROUND_ROBIN(2)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:31
  }
}
class AnonFilter_a2 extends Pipeline // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:36
{
  public AnonFilter_a2(int i, int mfSize, int numChannels, int numPostDec2)
  {
  }
  public void init(final int i, final int mfSize, final int numChannels, final int numPostDec2) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:36
    add(new BeamForm(i, numChannels)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:37
    add(new CoarseBeamFirFilter(mfSize, numPostDec2, 1)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:38
    add(new Magnitude()); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:39
    add(new FloatPrinter()); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:40
  }
}
class AnonFilter_a3 extends SplitJoin // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:33
{
  public AnonFilter_a3(int mfSize, int numBeams, int numChannels, int numPostDec2)
  {
  }
  public void init(final int mfSize, final int numBeams, final int numChannels, final int numPostDec2) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:33
    setSplitter(DUPLICATE()); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:34
    for (int i = 0; (i < numBeams); i++) { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:35
      add(new AnonFilter_a2(i, mfSize, numChannels, numPostDec2)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:36
    }; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:35
    setJoiner(ROUND_ROBIN(0)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:43
  }
}
public class CoarseBeamFormer extends StreamItPipeline // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:1
{
  public void init() { // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:1
    int numChannels = 12; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:2
    int numSamples = 1024; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:3
    int numBeams = 4; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:4
    int numCoarseFilterTaps = 64; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:5
    int numFineFilterTaps = 64; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:6
    int coarseDecimationRatio = 1; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:7
    int fineDecimationRatio = 2; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:8
    int numSegments = 1; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:9
    int numPostDec1 = (numSamples / coarseDecimationRatio); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:10
    int numPostDec2 = (numPostDec1 / fineDecimationRatio); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:11
    int mfSize = (numSegments * numPostDec2); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:12
    int pulseSize = (numPostDec2 / 2); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:13
    int predecPulseSize = ((pulseSize * coarseDecimationRatio) * fineDecimationRatio); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:14
    int targetBeam = (numBeams / 4); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:15
    int targetSample = (numSamples / 4); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:16
    int targetSamplePostDec = ((targetSample / coarseDecimationRatio) / fineDecimationRatio); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:17
    float dOverLambda = 0.5f; // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:18
    float cfarThreshold = (((0.95f * dOverLambda) * numChannels) * (0.5f * pulseSize)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:19
    add(new AnonFilter_a1(cfarThreshold, coarseDecimationRatio, fineDecimationRatio, numChannels, numCoarseFilterTaps, numFineFilterTaps, numPostDec1, numPostDec2, numSamples, targetBeam, targetSample)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:20
    add(new AnonFilter_a3(mfSize, numBeams, numChannels, numPostDec2)); // ../benchmarks//beamformer//streamit//CoarseBeamFormer.str:33
  }
}
