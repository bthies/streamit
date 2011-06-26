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
class InputGenerate extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:44
{
  public InputGenerate(int _param_myChannel, int _param_numberOfSamples, int _param_tarBeam, int _param_targetSample, float _param_thresh)
  {
  }
  int curSample; // ../benchmarks//beamformer//streamit//BeamFormer.str:45
  boolean holdsTarget; // ../benchmarks//beamformer//streamit//BeamFormer.str:46
  int myChannel; // ../benchmarks//beamformer//streamit//BeamFormer.str:44
  int numberOfSamples; // ../benchmarks//beamformer//streamit//BeamFormer.str:44
  int tarBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:44
  int targetSample; // ../benchmarks//beamformer//streamit//BeamFormer.str:44
  float thresh; // ../benchmarks//beamformer//streamit//BeamFormer.str:44
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:51
    if ((holdsTarget && (curSample == targetSample))) { // ../benchmarks//beamformer//streamit//BeamFormer.str:52
      outputChannel.pushFloat((float)Math.sqrt((curSample * myChannel))); // ../benchmarks//beamformer//streamit//BeamFormer.str:53
      outputChannel.pushFloat(((float)Math.sqrt((curSample * myChannel)) + 1)); // ../benchmarks//beamformer//streamit//BeamFormer.str:54
    } else { // ../benchmarks//beamformer//streamit//BeamFormer.str:55
      outputChannel.pushFloat(-(float)Math.sqrt((curSample * myChannel))); // ../benchmarks//beamformer//streamit//BeamFormer.str:56
      outputChannel.pushFloat(-((float)Math.sqrt((curSample * myChannel)) + 1)); // ../benchmarks//beamformer//streamit//BeamFormer.str:57
    } // ../benchmarks//beamformer//streamit//BeamFormer.str:52
    curSample++; // ../benchmarks//beamformer//streamit//BeamFormer.str:59
    if ((curSample >= numberOfSamples)) { // ../benchmarks//beamformer//streamit//BeamFormer.str:60
      curSample = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:61
    } // ../benchmarks//beamformer//streamit//BeamFormer.str:60
  }
  public void init(final int _param_myChannel, final int _param_numberOfSamples, final int _param_tarBeam, final int _param_targetSample, final float _param_thresh) { // ../benchmarks//beamformer//streamit//BeamFormer.str:47
    myChannel = _param_myChannel; // ../benchmarks//beamformer//streamit//BeamFormer.str:47
    numberOfSamples = _param_numberOfSamples; // ../benchmarks//beamformer//streamit//BeamFormer.str:47
    tarBeam = _param_tarBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:47
    targetSample = _param_targetSample; // ../benchmarks//beamformer//streamit//BeamFormer.str:47
    thresh = _param_thresh; // ../benchmarks//beamformer//streamit//BeamFormer.str:47
    setIOTypes(Void.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:44
    addSteadyPhase(0, 0, 2, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:51
    curSample = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:48
    holdsTarget = (tarBeam == myChannel); // ../benchmarks//beamformer//streamit//BeamFormer.str:49
  }
}
class FloatPrinter extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:65
{
  public FloatPrinter()
  {
  }
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:66
    System.out.println(inputChannel.popFloat()); // ../benchmarks//beamformer//streamit//BeamFormer.str:66
  }
  public void init() { // ../benchmarks//beamformer//streamit//BeamFormer.str:65
    setIOTypes(Float.TYPE, Void.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:65
    addSteadyPhase(1, 1, 0, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:66
  }
}
class BeamFirFilter extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:68
{
  public BeamFirFilter(int _param_numTaps, int _param_inputLength, int _param_decimationRatio)
  {
  }
  float[numTaps] real_weight; // ../benchmarks//beamformer//streamit//BeamFormer.str:69
  float[numTaps] imag_weight; // ../benchmarks//beamformer//streamit//BeamFormer.str:70
  int numTapsMinusOne; // ../benchmarks//beamformer//streamit//BeamFormer.str:71
  float[numTaps] realBuffer; // ../benchmarks//beamformer//streamit//BeamFormer.str:72
  float[numTaps] imagBuffer; // ../benchmarks//beamformer//streamit//BeamFormer.str:73
  int count; // ../benchmarks//beamformer//streamit//BeamFormer.str:74
  int pos; // ../benchmarks//beamformer//streamit//BeamFormer.str:75
  int numTaps; // ../benchmarks//beamformer//streamit//BeamFormer.str:68
  int inputLength; // ../benchmarks//beamformer//streamit//BeamFormer.str:68
  int decimationRatio; // ../benchmarks//beamformer//streamit//BeamFormer.str:68
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:86
    float real_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:87
    float imag_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:88
    int i; // ../benchmarks//beamformer//streamit//BeamFormer.str:89
    int modPos; // ../benchmarks//beamformer//streamit//BeamFormer.str:90
    realBuffer[(numTapsMinusOne - pos)] = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:91
    imagBuffer[(numTapsMinusOne - pos)] = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:92
    modPos = (numTapsMinusOne - pos); // ../benchmarks//beamformer//streamit//BeamFormer.str:93
    for (i = 0; (i < numTaps); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:94
      real_curr += ((realBuffer[modPos] * real_weight[i]) + (imagBuffer[modPos] * imag_weight[i])); // ../benchmarks//beamformer//streamit//BeamFormer.str:95
      imag_curr += ((imagBuffer[modPos] * real_weight[i]) + (realBuffer[modPos] * imag_weight[i])); // ../benchmarks//beamformer//streamit//BeamFormer.str:97
      modPos = ((modPos + 1) & numTapsMinusOne); // ../benchmarks//beamformer//streamit//BeamFormer.str:99
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:94
    pos = ((pos + 1) & numTapsMinusOne); // ../benchmarks//beamformer//streamit//BeamFormer.str:101
    outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:102
    outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:103
    for (i = 2; (i < (2 * decimationRatio)); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:104
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:105
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:104
    count += decimationRatio; // ../benchmarks//beamformer//streamit//BeamFormer.str:107
    if ((count == inputLength)) { // ../benchmarks//beamformer//streamit//BeamFormer.str:108
      count = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:109
      pos = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:110
      for (i = 0; (i < numTaps); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:111
        realBuffer[i] = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:112
        imagBuffer[i] = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:113
      }; // ../benchmarks//beamformer//streamit//BeamFormer.str:111
    } // ../benchmarks//beamformer//streamit//BeamFormer.str:108
  }
  public void init(final int _param_numTaps, final int _param_inputLength, final int _param_decimationRatio) { // ../benchmarks//beamformer//streamit//BeamFormer.str:76
    numTaps = _param_numTaps; // ../benchmarks//beamformer//streamit//BeamFormer.str:76
    inputLength = _param_inputLength; // ../benchmarks//beamformer//streamit//BeamFormer.str:76
    decimationRatio = _param_decimationRatio; // ../benchmarks//beamformer//streamit//BeamFormer.str:76
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:68
    addSteadyPhase((2 * decimationRatio), (2 * decimationRatio), 2, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:86
    int i; // ../benchmarks//beamformer//streamit//BeamFormer.str:77
    numTapsMinusOne = (numTaps - 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:78
    pos = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:79
    for (int j = 0; (j < numTaps); j++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:80
      int idx; // ../benchmarks//beamformer//streamit//BeamFormer.str:81
      idx = (j + 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:81
      real_weight[j] = ((float)Math.sin(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//BeamFormer.str:82
      imag_weight[j] = ((float)Math.cos(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//BeamFormer.str:83
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:80
  }
}
class Decimator extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:118
{
  public Decimator(int _param_decimationFactor)
  {
  }
  int decimationFactor; // ../benchmarks//beamformer//streamit//BeamFormer.str:118
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:120
    outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//beamformer//streamit//BeamFormer.str:121
    outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//beamformer//streamit//BeamFormer.str:122
    for (int i = 1; (i < decimationFactor); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:123
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:124
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:125
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:123
  }
  public void init(final int _param_decimationFactor) { // ../benchmarks//beamformer//streamit//BeamFormer.str:119
    decimationFactor = _param_decimationFactor; // ../benchmarks//beamformer//streamit//BeamFormer.str:119
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:118
    addSteadyPhase((2 * decimationFactor), (2 * decimationFactor), 2, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:120
  }
}
class CoarseBeamFirFilter extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:129
{
  public CoarseBeamFirFilter(int _param_numTaps, int _param_inputLength, int _param_decimationRatio)
  {
  }
  float[numTaps] real_weight; // ../benchmarks//beamformer//streamit//BeamFormer.str:130
  float[numTaps] imag_weight; // ../benchmarks//beamformer//streamit//BeamFormer.str:131
  int numTaps; // ../benchmarks//beamformer//streamit//BeamFormer.str:129
  int inputLength; // ../benchmarks//beamformer//streamit//BeamFormer.str:129
  int decimationRatio; // ../benchmarks//beamformer//streamit//BeamFormer.str:129
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:140
    int min; // ../benchmarks//beamformer//streamit//BeamFormer.str:141
    if ((numTaps < inputLength)) { // ../benchmarks//beamformer//streamit//BeamFormer.str:142
      min = numTaps; // ../benchmarks//beamformer//streamit//BeamFormer.str:143
    } else { // ../benchmarks//beamformer//streamit//BeamFormer.str:144
      min = inputLength; // ../benchmarks//beamformer//streamit//BeamFormer.str:145
    } // ../benchmarks//beamformer//streamit//BeamFormer.str:142
    for (int i = 1; (i <= min); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:147
      float real_curr; // ../benchmarks//beamformer//streamit//BeamFormer.str:148
      real_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:148
      float imag_curr; // ../benchmarks//beamformer//streamit//BeamFormer.str:149
      imag_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:149
      for (int j = 0; (j < i); j++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:150
        int realIndex; // ../benchmarks//beamformer//streamit//BeamFormer.str:151
        realIndex = (2 * ((i - j) - 1)); // ../benchmarks//beamformer//streamit//BeamFormer.str:151
        int imagIndex; // ../benchmarks//beamformer//streamit//BeamFormer.str:152
        imagIndex = (realIndex + 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:152
        real_curr += ((real_weight[j] * inputChannel.peekFloat(realIndex)) + (imag_weight[j] * inputChannel.peekFloat(imagIndex))); // ../benchmarks//beamformer//streamit//BeamFormer.str:153
        imag_curr += ((real_weight[j] * inputChannel.peekFloat(imagIndex)) + (imag_weight[j] * inputChannel.peekFloat(realIndex))); // ../benchmarks//beamformer//streamit//BeamFormer.str:154
      }; // ../benchmarks//beamformer//streamit//BeamFormer.str:150
      outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:156
      outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:157
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:147
    for (int i = 0; (i < (inputLength - numTaps)); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:159
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:160
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:161
      float real_curr; // ../benchmarks//beamformer//streamit//BeamFormer.str:162
      real_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:162
      float imag_curr; // ../benchmarks//beamformer//streamit//BeamFormer.str:163
      imag_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:163
      for (int j = 0; (j < numTaps); j++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:164
        int realIndex; // ../benchmarks//beamformer//streamit//BeamFormer.str:165
        realIndex = (2 * ((numTaps - j) - 1)); // ../benchmarks//beamformer//streamit//BeamFormer.str:165
        int imagIndex; // ../benchmarks//beamformer//streamit//BeamFormer.str:166
        imagIndex = (realIndex + 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:166
        real_curr += ((real_weight[j] * inputChannel.peekFloat(realIndex)) + (imag_weight[j] * inputChannel.peekFloat(imagIndex))); // ../benchmarks//beamformer//streamit//BeamFormer.str:167
        imag_curr += ((real_weight[j] * inputChannel.peekFloat(imagIndex)) + (imag_weight[j] * inputChannel.peekFloat(realIndex))); // ../benchmarks//beamformer//streamit//BeamFormer.str:168
      }; // ../benchmarks//beamformer//streamit//BeamFormer.str:164
      outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:170
      outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:171
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:159
    for (int i = 0; (i < min); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:173
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:174
      inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:175
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:173
  }
  public void init(final int _param_numTaps, final int _param_inputLength, final int _param_decimationRatio) { // ../benchmarks//beamformer//streamit//BeamFormer.str:132
    numTaps = _param_numTaps; // ../benchmarks//beamformer//streamit//BeamFormer.str:132
    inputLength = _param_inputLength; // ../benchmarks//beamformer//streamit//BeamFormer.str:132
    decimationRatio = _param_decimationRatio; // ../benchmarks//beamformer//streamit//BeamFormer.str:132
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:129
    addSteadyPhase((2 * inputLength), (2 * inputLength), (2 * inputLength), "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:140
    int i; // ../benchmarks//beamformer//streamit//BeamFormer.str:133
    for (int j = 0; (j < numTaps); j++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:134
      int idx; // ../benchmarks//beamformer//streamit//BeamFormer.str:135
      idx = (j + 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:135
      real_weight[j] = ((float)Math.sin(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//BeamFormer.str:136
      imag_weight[j] = ((float)Math.cos(idx) / ((float)(idx))); // ../benchmarks//beamformer//streamit//BeamFormer.str:137
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:134
  }
}
class BeamForm extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:179
{
  public BeamForm(int _param_myBeamId, int _param_numChannels)
  {
  }
  float[numChannels] real_weight; // ../benchmarks//beamformer//streamit//BeamFormer.str:180
  float[numChannels] imag_weight; // ../benchmarks//beamformer//streamit//BeamFormer.str:181
  int myBeamId; // ../benchmarks//beamformer//streamit//BeamFormer.str:179
  int numChannels; // ../benchmarks//beamformer//streamit//BeamFormer.str:179
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:189
    float real_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:190
    float imag_curr = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:191
    for (int i = 0; (i < numChannels); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:192
      float real_pop; // ../benchmarks//beamformer//streamit//BeamFormer.str:193
      real_pop = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:193
      float imag_pop; // ../benchmarks//beamformer//streamit//BeamFormer.str:194
      imag_pop = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:194
      real_curr += ((real_weight[i] * real_pop) - (imag_weight[i] * imag_pop)); // ../benchmarks//beamformer//streamit//BeamFormer.str:195
      imag_curr += ((real_weight[i] * imag_pop) + (imag_weight[i] * real_pop)); // ../benchmarks//beamformer//streamit//BeamFormer.str:197
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:192
    outputChannel.pushFloat(real_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:200
    outputChannel.pushFloat(imag_curr); // ../benchmarks//beamformer//streamit//BeamFormer.str:201
  }
  public void init(final int _param_myBeamId, final int _param_numChannels) { // ../benchmarks//beamformer//streamit//BeamFormer.str:182
    myBeamId = _param_myBeamId; // ../benchmarks//beamformer//streamit//BeamFormer.str:182
    numChannels = _param_numChannels; // ../benchmarks//beamformer//streamit//BeamFormer.str:182
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:179
    addSteadyPhase((2 * numChannels), (2 * numChannels), 2, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:189
    for (int j = 0; (j < numChannels); j++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:183
      int idx; // ../benchmarks//beamformer//streamit//BeamFormer.str:184
      idx = (j + 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:184
      real_weight[j] = ((float)Math.sin(idx) / ((float)((myBeamId + idx)))); // ../benchmarks//beamformer//streamit//BeamFormer.str:185
      imag_weight[j] = ((float)Math.cos(idx) / ((float)((myBeamId + idx)))); // ../benchmarks//beamformer//streamit//BeamFormer.str:186
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:183
  }
}
class Magnitude extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:204
{
  public Magnitude()
  {
  }
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:206
    float f1 = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:207
    float f2 = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:208
    outputChannel.pushFloat(mag(f1, f2)); // ../benchmarks//beamformer//streamit//BeamFormer.str:209
  }
  public float mag(float real, float imag) { // ../benchmarks//beamformer//streamit//BeamFormer.str:211
    return ((float)((float)Math.sqrt(((real * real) + (imag * imag))))); // ../benchmarks//beamformer//streamit//BeamFormer.str:212
  }
  public void init() { // ../benchmarks//beamformer//streamit//BeamFormer.str:205
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:204
    addSteadyPhase(2, 2, 1, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:206
  }
}
class Detector extends Filter // ../benchmarks//beamformer//streamit//BeamFormer.str:215
{
  public Detector(int _param__myBeam, int _param_numSamples, int _param_targetBeam, int _param_targetSample, float _param_cfarThreshold)
  {
  }
  int curSample; // ../benchmarks//beamformer//streamit//BeamFormer.str:217
  int myBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:218
  boolean holdsTarget; // ../benchmarks//beamformer//streamit//BeamFormer.str:219
  float thresh; // ../benchmarks//beamformer//streamit//BeamFormer.str:220
  int _myBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:215
  int numSamples; // ../benchmarks//beamformer//streamit//BeamFormer.str:215
  int targetBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:215
  int targetSample; // ../benchmarks//beamformer//streamit//BeamFormer.str:215
  float cfarThreshold; // ../benchmarks//beamformer//streamit//BeamFormer.str:215
  public void work() { // ../benchmarks//beamformer//streamit//BeamFormer.str:228
    float inputVal = inputChannel.popFloat(); // ../benchmarks//beamformer//streamit//BeamFormer.str:229
    float outputVal; // ../benchmarks//beamformer//streamit//BeamFormer.str:230
    if ((holdsTarget && (targetSample == curSample))) { // ../benchmarks//beamformer//streamit//BeamFormer.str:231
      if (!(inputVal >= thresh)) { // ../benchmarks//beamformer//streamit//BeamFormer.str:232
        outputVal = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:233
      } else { // ../benchmarks//beamformer//streamit//BeamFormer.str:234
        outputVal = myBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:235
      } // ../benchmarks//beamformer//streamit//BeamFormer.str:232
    } else { // ../benchmarks//beamformer//streamit//BeamFormer.str:237
      if (!(inputVal >= thresh)) { // ../benchmarks//beamformer//streamit//BeamFormer.str:238
        outputVal = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:239
      } else { // ../benchmarks//beamformer//streamit//BeamFormer.str:240
        outputVal = -myBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:241
      } // ../benchmarks//beamformer//streamit//BeamFormer.str:238
    } // ../benchmarks//beamformer//streamit//BeamFormer.str:231
    outputVal = inputVal; // ../benchmarks//beamformer//streamit//BeamFormer.str:244
    System.out.println(outputVal); // ../benchmarks//beamformer//streamit//BeamFormer.str:245
    curSample++; // ../benchmarks//beamformer//streamit//BeamFormer.str:246
    if ((curSample >= numSamples)) { // ../benchmarks//beamformer//streamit//BeamFormer.str:248
      curSample = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:248
    } // ../benchmarks//beamformer//streamit//BeamFormer.str:247
  }
  public void init(final int _param__myBeam, final int _param_numSamples, final int _param_targetBeam, final int _param_targetSample, final float _param_cfarThreshold) { // ../benchmarks//beamformer//streamit//BeamFormer.str:221
    _myBeam = _param__myBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:221
    numSamples = _param_numSamples; // ../benchmarks//beamformer//streamit//BeamFormer.str:221
    targetBeam = _param_targetBeam; // ../benchmarks//beamformer//streamit//BeamFormer.str:221
    targetSample = _param_targetSample; // ../benchmarks//beamformer//streamit//BeamFormer.str:221
    cfarThreshold = _param_cfarThreshold; // ../benchmarks//beamformer//streamit//BeamFormer.str:221
    setIOTypes(Float.TYPE, Void.TYPE); // ../benchmarks//beamformer//streamit//BeamFormer.str:215
    addSteadyPhase(1, 1, 0, "work"); // ../benchmarks//beamformer//streamit//BeamFormer.str:227
    curSample = 0; // ../benchmarks//beamformer//streamit//BeamFormer.str:222
    holdsTarget = (_myBeam == targetBeam); // ../benchmarks//beamformer//streamit//BeamFormer.str:223
    myBeam = (_myBeam + 1); // ../benchmarks//beamformer//streamit//BeamFormer.str:224
    thresh = 0.1f; // ../benchmarks//beamformer//streamit//BeamFormer.str:225
  }
}
class AnonFilter_a0 extends Pipeline // ../benchmarks//beamformer//streamit//BeamFormer.str:23
{
  public AnonFilter_a0(float cfarThreshold, int coarseDecimationRatio, int fineDecimationRatio, int i, int numCoarseFilterTaps, int numFineFilterTaps, int numPostDec1, int numSamples, int targetBeam, int targetSample)
  {
  }
  public void init(final float cfarThreshold, final int coarseDecimationRatio, final int fineDecimationRatio, final int i, final int numCoarseFilterTaps, final int numFineFilterTaps, final int numPostDec1, final int numSamples, final int targetBeam, final int targetSample) { // ../benchmarks//beamformer//streamit//BeamFormer.str:23
    add(new InputGenerate(i, numSamples, targetBeam, targetSample, cfarThreshold)); // ../benchmarks//beamformer//streamit//BeamFormer.str:24
    add(new BeamFirFilter(numCoarseFilterTaps, numSamples, coarseDecimationRatio)); // ../benchmarks//beamformer//streamit//BeamFormer.str:25
    add(new BeamFirFilter(numFineFilterTaps, numPostDec1, fineDecimationRatio)); // ../benchmarks//beamformer//streamit//BeamFormer.str:26
  }
}
class AnonFilter_a1 extends SplitJoin // ../benchmarks//beamformer//streamit//BeamFormer.str:20
{
  public AnonFilter_a1(float cfarThreshold, int coarseDecimationRatio, int fineDecimationRatio, int numChannels, int numCoarseFilterTaps, int numFineFilterTaps, int numPostDec1, int numSamples, int targetBeam, int targetSample)
  {
  }
  public void init(final float cfarThreshold, final int coarseDecimationRatio, final int fineDecimationRatio, final int numChannels, final int numCoarseFilterTaps, final int numFineFilterTaps, final int numPostDec1, final int numSamples, final int targetBeam, final int targetSample) { // ../benchmarks//beamformer//streamit//BeamFormer.str:20
    setSplitter(ROUND_ROBIN(0)); // ../benchmarks//beamformer//streamit//BeamFormer.str:21
    for (int i = 0; (i < numChannels); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:22
      add(new AnonFilter_a0(cfarThreshold, coarseDecimationRatio, fineDecimationRatio, i, numCoarseFilterTaps, numFineFilterTaps, numPostDec1, numSamples, targetBeam, targetSample)); // ../benchmarks//beamformer//streamit//BeamFormer.str:23
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:22
    setJoiner(ROUND_ROBIN(2)); // ../benchmarks//beamformer//streamit//BeamFormer.str:29
  }
}
class AnonFilter_a2 extends Pipeline // ../benchmarks//beamformer//streamit//BeamFormer.str:34
{
  public AnonFilter_a2(int i, int mfSize, int numChannels, int numPostDec2)
  {
  }
  public void init(final int i, final int mfSize, final int numChannels, final int numPostDec2) { // ../benchmarks//beamformer//streamit//BeamFormer.str:34
    add(new BeamForm(i, numChannels)); // ../benchmarks//beamformer//streamit//BeamFormer.str:35
    add(new BeamFirFilter(mfSize, numPostDec2, 1)); // ../benchmarks//beamformer//streamit//BeamFormer.str:36
    add(new Magnitude()); // ../benchmarks//beamformer//streamit//BeamFormer.str:37
    add(new FloatPrinter()); // ../benchmarks//beamformer//streamit//BeamFormer.str:38
  }
}
class AnonFilter_a3 extends SplitJoin // ../benchmarks//beamformer//streamit//BeamFormer.str:31
{
  public AnonFilter_a3(int mfSize, int numBeams, int numChannels, int numPostDec2)
  {
  }
  public void init(final int mfSize, final int numBeams, final int numChannels, final int numPostDec2) { // ../benchmarks//beamformer//streamit//BeamFormer.str:31
    setSplitter(DUPLICATE()); // ../benchmarks//beamformer//streamit//BeamFormer.str:32
    for (int i = 0; (i < numBeams); i++) { // ../benchmarks//beamformer//streamit//BeamFormer.str:33
      add(new AnonFilter_a2(i, mfSize, numChannels, numPostDec2)); // ../benchmarks//beamformer//streamit//BeamFormer.str:34
    }; // ../benchmarks//beamformer//streamit//BeamFormer.str:33
    setJoiner(ROUND_ROBIN(0)); // ../benchmarks//beamformer//streamit//BeamFormer.str:41
  }
}
public class BeamFormer extends StreamItPipeline // ../benchmarks//beamformer//streamit//BeamFormer.str:1
{
  public void init() { // ../benchmarks//beamformer//streamit//BeamFormer.str:1
    int numChannels = 12; // ../benchmarks//beamformer//streamit//BeamFormer.str:2
    int numSamples = 1024; // ../benchmarks//beamformer//streamit//BeamFormer.str:3
    int numBeams = 4; // ../benchmarks//beamformer//streamit//BeamFormer.str:4
    int numCoarseFilterTaps = 64; // ../benchmarks//beamformer//streamit//BeamFormer.str:5
    int numFineFilterTaps = 64; // ../benchmarks//beamformer//streamit//BeamFormer.str:6
    int coarseDecimationRatio = 1; // ../benchmarks//beamformer//streamit//BeamFormer.str:7
    int fineDecimationRatio = 2; // ../benchmarks//beamformer//streamit//BeamFormer.str:8
    int numSegments = 1; // ../benchmarks//beamformer//streamit//BeamFormer.str:9
    int numPostDec1 = (numSamples / coarseDecimationRatio); // ../benchmarks//beamformer//streamit//BeamFormer.str:10
    int numPostDec2 = (numPostDec1 / fineDecimationRatio); // ../benchmarks//beamformer//streamit//BeamFormer.str:11
    int mfSize = (numSegments * numPostDec2); // ../benchmarks//beamformer//streamit//BeamFormer.str:12
    int pulseSize = (numPostDec2 / 2); // ../benchmarks//beamformer//streamit//BeamFormer.str:13
    int predecPulseSize = ((pulseSize * coarseDecimationRatio) * fineDecimationRatio); // ../benchmarks//beamformer//streamit//BeamFormer.str:14
    int targetBeam = (numBeams / 4); // ../benchmarks//beamformer//streamit//BeamFormer.str:15
    int targetSample = (numSamples / 4); // ../benchmarks//beamformer//streamit//BeamFormer.str:16
    int targetSamplePostDec = ((targetSample / coarseDecimationRatio) / fineDecimationRatio); // ../benchmarks//beamformer//streamit//BeamFormer.str:17
    float dOverLambda = 0.5f; // ../benchmarks//beamformer//streamit//BeamFormer.str:18
    float cfarThreshold = (((0.95f * dOverLambda) * numChannels) * (0.5f * pulseSize)); // ../benchmarks//beamformer//streamit//BeamFormer.str:19
    add(new AnonFilter_a1(cfarThreshold, coarseDecimationRatio, fineDecimationRatio, numChannels, numCoarseFilterTaps, numFineFilterTaps, numPostDec1, numSamples, targetBeam, targetSample)); // ../benchmarks//beamformer//streamit//BeamFormer.str:20
    add(new AnonFilter_a3(mfSize, numBeams, numChannels, numPostDec2)); // ../benchmarks//beamformer//streamit//BeamFormer.str:31
  }
}
