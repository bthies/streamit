import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class CombineDFT extends Filter // FHRFeedback.str:1
{
  private int __param__param_n;
  public static CombineDFT __construct(int _param_n)
  {
    CombineDFT __obj = new CombineDFT();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  float[] w; // FHRFeedback.str:3
  int n; // FHRFeedback.str:1
  public void work() { // FHRFeedback.str:22
    int i; // FHRFeedback.str:23
    float[] results = new float[(2 * n)]; // FHRFeedback.str:24
    for (i = 0; (i < n); i += 2) { // FHRFeedback.str:26
      int i_plus_1; // FHRFeedback.str:27
      i_plus_1 = (i + 1); // FHRFeedback.str:27
      float y0_r; // FHRFeedback.str:29
      y0_r = inputChannel.peekFloat(i); // FHRFeedback.str:29
      float y0_i; // FHRFeedback.str:30
      y0_i = inputChannel.peekFloat(i_plus_1); // FHRFeedback.str:30
      float y1_r; // FHRFeedback.str:32
      y1_r = inputChannel.peekFloat((n + i)); // FHRFeedback.str:32
      float y1_i; // FHRFeedback.str:33
      y1_i = inputChannel.peekFloat((n + i_plus_1)); // FHRFeedback.str:33
      float weight_real; // FHRFeedback.str:35
      weight_real = w[i]; // FHRFeedback.str:35
      float weight_imag; // FHRFeedback.str:36
      weight_imag = w[i_plus_1]; // FHRFeedback.str:36
      float y1w_r; // FHRFeedback.str:38
      y1w_r = ((y1_r * weight_real) - (y1_i * weight_imag)); // FHRFeedback.str:38
      float y1w_i; // FHRFeedback.str:39
      y1w_i = ((y1_r * weight_imag) + (y1_i * weight_real)); // FHRFeedback.str:39
      results[i] = (y0_r + y1w_r); // FHRFeedback.str:41
      results[i_plus_1] = (y0_i + y1w_i); // FHRFeedback.str:42
      results[(n + i)] = (y0_r - y1w_r); // FHRFeedback.str:44
      results[(n + i_plus_1)] = (y0_i - y1w_i); // FHRFeedback.str:45
    }; // FHRFeedback.str:26
    for (i = 0; (i < (2 * n)); i++) { // FHRFeedback.str:48
      inputChannel.popFloat(); // FHRFeedback.str:49
      outputChannel.pushFloat(results[i]); // FHRFeedback.str:50
    }; // FHRFeedback.str:48
    iterationCount++; // filter count update
  }
  public void init(final int _param_n) { // FHRFeedback.str:5
    n = _param_n; // FHRFeedback.str:5
    w = new float[n]; // FHRFeedback.str:3
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:1
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // FHRFeedback.str:22
    float wn_r = ((float)((float)Math.cos(((2 * 3.141592654f) / n)))); // FHRFeedback.str:6
    float wn_i = ((float)((float)Math.sin(((-2 * 3.141592654f) / n)))); // FHRFeedback.str:7
    float real = 1; // FHRFeedback.str:8
    float imag = 0; // FHRFeedback.str:9
    float next_real, next_imag; // FHRFeedback.str:10
    for (int i = 0; (i < n); i += 2) { // FHRFeedback.str:12
      w[i] = real; // FHRFeedback.str:13
      w[(i + 1)] = imag; // FHRFeedback.str:14
      next_real = ((real * wn_r) - (imag * wn_i)); // FHRFeedback.str:15
      next_imag = ((real * wn_i) + (imag * wn_r)); // FHRFeedback.str:16
      real = next_real; // FHRFeedback.str:17
      imag = next_imag; // FHRFeedback.str:18
    }; // FHRFeedback.str:12
  }
}
class FFTReorderSimple extends Filter // FHRFeedback.str:56
{
  private int __param__param_n;
  public static FFTReorderSimple __construct(int _param_n)
  {
    FFTReorderSimple __obj = new FFTReorderSimple();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  int totalData; // FHRFeedback.str:58
  int n; // FHRFeedback.str:56
  public void work() { // FHRFeedback.str:64
    int i; // FHRFeedback.str:65
    for (i = 0; (i < totalData); i += 4) { // FHRFeedback.str:67
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // FHRFeedback.str:68
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // FHRFeedback.str:69
    }; // FHRFeedback.str:67
    for (i = 2; (i < totalData); i += 4) { // FHRFeedback.str:72
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // FHRFeedback.str:73
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // FHRFeedback.str:74
    }; // FHRFeedback.str:72
    for (i = 0; (i < n); i++) { // FHRFeedback.str:77
      inputChannel.popFloat(); // FHRFeedback.str:78
      inputChannel.popFloat(); // FHRFeedback.str:79
    }; // FHRFeedback.str:77
    iterationCount++; // filter count update
  }
  public void init(final int _param_n) { // FHRFeedback.str:60
    n = _param_n; // FHRFeedback.str:60
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:56
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // FHRFeedback.str:64
    totalData = (2 * n); // FHRFeedback.str:61
  }
}
class FFTReorder extends Pipeline // FHRFeedback.str:85
{
  private int __param_n;
  public static FFTReorder __construct(int n)
  {
    FFTReorder __obj = new FFTReorder();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // FHRFeedback.str:86
    for (int i = 1; (i < (n / 2)); i *= 2) { // FHRFeedback.str:87
      add(FFTReorderSimple.__construct((n / i))); // FHRFeedback.str:88
    }; // FHRFeedback.str:87
  }
}
class FFT_Kernel extends Pipeline // FHRFeedback.str:93
{
  private int __param_n;
  public static FFT_Kernel __construct(int n)
  {
    FFT_Kernel __obj = new FFT_Kernel();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // FHRFeedback.str:94
    add(FFTReorder.__construct(n)); // FHRFeedback.str:95
    for (int j = 2; (j <= n); j *= 2) { // FHRFeedback.str:96
      add(CombineDFT.__construct(j)); // FHRFeedback.str:97
    }; // FHRFeedback.str:96
  }
}
class Float_Identity extends Filter // FHRFeedback.str:104
{
  public static Float_Identity __construct()
  {
    Float_Identity __obj = new Float_Identity();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // FHRFeedback.str:106
    outputChannel.pushFloat(inputChannel.popFloat()); // FHRFeedback.str:107
    iterationCount++; // filter count update
  }
  public void init() { // FHRFeedback.str:104
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:104
    addSteadyPhase(1, 1, 1, "work"); // FHRFeedback.str:106
  }
}
class Read_From_AtoD extends Filter // FHRFeedback.str:112
{
  private int __param__param_N;
  public static Read_From_AtoD __construct(int _param_N)
  {
    Read_From_AtoD __obj = new Read_From_AtoD();
    __obj.__param__param_N = _param_N;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N);
  }
  int N; // FHRFeedback.str:112
  public void work() { // FHRFeedback.str:114
    for (int i = 1; (i <= N); i++) { // FHRFeedback.str:115
      float val; // FHRFeedback.str:116
      val = ((float)((float)Math.sin(((i * 3.141592653589793f) / N)))); // FHRFeedback.str:116
      outputChannel.pushFloat((val * val)); // FHRFeedback.str:117
    }; // FHRFeedback.str:115
    iterationCount++; // filter count update
  }
  public void init(final int _param_N) { // FHRFeedback.str:112
    N = _param_N; // FHRFeedback.str:112
    setIOTypes(Void.TYPE, Float.TYPE); // FHRFeedback.str:112
    addSteadyPhase(0, 0, N, "work"); // FHRFeedback.str:114
  }
}
interface RFtoIFInterface {
  public void set_frequency_from_detector(int source);
}
class RFtoIFPortal extends Portal implements RFtoIFInterface {
  public void set_frequency_from_detector(int source) { }
}
class RFtoIF extends Filter implements RFtoIFInterface // FHRFeedback.str:123
{
  private int __param__param_N;
  private float __param__param__start_freq;
  private float __param__param__current_freq;
  private int __param__param_channels;
  private float __param__param_channel_bandwidth;
  public static RFtoIF __construct(int _param_N, float _param__start_freq, float _param__current_freq, int _param_channels, float _param_channel_bandwidth)
  {
    RFtoIF __obj = new RFtoIF();
    __obj.__param__param_N = _param_N;
    __obj.__param__param__start_freq = _param__start_freq;
    __obj.__param__param__current_freq = _param__current_freq;
    __obj.__param__param_channels = _param_channels;
    __obj.__param__param_channel_bandwidth = _param_channel_bandwidth;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N, __param__param__start_freq, __param__param__current_freq, __param__param_channels, __param__param_channel_bandwidth);
  }
  float[] weights; // FHRFeedback.str:126
  int size, count; // FHRFeedback.str:127
  int[] frequency_index; // FHRFeedback.str:132
  int N; // FHRFeedback.str:123
  float _start_freq; // FHRFeedback.str:123
  float _current_freq; // FHRFeedback.str:123
  int channels; // FHRFeedback.str:123
  float channel_bandwidth; // FHRFeedback.str:123
  public void work() { // FHRFeedback.str:138
    int iterVar = ((iter() * 2) * N); // FHRFeedback.str:139
    for (int i = 0; (i < (2 * N)); i++) { // FHRFeedback.str:140
      outputChannel.pushFloat((inputChannel.popFloat() * weights[count++])); // FHRFeedback.str:141
      iterVar++; // FHRFeedback.str:143
      if ((count == size)) { // FHRFeedback.str:145
        count = 0; // FHRFeedback.str:146
      } // FHRFeedback.str:145
    }; // FHRFeedback.str:140
    for (int i = 0; (i < N); i++) { // FHRFeedback.str:150
      int detector; // FHRFeedback.str:152
      detector = ((int)(inputChannel.popFloat())); // FHRFeedback.str:152
      if ((detector != 0)) { // FHRFeedback.str:153
        set_frequency_from_detector(detector); // FHRFeedback.str:154
      } // FHRFeedback.str:153
    }; // FHRFeedback.str:150
    iterationCount++; // filter count update
  }
  public void set_frequency_from_detector(int source) { // FHRFeedback.str:159
    float _freq = (_start_freq + (frequency_index[source] * channel_bandwidth)); // FHRFeedback.str:160
    frequency_index[source]++; // FHRFeedback.str:161
    if ((frequency_index[source] == channels)) { // FHRFeedback.str:162
      frequency_index[source] = 0; // FHRFeedback.str:163
    } // FHRFeedback.str:162
    set_frequency(_freq); // FHRFeedback.str:165
  }
  public void set_frequency(float _freq) { // FHRFeedback.str:168
    count = 0; // FHRFeedback.str:170
    size = ((int)(((_start_freq / _freq) * N))); // FHRFeedback.str:171
    for (int i = 0; (i < size); i++) { // FHRFeedback.str:172
      weights[i] = ((float)((float)Math.sin(((i * 3.141592653589793f) / size)))); // FHRFeedback.str:173
    }; // FHRFeedback.str:172
  }
  public void init(final int _param_N, final float _param__start_freq, final float _param__current_freq, final int _param_channels, final float _param_channel_bandwidth) { // FHRFeedback.str:134
    N = _param_N; // FHRFeedback.str:134
    _start_freq = _param__start_freq; // FHRFeedback.str:134
    _current_freq = _param__current_freq; // FHRFeedback.str:134
    channels = _param_channels; // FHRFeedback.str:134
    channel_bandwidth = _param_channel_bandwidth; // FHRFeedback.str:134
    weights = new float[N]; // FHRFeedback.str:126
    frequency_index = new int[8]; // FHRFeedback.str:132
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:123
    addSteadyPhase(1, (N * 3), (N * 2), "work"); // FHRFeedback.str:138
    set_frequency(_current_freq); // FHRFeedback.str:135
  }
}
class Magnitude extends Filter // FHRFeedback.str:179
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
  public void work() { // FHRFeedback.str:181
    float f1 = inputChannel.popFloat(); // FHRFeedback.str:182
    float f2 = inputChannel.popFloat(); // FHRFeedback.str:183
    outputChannel.pushFloat(mag(f1, f2)); // FHRFeedback.str:184
    iterationCount++; // filter count update
  }
  public float mag(float real, float imag) { // FHRFeedback.str:187
    return ((float)((float)Math.sqrt(((real * real) + (imag * imag))))); // FHRFeedback.str:188
  }
  public void init() { // FHRFeedback.str:179
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:179
    addSteadyPhase(2, 2, 1, "work"); // FHRFeedback.str:181
  }
}
class Inject_Hop extends Filter // FHRFeedback.str:192
{
  private int __param__param_N;
  private float __param__param_hop_threshold;
  public static Inject_Hop __construct(int _param_N, float _param_hop_threshold)
  {
    Inject_Hop __obj = new Inject_Hop();
    __obj.__param__param_N = _param_N;
    __obj.__param__param_hop_threshold = _param_hop_threshold;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N, __param__param_hop_threshold);
  }
  int[] hops; // FHRFeedback.str:194
  int N; // FHRFeedback.str:192
  float hop_threshold; // FHRFeedback.str:192
  public void work() { // FHRFeedback.str:203
    int hop_index = (iter() & 3); // FHRFeedback.str:204
    for (int i = 1; (i <= hops[hop_index]); i++) { // FHRFeedback.str:205
      outputChannel.pushFloat(inputChannel.popFloat()); // FHRFeedback.str:206
    }; // FHRFeedback.str:205
    outputChannel.pushFloat((2 * hop_threshold)); // FHRFeedback.str:209
    inputChannel.popFloat(); // FHRFeedback.str:210
    for (int i = (hops[hop_index] + 2); (i <= N); i++) { // FHRFeedback.str:212
      outputChannel.pushFloat(inputChannel.popFloat()); // FHRFeedback.str:213
    }; // FHRFeedback.str:212
    iterationCount++; // filter count update
  }
  public void init(final int _param_N, final float _param_hop_threshold) { // FHRFeedback.str:196
    N = _param_N; // FHRFeedback.str:196
    hop_threshold = _param_hop_threshold; // FHRFeedback.str:196
    hops = new int[4]; // FHRFeedback.str:194
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:192
    addSteadyPhase(N, N, N, "work"); // FHRFeedback.str:203
    hops[0] = ((N / 4) - 2); // FHRFeedback.str:197
    hops[1] = ((N / 4) - 1); // FHRFeedback.str:198
    hops[2] = (N / 2); // FHRFeedback.str:199
    hops[3] = ((N / 2) + 1); // FHRFeedback.str:200
  }
}
class Check_Freq_Hop extends SplitJoin // FHRFeedback.str:218
{
  private int __param_N;
  private int __param_channels;
  private float __param_start_freq;
  private float __param_channel_bandwidth;
  private float __param_hop_threshold;
  public static Check_Freq_Hop __construct(int N, int channels, float start_freq, float channel_bandwidth, float hop_threshold)
  {
    Check_Freq_Hop __obj = new Check_Freq_Hop();
    __obj.__param_N = N;
    __obj.__param_channels = channels;
    __obj.__param_start_freq = start_freq;
    __obj.__param_channel_bandwidth = channel_bandwidth;
    __obj.__param_hop_threshold = hop_threshold;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_N, __param_channels, __param_start_freq, __param_channel_bandwidth, __param_hop_threshold);
  }
  public void init(final int N, final int channels, final float start_freq, final float channel_bandwidth, final float hop_threshold) { // FHRFeedback.str:219
    setSplitter(WEIGHTED_ROUND_ROBIN(((N / 4) - 2), 1, 1, (N / 2), 1, 1, ((N / 4) - 2))); // FHRFeedback.str:224
    for (int i = 1; (i <= 7); i++) { // FHRFeedback.str:226
      if ((((i == 1) || (i == 4)) || (i == 7))) { // FHRFeedback.str:227
        add(AnonFilter_a0.__construct()); // FHRFeedback.str:228
      } else { // FHRFeedback.str:234
        add(Detect.__construct(start_freq, channels, channel_bandwidth, hop_threshold, i)); // FHRFeedback.str:235
      } // FHRFeedback.str:227
    }; // FHRFeedback.str:226
    setJoiner(WEIGHTED_ROUND_ROBIN((2 * ((N / 4) - 2)), 2, 2, (2 * (N / 2)), 2, 2, (2 * ((N / 4) - 2)))); // FHRFeedback.str:239
  }
}
class Detect extends Filter // FHRFeedback.str:242
{
  private float __param__param_start_freq;
  private int __param__param_channels;
  private float __param__param_channel_bandwidth;
  private float __param__param_hop_threshold;
  private int __param__param_pos;
  public static Detect __construct(float _param_start_freq, int _param_channels, float _param_channel_bandwidth, float _param_hop_threshold, int _param_pos)
  {
    Detect __obj = new Detect();
    __obj.__param__param_start_freq = _param_start_freq;
    __obj.__param__param_channels = _param_channels;
    __obj.__param__param_channel_bandwidth = _param_channel_bandwidth;
    __obj.__param__param_hop_threshold = _param_hop_threshold;
    __obj.__param__param_pos = _param_pos;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_start_freq, __param__param_channels, __param__param_channel_bandwidth, __param__param_hop_threshold, __param__param_pos);
  }
  float start_freq; // FHRFeedback.str:242
  int channels; // FHRFeedback.str:242
  float channel_bandwidth; // FHRFeedback.str:242
  float hop_threshold; // FHRFeedback.str:242
  int pos; // FHRFeedback.str:242
  public void work() { // FHRFeedback.str:244
    float val = inputChannel.popFloat(); // FHRFeedback.str:245
    outputChannel.pushFloat(val); // FHRFeedback.str:246
    if ((val > hop_threshold)) { // FHRFeedback.str:247
      outputChannel.pushFloat(pos); // FHRFeedback.str:248
    } else { // FHRFeedback.str:249
      outputChannel.pushFloat(0); // FHRFeedback.str:250
    } // FHRFeedback.str:247
    iterationCount++; // filter count update
  }
  public void init(final float _param_start_freq, final int _param_channels, final float _param_channel_bandwidth, final float _param_hop_threshold, final int _param_pos) { // FHRFeedback.str:242
    start_freq = _param_start_freq; // FHRFeedback.str:242
    channels = _param_channels; // FHRFeedback.str:242
    channel_bandwidth = _param_channel_bandwidth; // FHRFeedback.str:242
    hop_threshold = _param_hop_threshold; // FHRFeedback.str:242
    pos = _param_pos; // FHRFeedback.str:242
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:242
    addSteadyPhase(1, 1, 2, "work"); // FHRFeedback.str:244
  }
}
class __Output_Filter__ extends Filter // FHRFeedback.str:256
{
  public static __Output_Filter__ __construct()
  {
    __Output_Filter__ __obj = new __Output_Filter__();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // FHRFeedback.str:258
    System.out.println(inputChannel.peekFloat(0)); // FHRFeedback.str:259
    inputChannel.popFloat(); // FHRFeedback.str:260
    iterationCount++; // filter count update
  }
  public void init() { // FHRFeedback.str:256
    setIOTypes(Float.TYPE, Void.TYPE); // FHRFeedback.str:256
    addSteadyPhase(1, 1, 0, "work"); // FHRFeedback.str:258
  }
}
class Print extends Filter // FHRFeedback.str:301
{
  private int __param__param_N;
  public static Print __construct(int _param_N)
  {
    Print __obj = new Print();
    __obj.__param__param_N = _param_N;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N);
  }
  int N; // FHRFeedback.str:301
  public void work() { // FHRFeedback.str:303
    System.out.println("---------------------------"); // FHRFeedback.str:304
    for (int i = 0; (i < N); i++) { // FHRFeedback.str:305
      System.out.println(inputChannel.peekFloat(0)); // FHRFeedback.str:306
      outputChannel.pushFloat(inputChannel.popFloat()); // FHRFeedback.str:307
    }; // FHRFeedback.str:305
    System.out.println("---------------------------"); // FHRFeedback.str:309
    iterationCount++; // filter count update
  }
  public void init(final int _param_N) { // FHRFeedback.str:301
    N = _param_N; // FHRFeedback.str:301
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:301
    addSteadyPhase(N, N, N, "work"); // FHRFeedback.str:303
  }
}
class AnonFilter_a0 extends Filter // FHRFeedback.str:228
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
  public void work() { // FHRFeedback.str:229
    outputChannel.pushFloat(inputChannel.popFloat()); // FHRFeedback.str:230
    outputChannel.pushFloat(0); // FHRFeedback.str:231
    iterationCount++; // filter count update
  }
  public void init() { // FHRFeedback.str:228
    setIOTypes(Float.TYPE, Float.TYPE); // FHRFeedback.str:228
    addSteadyPhase(1, 1, 2, "work"); // FHRFeedback.str:229
  }
}
class AnonFilter_a1 extends Pipeline // FHRFeedback.str:282
{
  private int __param_CHANNELS;
  private float __param_CHANNEL_BAND;
  private float __param_HOP_THRESHOLD;
  private int __param_N;
  private float __param_START_FREQ;
  public static AnonFilter_a1 __construct(int CHANNELS, float CHANNEL_BAND, float HOP_THRESHOLD, int N, float START_FREQ)
  {
    AnonFilter_a1 __obj = new AnonFilter_a1();
    __obj.__param_CHANNELS = CHANNELS;
    __obj.__param_CHANNEL_BAND = CHANNEL_BAND;
    __obj.__param_HOP_THRESHOLD = HOP_THRESHOLD;
    __obj.__param_N = N;
    __obj.__param_START_FREQ = START_FREQ;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_CHANNELS, __param_CHANNEL_BAND, __param_HOP_THRESHOLD, __param_N, __param_START_FREQ);
  }
  public void init(final int CHANNELS, final float CHANNEL_BAND, final float HOP_THRESHOLD, final int N, final float START_FREQ) { // FHRFeedback.str:282
    add(RFtoIF.__construct(N, START_FREQ, START_FREQ, CHANNELS, CHANNEL_BAND)); // FHRFeedback.str:283
    add(FFT_Kernel.__construct(N)); // FHRFeedback.str:284
    add(Magnitude.__construct()); // FHRFeedback.str:285
    add(Inject_Hop.__construct(N, HOP_THRESHOLD)); // FHRFeedback.str:286
    add(Check_Freq_Hop.__construct(N, CHANNELS, START_FREQ, CHANNEL_BAND, HOP_THRESHOLD)); // FHRFeedback.str:287
  }
}
class AnonFilter_a2 extends FeedbackLoop // FHRFeedback.str:280
{
  private int __param_CHANNELS;
  private float __param_CHANNEL_BAND;
  private float __param_HOP_THRESHOLD;
  private int __param_N;
  private float __param_START_FREQ;
  public static AnonFilter_a2 __construct(int CHANNELS, float CHANNEL_BAND, float HOP_THRESHOLD, int N, float START_FREQ)
  {
    AnonFilter_a2 __obj = new AnonFilter_a2();
    __obj.__param_CHANNELS = CHANNELS;
    __obj.__param_CHANNEL_BAND = CHANNEL_BAND;
    __obj.__param_HOP_THRESHOLD = HOP_THRESHOLD;
    __obj.__param_N = N;
    __obj.__param_START_FREQ = START_FREQ;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_CHANNELS, __param_CHANNEL_BAND, __param_HOP_THRESHOLD, __param_N, __param_START_FREQ);
  }
  public void init(final int CHANNELS, final float CHANNEL_BAND, final float HOP_THRESHOLD, final int N, final float START_FREQ) { // FHRFeedback.str:280
    setJoiner(WEIGHTED_ROUND_ROBIN((2 * N), N)); // FHRFeedback.str:281
    setBody(AnonFilter_a1.__construct(CHANNELS, CHANNEL_BAND, HOP_THRESHOLD, N, START_FREQ)); // FHRFeedback.str:282
    setLoop(new Identity(Float.TYPE)); // FHRFeedback.str:289
    setSplitter(ROUND_ROBIN(1)); // FHRFeedback.str:290
    for (int i = 0; (i < (6 * N)); i++) { // FHRFeedback.str:292
      enqueueFloat(0); // FHRFeedback.str:293
    }; // FHRFeedback.str:292
  }
}
public class FHRFeedback extends StreamItPipeline // FHRFeedback.str:265
{
  public void init() { // FHRFeedback.str:266
    int N = 256; // FHRFeedback.str:268
    float START_FREQ = 2.402E9f; // FHRFeedback.str:271
    int CHANNELS = 79; // FHRFeedback.str:272
    float CHANNEL_BAND = 1000000; // FHRFeedback.str:273
    float HOP_THRESHOLD = 9.0E9f; // FHRFeedback.str:275
    add(Read_From_AtoD.__construct(N)); // FHRFeedback.str:278
    add(AnonFilter_a2.__construct(CHANNELS, CHANNEL_BAND, HOP_THRESHOLD, N, START_FREQ)); // FHRFeedback.str:280
    add(__Output_Filter__.__construct()); // FHRFeedback.str:297
  }
  public static void main(String[] args) {
    FHRFeedback program = new FHRFeedback();
    program.run(args);
    FileWriter.closeAll();
  }
}
