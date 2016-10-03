import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // FFT2_256.str:8
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
  int seed; // FFT2_256.str:9
  public void work() { // FFT2_256.str:10
    outputChannel.pushFloat(seed); // FFT2_256.str:11
    seed = (((65793 * seed) + 4282663) % 8388608); // FFT2_256.str:12
  }
  public void init() { // FFT2_256.str:8
    seed = 0; // FFT2_256.str:9
    setIOTypes(Void.TYPE, Float.TYPE); // FFT2_256.str:8
    addSteadyPhase(0, 0, 1, "work"); // FFT2_256.str:10
  }
}
class CombineDFT extends Filter // FFT2_256.str:16
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
  float[] w; // FFT2_256.str:19
  int n; // FFT2_256.str:16
  public void work() { // FFT2_256.str:37
    int i; // FFT2_256.str:38
    float[] results = new float[(2 * n)]; // FFT2_256.str:39
    for (i = 0; (i < n); i += 2) { // FFT2_256.str:42
      int i_plus_1; // FFT2_256.str:52
      i_plus_1 = (i + 1); // FFT2_256.str:52
      float y0_r; // FFT2_256.str:54
      y0_r = inputChannel.peekFloat(i); // FFT2_256.str:54
      float y0_i; // FFT2_256.str:55
      y0_i = inputChannel.peekFloat(i_plus_1); // FFT2_256.str:55
      float y1_r; // FFT2_256.str:57
      y1_r = inputChannel.peekFloat((n + i)); // FFT2_256.str:57
      float y1_i; // FFT2_256.str:58
      y1_i = inputChannel.peekFloat((n + i_plus_1)); // FFT2_256.str:58
      float weight_real; // FFT2_256.str:62
      weight_real = w[i]; // FFT2_256.str:62
      float weight_imag; // FFT2_256.str:63
      weight_imag = w[i_plus_1]; // FFT2_256.str:63
      float y1w_r; // FFT2_256.str:65
      y1w_r = ((y1_r * weight_real) - (y1_i * weight_imag)); // FFT2_256.str:65
      float y1w_i; // FFT2_256.str:66
      y1w_i = ((y1_r * weight_imag) + (y1_i * weight_real)); // FFT2_256.str:66
      results[i] = (y0_r + y1w_r); // FFT2_256.str:68
      results[(i + 1)] = (y0_i + y1w_i); // FFT2_256.str:69
      results[(n + i)] = (y0_r - y1w_r); // FFT2_256.str:71
      results[((n + i) + 1)] = (y0_i - y1w_i); // FFT2_256.str:72
    }; // FFT2_256.str:41
    for (i = 0; (i < (2 * n)); i++) { // FFT2_256.str:76
      inputChannel.popFloat(); // FFT2_256.str:77
      outputChannel.pushFloat(results[i]); // FFT2_256.str:78
    }; // FFT2_256.str:75
  }
  public void init(final int _param_n) { // FFT2_256.str:21
    n = _param_n; // FFT2_256.str:21
    w = new float[n]; // FFT2_256.str:19
    setIOTypes(Float.TYPE, Float.TYPE); // FFT2_256.str:16
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // FFT2_256.str:37
    float wn_r = ((float)((float)Math.cos(((2 * 3.141592654f) / n)))); // FFT2_256.str:22
    float wn_i = ((float)((float)Math.sin(((-2 * 3.141592654f) / n)))); // FFT2_256.str:23
    float real = 1; // FFT2_256.str:24
    float imag = 0; // FFT2_256.str:25
    float next_real, next_imag; // FFT2_256.str:26
    for (int i = 0; (i < n); i += 2) { // FFT2_256.str:27
      w[i] = real; // FFT2_256.str:28
      w[(i + 1)] = imag; // FFT2_256.str:29
      next_real = ((real * wn_r) - (imag * wn_i)); // FFT2_256.str:30
      next_imag = ((real * wn_i) + (imag * wn_r)); // FFT2_256.str:31
      real = next_real; // FFT2_256.str:32
      imag = next_imag; // FFT2_256.str:33
    }; // FFT2_256.str:27
  }
}
class FFTReorderSimple extends Filter // FFT2_256.str:85
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
  int totalData; // FFT2_256.str:87
  int n; // FFT2_256.str:85
  public void work() { // FFT2_256.str:93
    int i; // FFT2_256.str:94
    for (i = 0; (i < totalData); i += 4) { // FFT2_256.str:97
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // FFT2_256.str:98
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // FFT2_256.str:99
    }; // FFT2_256.str:96
    for (i = 2; (i < totalData); i += 4) { // FFT2_256.str:103
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // FFT2_256.str:104
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // FFT2_256.str:105
    }; // FFT2_256.str:102
    for (i = 0; (i < n); i++) { // FFT2_256.str:109
      inputChannel.popFloat(); // FFT2_256.str:110
      inputChannel.popFloat(); // FFT2_256.str:111
    }; // FFT2_256.str:108
  }
  public void init(final int _param_n) { // FFT2_256.str:89
    n = _param_n; // FFT2_256.str:89
    setIOTypes(Float.TYPE, Float.TYPE); // FFT2_256.str:85
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // FFT2_256.str:93
    totalData = (2 * n); // FFT2_256.str:90
  }
}
class FFTReorder extends Pipeline // FFT2_256.str:117
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
  public void init(final int n) { // FFT2_256.str:117
    for (int i = 1; (i < (n / 2)); i *= 2) { // FFT2_256.str:120
      add(FFTReorderSimple.__construct((n / i))); // FFT2_256.str:120
    }; // FFT2_256.str:119
  }
}
class FFTKernel1 extends Pipeline // FFT2_256.str:125
{
  private int __param_n;
  public static FFTKernel1 __construct(int n)
  {
    FFTKernel1 __obj = new FFTKernel1();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // FFT2_256.str:125
    if ((n > 2)) { // FFT2_256.str:127
      add(AnonFilter_a0.__construct(n)); // FFT2_256.str:128
    } // FFT2_256.str:127
    add(CombineDFT.__construct(n)); // FFT2_256.str:135
  }
}
class FFTKernel2 extends SplitJoin // FFT2_256.str:139
{
  private int __param_n;
  public static FFTKernel2 __construct(int n)
  {
    FFTKernel2 __obj = new FFTKernel2();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // FFT2_256.str:139
    setSplitter(ROUND_ROBIN((2 * n))); // FFT2_256.str:141
    for (int i = 0; (i < 2); i++) { // FFT2_256.str:142
      add(AnonFilter_a1.__construct(n)); // FFT2_256.str:143
    }; // FFT2_256.str:142
    setJoiner(ROUND_ROBIN((2 * n))); // FFT2_256.str:149
  }
}
class FFTTestSource extends Filter // FFT2_256.str:153
{
  private int __param__param_N;
  public static FFTTestSource __construct(int _param_N)
  {
    FFTTestSource __obj = new FFTTestSource();
    __obj.__param__param_N = _param_N;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N);
  }
  int N; // FFT2_256.str:153
  public void work() { // FFT2_256.str:155
    int i; // FFT2_256.str:156
    outputChannel.pushFloat(0.0f); // FFT2_256.str:157
    outputChannel.pushFloat(0.0f); // FFT2_256.str:158
    outputChannel.pushFloat(1.0f); // FFT2_256.str:159
    outputChannel.pushFloat(0.0f); // FFT2_256.str:160
    for (i = 0; (i < (2 * (N - 2))); i++) { // FFT2_256.str:163
      outputChannel.pushFloat(0.0f); // FFT2_256.str:163
    }; // FFT2_256.str:162
  }
  public void init(final int _param_N) { // FFT2_256.str:153
    N = _param_N; // FFT2_256.str:153
    setIOTypes(Void.TYPE, Float.TYPE); // FFT2_256.str:153
    addSteadyPhase(0, 0, (2 * N), "work"); // FFT2_256.str:155
  }
}
class FloatPrinter extends Filter // FFT2_256.str:168
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
  float x; // FFT2_256.str:170
  public void work() { // FFT2_256.str:171
    x = inputChannel.popFloat(); // FFT2_256.str:173
    System.out.println(x); // FFT2_256.str:174
  }
  public void init() { // FFT2_256.str:168
    setIOTypes(Float.TYPE, Void.TYPE); // FFT2_256.str:168
    addSteadyPhase(1, 1, 0, "work"); // FFT2_256.str:171
  }
}
class AnonFilter_a0 extends SplitJoin // FFT2_256.str:128
{
  private int __param_n;
  public static AnonFilter_a0 __construct(int n)
  {
    AnonFilter_a0 __obj = new AnonFilter_a0();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // FFT2_256.str:128
    setSplitter(ROUND_ROBIN(2)); // FFT2_256.str:129
    add(FFTKernel1.__construct((n / 2))); // FFT2_256.str:130
    add(FFTKernel1.__construct((n / 2))); // FFT2_256.str:131
    setJoiner(ROUND_ROBIN(n)); // FFT2_256.str:132
  }
}
class AnonFilter_a1 extends Pipeline // FFT2_256.str:143
{
  private int __param_n;
  public static AnonFilter_a1 __construct(int n)
  {
    AnonFilter_a1 __obj = new AnonFilter_a1();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // FFT2_256.str:143
    add(FFTReorder.__construct(n)); // FFT2_256.str:144
    for (int j = 2; (j <= n); j *= 2) { // FFT2_256.str:146
      add(CombineDFT.__construct(j)); // FFT2_256.str:146
    }; // FFT2_256.str:145
  }
}
public class FFT2_256 extends StreamItPipeline // FFT2_256.str:1
{
  public void init() { // FFT2_256.str:1
    add(RandomSource.__construct()); // FFT2_256.str:3
    add(FFTKernel2.__construct(64)); // FFT2_256.str:4
    add(FloatPrinter.__construct()); // FFT2_256.str:5
  }
  public static void main(String[] args) {
    FFT2_256 program = new FFT2_256();
    program.run(args);
    FileWriter.closeAll();
  }
}
