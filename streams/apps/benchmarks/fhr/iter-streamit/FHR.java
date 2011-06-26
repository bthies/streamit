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
class CombineDFT extends Filter // ../benchmarks//fhr//streamit//FHR.str:1
{
  public CombineDFT(int _param_n)
  {
  }
  float[n] w; // ../benchmarks//fhr//streamit//FHR.str:3
  int n; // ../benchmarks//fhr//streamit//FHR.str:1
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:22
    int i; // ../benchmarks//fhr//streamit//FHR.str:23
    float[(2 * n)] results; // ../benchmarks//fhr//streamit//FHR.str:24
    for (i = 0; (i < n); i += 2) { // ../benchmarks//fhr//streamit//FHR.str:26
      int i_plus_1; // ../benchmarks//fhr//streamit//FHR.str:27
      i_plus_1 = (i + 1); // ../benchmarks//fhr//streamit//FHR.str:27
      float y0_r; // ../benchmarks//fhr//streamit//FHR.str:29
      y0_r = inputChannel.peekFloat(i); // ../benchmarks//fhr//streamit//FHR.str:29
      float y0_i; // ../benchmarks//fhr//streamit//FHR.str:30
      y0_i = inputChannel.peekFloat(i_plus_1); // ../benchmarks//fhr//streamit//FHR.str:30
      float y1_r; // ../benchmarks//fhr//streamit//FHR.str:32
      y1_r = inputChannel.peekFloat((n + i)); // ../benchmarks//fhr//streamit//FHR.str:32
      float y1_i; // ../benchmarks//fhr//streamit//FHR.str:33
      y1_i = inputChannel.peekFloat((n + i_plus_1)); // ../benchmarks//fhr//streamit//FHR.str:33
      float weight_real; // ../benchmarks//fhr//streamit//FHR.str:35
      weight_real = w[i]; // ../benchmarks//fhr//streamit//FHR.str:35
      float weight_imag; // ../benchmarks//fhr//streamit//FHR.str:36
      weight_imag = w[i_plus_1]; // ../benchmarks//fhr//streamit//FHR.str:36
      float y1w_r; // ../benchmarks//fhr//streamit//FHR.str:38
      y1w_r = ((y1_r * weight_real) - (y1_i * weight_imag)); // ../benchmarks//fhr//streamit//FHR.str:38
      float y1w_i; // ../benchmarks//fhr//streamit//FHR.str:39
      y1w_i = ((y1_r * weight_imag) + (y1_i * weight_real)); // ../benchmarks//fhr//streamit//FHR.str:39
      results[i] = (y0_r + y1w_r); // ../benchmarks//fhr//streamit//FHR.str:41
      results[i_plus_1] = (y0_i + y1w_i); // ../benchmarks//fhr//streamit//FHR.str:42
      results[(n + i)] = (y0_r - y1w_r); // ../benchmarks//fhr//streamit//FHR.str:44
      results[(n + i_plus_1)] = (y0_i - y1w_i); // ../benchmarks//fhr//streamit//FHR.str:45
    }; // ../benchmarks//fhr//streamit//FHR.str:26
    for (i = 0; (i < (2 * n)); i++) { // ../benchmarks//fhr//streamit//FHR.str:48
      inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:49
      outputChannel.pushFloat(results[i]); // ../benchmarks//fhr//streamit//FHR.str:50
    }; // ../benchmarks//fhr//streamit//FHR.str:48
  }
  public void init(final int _param_n) { // ../benchmarks//fhr//streamit//FHR.str:5
    n = _param_n; // ../benchmarks//fhr//streamit//FHR.str:5
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:1
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // ../benchmarks//fhr//streamit//FHR.str:22
    float wn_r = ((float)((float)Math.cos(((2 * 3.141592654f) / n)))); // ../benchmarks//fhr//streamit//FHR.str:6
    float wn_i = ((float)((float)Math.sin(((-2 * 3.141592654f) / n)))); // ../benchmarks//fhr//streamit//FHR.str:7
    float real = 1; // ../benchmarks//fhr//streamit//FHR.str:8
    float imag = 0; // ../benchmarks//fhr//streamit//FHR.str:9
    float next_real, next_imag; // ../benchmarks//fhr//streamit//FHR.str:10
    for (int i = 0; (i < n); i += 2) { // ../benchmarks//fhr//streamit//FHR.str:12
      w[i] = real; // ../benchmarks//fhr//streamit//FHR.str:13
      w[(i + 1)] = imag; // ../benchmarks//fhr//streamit//FHR.str:14
      next_real = ((real * wn_r) - (imag * wn_i)); // ../benchmarks//fhr//streamit//FHR.str:15
      next_imag = ((real * wn_i) + (imag * wn_r)); // ../benchmarks//fhr//streamit//FHR.str:16
      real = next_real; // ../benchmarks//fhr//streamit//FHR.str:17
      imag = next_imag; // ../benchmarks//fhr//streamit//FHR.str:18
    }; // ../benchmarks//fhr//streamit//FHR.str:12
  }
}
class FFTReorderSimple extends Filter // ../benchmarks//fhr//streamit//FHR.str:56
{
  public FFTReorderSimple(int _param_n)
  {
  }
  int totalData; // ../benchmarks//fhr//streamit//FHR.str:58
  int n; // ../benchmarks//fhr//streamit//FHR.str:56
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:64
    int i; // ../benchmarks//fhr//streamit//FHR.str:65
    for (i = 0; (i < totalData); i += 4) { // ../benchmarks//fhr//streamit//FHR.str:67
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // ../benchmarks//fhr//streamit//FHR.str:68
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // ../benchmarks//fhr//streamit//FHR.str:69
    }; // ../benchmarks//fhr//streamit//FHR.str:67
    for (i = 2; (i < totalData); i += 4) { // ../benchmarks//fhr//streamit//FHR.str:72
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // ../benchmarks//fhr//streamit//FHR.str:73
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // ../benchmarks//fhr//streamit//FHR.str:74
    }; // ../benchmarks//fhr//streamit//FHR.str:72
    for (i = 0; (i < n); i++) { // ../benchmarks//fhr//streamit//FHR.str:77
      inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:78
      inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:79
    }; // ../benchmarks//fhr//streamit//FHR.str:77
  }
  public void init(final int _param_n) { // ../benchmarks//fhr//streamit//FHR.str:60
    n = _param_n; // ../benchmarks//fhr//streamit//FHR.str:60
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:56
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // ../benchmarks//fhr//streamit//FHR.str:64
    totalData = (2 * n); // ../benchmarks//fhr//streamit//FHR.str:61
  }
}
class FFTReorder extends Pipeline // ../benchmarks//fhr//streamit//FHR.str:85
{
  public FFTReorder(int n)
  {
  }
  public void init(final int n) { // ../benchmarks//fhr//streamit//FHR.str:86
    for (int i = 1; (i < (n / 2)); i *= 2) { // ../benchmarks//fhr//streamit//FHR.str:87
      add(new FFTReorderSimple((n / i))); // ../benchmarks//fhr//streamit//FHR.str:88
    }; // ../benchmarks//fhr//streamit//FHR.str:87
  }
}
class FFT_Kernel extends Pipeline // ../benchmarks//fhr//streamit//FHR.str:93
{
  public FFT_Kernel(int n)
  {
  }
  public void init(final int n) { // ../benchmarks//fhr//streamit//FHR.str:94
    add(new FFTReorder(n)); // ../benchmarks//fhr//streamit//FHR.str:95
    for (int j = 2; (j <= n); j *= 2) { // ../benchmarks//fhr//streamit//FHR.str:96
      add(new CombineDFT(j)); // ../benchmarks//fhr//streamit//FHR.str:97
    }; // ../benchmarks//fhr//streamit//FHR.str:96
  }
}
class Read_From_AtoD extends Filter // ../benchmarks//fhr//streamit//FHR.str:101
{
  public Read_From_AtoD(int _param_N)
  {
  }
  int N; // ../benchmarks//fhr//streamit//FHR.str:101
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:103
    for (int i = 1; (i <= N); i++) { // ../benchmarks//fhr//streamit//FHR.str:104
      float val; // ../benchmarks//fhr//streamit//FHR.str:105
      val = ((float)((float)Math.sin(((i * 3.141592653589793f) / N)))); // ../benchmarks//fhr//streamit//FHR.str:105
      outputChannel.pushFloat((val * val)); // ../benchmarks//fhr//streamit//FHR.str:106
    }; // ../benchmarks//fhr//streamit//FHR.str:104
  }
  public void init(final int _param_N) { // ../benchmarks//fhr//streamit//FHR.str:101
    N = _param_N; // ../benchmarks//fhr//streamit//FHR.str:101
    setIOTypes(Void.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:101
    addSteadyPhase(0, 0, N, "work"); // ../benchmarks//fhr//streamit//FHR.str:103
  }
}
interface RFtoIFInterface {
  public void set_frequency_from_detector(int source);
}
class RFtoIFPortal extends Portal implements RFtoIFInterface {
  public void set_frequency_from_detector(int source) { }
}
class RFtoIF extends Filter implements RFtoIFInterface // ../benchmarks//fhr//streamit//FHR.str:112
{
  public RFtoIF(int _param_N, float _param__start_freq, float _param__current_freq, int _param_channels, float _param_channel_bandwidth)
  {
  }
  float[N] weights; // ../benchmarks//fhr//streamit//FHR.str:115
  int size, count; // ../benchmarks//fhr//streamit//FHR.str:116
  int iter; // ../benchmarks//fhr//streamit//FHR.str:117
  int[8] frequency_index; // ../benchmarks//fhr//streamit//FHR.str:121
  int N; // ../benchmarks//fhr//streamit//FHR.str:112
  float _start_freq; // ../benchmarks//fhr//streamit//FHR.str:112
  float _current_freq; // ../benchmarks//fhr//streamit//FHR.str:112
  int channels; // ../benchmarks//fhr//streamit//FHR.str:112
  float channel_bandwidth; // ../benchmarks//fhr//streamit//FHR.str:112
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:128
    outputChannel.pushFloat((inputChannel.popFloat() * weights[count++])); // ../benchmarks//fhr//streamit//FHR.str:129
    iter++; // ../benchmarks//fhr//streamit//FHR.str:131
    if ((count == size)) { // ../benchmarks//fhr//streamit//FHR.str:133
      count = 0; // ../benchmarks//fhr//streamit//FHR.str:134
    } // ../benchmarks//fhr//streamit//FHR.str:133
  }
  public void set_frequency_from_detector(int source) { // ../benchmarks//fhr//streamit//FHR.str:138
    float _freq = (_start_freq + (frequency_index[source] * channel_bandwidth)); // ../benchmarks//fhr//streamit//FHR.str:139
    frequency_index[source]++; // ../benchmarks//fhr//streamit//FHR.str:140
    if ((frequency_index[source] == channels)) { // ../benchmarks//fhr//streamit//FHR.str:141
      frequency_index[source] = 0; // ../benchmarks//fhr//streamit//FHR.str:142
    } // ../benchmarks//fhr//streamit//FHR.str:141
    set_frequency(_freq); // ../benchmarks//fhr//streamit//FHR.str:144
  }
  public void set_frequency(float _freq) { // ../benchmarks//fhr//streamit//FHR.str:147
    count = 0; // ../benchmarks//fhr//streamit//FHR.str:149
    size = ((int)(((_start_freq / _freq) * N))); // ../benchmarks//fhr//streamit//FHR.str:150
    for (int i = 0; (i < size); i++) { // ../benchmarks//fhr//streamit//FHR.str:151
      weights[i] = ((float)((float)Math.sin(((i * 3.141592653589793f) / size)))); // ../benchmarks//fhr//streamit//FHR.str:152
    }; // ../benchmarks//fhr//streamit//FHR.str:151
  }
  public void init(final int _param_N, final float _param__start_freq, final float _param__current_freq, final int _param_channels, final float _param_channel_bandwidth) { // ../benchmarks//fhr//streamit//FHR.str:123
    N = _param_N; // ../benchmarks//fhr//streamit//FHR.str:123
    _start_freq = _param__start_freq; // ../benchmarks//fhr//streamit//FHR.str:123
    _current_freq = _param__current_freq; // ../benchmarks//fhr//streamit//FHR.str:123
    channels = _param_channels; // ../benchmarks//fhr//streamit//FHR.str:123
    channel_bandwidth = _param_channel_bandwidth; // ../benchmarks//fhr//streamit//FHR.str:123
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:112
    addSteadyPhase(1, 1, 1, "work"); // ../benchmarks//fhr//streamit//FHR.str:128
    set_frequency(_current_freq); // ../benchmarks//fhr//streamit//FHR.str:124
    iter = 0; // ../benchmarks//fhr//streamit//FHR.str:125
  }
}
class Magnitude extends Filter // ../benchmarks//fhr//streamit//FHR.str:158
{
  public Magnitude()
  {
  }
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:160
    float f1 = inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:161
    float f2 = inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:162
    outputChannel.pushFloat(mag(f1, f2)); // ../benchmarks//fhr//streamit//FHR.str:163
  }
  public float mag(float real, float imag) { // ../benchmarks//fhr//streamit//FHR.str:166
    return ((float)((float)Math.sqrt(((real * real) + (imag * imag))))); // ../benchmarks//fhr//streamit//FHR.str:167
  }
  public void init() { // ../benchmarks//fhr//streamit//FHR.str:158
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:158
    addSteadyPhase(2, 2, 1, "work"); // ../benchmarks//fhr//streamit//FHR.str:160
  }
}
class Inject_Hop extends Filter // ../benchmarks//fhr//streamit//FHR.str:172
{
  public Inject_Hop(int _param_N, float _param_hop_threshold)
  {
  }
  int[4] hops; // ../benchmarks//fhr//streamit//FHR.str:174
  int hop_index; // ../benchmarks//fhr//streamit//FHR.str:175
  int N; // ../benchmarks//fhr//streamit//FHR.str:172
  float hop_threshold; // ../benchmarks//fhr//streamit//FHR.str:172
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:185
    for (int i = 1; (i <= hops[hop_index]); i++) { // ../benchmarks//fhr//streamit//FHR.str:186
      outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//fhr//streamit//FHR.str:187
    }; // ../benchmarks//fhr//streamit//FHR.str:186
    outputChannel.pushFloat((2 * hop_threshold)); // ../benchmarks//fhr//streamit//FHR.str:190
    inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:191
    for (int i = (hops[hop_index] + 2); (i <= N); i++) { // ../benchmarks//fhr//streamit//FHR.str:193
      outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//fhr//streamit//FHR.str:194
    }; // ../benchmarks//fhr//streamit//FHR.str:193
    hop_index = ((hop_index + 1) & 3); // ../benchmarks//fhr//streamit//FHR.str:197
  }
  public void init(final int _param_N, final float _param_hop_threshold) { // ../benchmarks//fhr//streamit//FHR.str:177
    N = _param_N; // ../benchmarks//fhr//streamit//FHR.str:177
    hop_threshold = _param_hop_threshold; // ../benchmarks//fhr//streamit//FHR.str:177
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:172
    addSteadyPhase(N, N, N, "work"); // ../benchmarks//fhr//streamit//FHR.str:185
    hops[0] = ((N / 4) - 2); // ../benchmarks//fhr//streamit//FHR.str:178
    hops[1] = ((N / 4) - 1); // ../benchmarks//fhr//streamit//FHR.str:179
    hops[2] = (N / 2); // ../benchmarks//fhr//streamit//FHR.str:180
    hops[3] = ((N / 2) + 1); // ../benchmarks//fhr//streamit//FHR.str:181
    hop_index = 0; // ../benchmarks//fhr//streamit//FHR.str:182
  }
}
class Check_Freq_Hop extends SplitJoin // ../benchmarks//fhr//streamit//FHR.str:202
{
  public Check_Freq_Hop(int N, float start_freq, int channels, float channel_bandwidth, float hop_threshold, RFtoIFPortal teleport)
  {
  }
  public void init(final int N, final float start_freq, final int channels, final float channel_bandwidth, final float hop_threshold, final RFtoIFPortal teleport) { // ../benchmarks//fhr//streamit//FHR.str:205
    setSplitter(WEIGHTED_ROUND_ROBIN(((N / 4) - 2), 1, 1, (N / 2), 1, 1, ((N / 4) - 2))); // ../benchmarks//fhr//streamit//FHR.str:210
    for (int i = 1; (i <= 7); i++) { // ../benchmarks//fhr//streamit//FHR.str:212
      if ((((i == 1) || (i == 4)) || (i == 7))) { // ../benchmarks//fhr//streamit//FHR.str:213
        add(new Identity(Float.TYPE)); // ../benchmarks//fhr//streamit//FHR.str:214
      } else { // ../benchmarks//fhr//streamit//FHR.str:215
        add(new Detect(start_freq, channels, channel_bandwidth, hop_threshold, teleport, i)); // ../benchmarks//fhr//streamit//FHR.str:216
      } // ../benchmarks//fhr//streamit//FHR.str:213
    }; // ../benchmarks//fhr//streamit//FHR.str:212
    setJoiner(WEIGHTED_ROUND_ROBIN(((N / 4) - 2), 1, 1, (N / 2), 1, 1, ((N / 4) - 2))); // ../benchmarks//fhr//streamit//FHR.str:220
  }
}
class Detect extends Filter // ../benchmarks//fhr//streamit//FHR.str:223
{
  public Detect(float _param_start_freq, int _param_channels, float _param_channel_bandwidth, float _param_hop_threshold, RFtoIFPortal _param_teleport, int _param_pos)
  {
  }
  float start_freq; // ../benchmarks//fhr//streamit//FHR.str:223
  int channels; // ../benchmarks//fhr//streamit//FHR.str:223
  float channel_bandwidth; // ../benchmarks//fhr//streamit//FHR.str:223
  float hop_threshold; // ../benchmarks//fhr//streamit//FHR.str:223
  RFtoIFPortal teleport; // ../benchmarks//fhr//streamit//FHR.str:223
  int pos; // ../benchmarks//fhr//streamit//FHR.str:223
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:225
    float val = inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:226
    if ((val > hop_threshold)) { // ../benchmarks//fhr//streamit//FHR.str:227
      teleport.setLatency(6, 6);
      teleport.set_frequency_from_detector(pos); // ../benchmarks//fhr//streamit//FHR.str:228
    } // ../benchmarks//fhr//streamit//FHR.str:227
    outputChannel.pushFloat(val); // ../benchmarks//fhr//streamit//FHR.str:230
  }
  public void init(final float _param_start_freq, final int _param_channels, final float _param_channel_bandwidth, final float _param_hop_threshold, final RFtoIFPortal _param_teleport, final int _param_pos) { // ../benchmarks//fhr//streamit//FHR.str:223
    start_freq = _param_start_freq; // ../benchmarks//fhr//streamit//FHR.str:223
    channels = _param_channels; // ../benchmarks//fhr//streamit//FHR.str:223
    channel_bandwidth = _param_channel_bandwidth; // ../benchmarks//fhr//streamit//FHR.str:223
    hop_threshold = _param_hop_threshold; // ../benchmarks//fhr//streamit//FHR.str:223
    teleport = _param_teleport; // ../benchmarks//fhr//streamit//FHR.str:223
    pos = _param_pos; // ../benchmarks//fhr//streamit//FHR.str:223
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:223
    addSteadyPhase(1, 1, 1, "work"); // ../benchmarks//fhr//streamit//FHR.str:225
  }
}
class __Output_Filter__ extends Filter // ../benchmarks//fhr//streamit//FHR.str:234
{
  public __Output_Filter__()
  {
  }
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:236
    System.out.println(inputChannel.peekFloat(0)); // ../benchmarks//fhr//streamit//FHR.str:237
    inputChannel.popFloat(); // ../benchmarks//fhr//streamit//FHR.str:238
  }
  public void init() { // ../benchmarks//fhr//streamit//FHR.str:234
    setIOTypes(Float.TYPE, Void.TYPE); // ../benchmarks//fhr//streamit//FHR.str:234
    addSteadyPhase(1, 1, 0, "work"); // ../benchmarks//fhr//streamit//FHR.str:236
  }
}
class Print extends Filter // ../benchmarks//fhr//streamit//FHR.str:274
{
  public Print(int _param_N)
  {
  }
  int N; // ../benchmarks//fhr//streamit//FHR.str:274
  public void work() { // ../benchmarks//fhr//streamit//FHR.str:276
    System.out.println("---------------------------"); // ../benchmarks//fhr//streamit//FHR.str:277
    for (int i = 0; (i < N); i++) { // ../benchmarks//fhr//streamit//FHR.str:278
      System.out.println(inputChannel.peekFloat(0)); // ../benchmarks//fhr//streamit//FHR.str:279
      outputChannel.pushFloat(inputChannel.popFloat()); // ../benchmarks//fhr//streamit//FHR.str:280
    }; // ../benchmarks//fhr//streamit//FHR.str:278
    System.out.println("---------------------------"); // ../benchmarks//fhr//streamit//FHR.str:282
  }
  public void init(final int _param_N) { // ../benchmarks//fhr//streamit//FHR.str:274
    N = _param_N; // ../benchmarks//fhr//streamit//FHR.str:274
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//fhr//streamit//FHR.str:274
    addSteadyPhase(N, N, N, "work"); // ../benchmarks//fhr//streamit//FHR.str:276
  }
}
public class FHR extends StreamItPipeline // ../benchmarks//fhr//streamit//FHR.str:243
{
  public void init() { // ../benchmarks//fhr//streamit//FHR.str:244
    int N = 256; // ../benchmarks//fhr//streamit//FHR.str:246
    float START_FREQ = 2.402E9f; // ../benchmarks//fhr//streamit//FHR.str:249
    int CHANNELS = 79; // ../benchmarks//fhr//streamit//FHR.str:250
    float CHANNEL_BAND = 1000000; // ../benchmarks//fhr//streamit//FHR.str:251
    float HOP_THRESHOLD = 9.0E9f; // ../benchmarks//fhr//streamit//FHR.str:253
    RFtoIFPortal teleport = new RFtoIFPortal(); // ../benchmarks//fhr//streamit//FHR.str:256
    add(new Read_From_AtoD(N)); // ../benchmarks//fhr//streamit//FHR.str:258
    RFtoIF __sa0 = new RFtoIF(N, START_FREQ, START_FREQ, CHANNELS, CHANNEL_BAND);
    add(__sa0);
    teleport.regReceiver(__sa0); // ../benchmarks//fhr//streamit//FHR.str:260
    add(new FFT_Kernel(N)); // ../benchmarks//fhr//streamit//FHR.str:262
    add(new Magnitude()); // ../benchmarks//fhr//streamit//FHR.str:264
    add(new Inject_Hop(N, HOP_THRESHOLD)); // ../benchmarks//fhr//streamit//FHR.str:266
    add(new Check_Freq_Hop(N, START_FREQ, CHANNELS, CHANNEL_BAND, HOP_THRESHOLD, teleport)); // ../benchmarks//fhr//streamit//FHR.str:268
    add(new __Output_Filter__()); // ../benchmarks//fhr//streamit//FHR.str:270
  }
}
