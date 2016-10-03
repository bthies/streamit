import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // FFT2.str:8
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
  int seed; // FFT2.str:9
  public void work() { // FFT2.str:10
    outputChannel.pushFloat(seed); // FFT2.str:11
    seed = (((65793 * seed) + 4282663) % 8388608); // FFT2.str:12
  }
  public void init() { // FFT2.str:8
    seed = 0; // FFT2.str:9
    setIOTypes(Void.TYPE, Float.TYPE); // FFT2.str:8
    addSteadyPhase(0, 0, 1, "work"); // FFT2.str:10
  }
}
class CombineDFT extends Filter // FFT2.str:17
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
  float[] w; // FFT2.str:20
  int n; // FFT2.str:17
  public void work() { // FFT2.str:38
    int i; // FFT2.str:39
    float[] results = new float[(2 * n)]; // FFT2.str:40
    for (i = 0; (i < n); i += 2) { // FFT2.str:43
      int i_plus_1; // FFT2.str:53
      i_plus_1 = (i + 1); // FFT2.str:53
      float y0_r; // FFT2.str:55
      y0_r = inputChannel.peekFloat(i); // FFT2.str:55
      float y0_i; // FFT2.str:56
      y0_i = inputChannel.peekFloat(i_plus_1); // FFT2.str:56
      float y1_r; // FFT2.str:58
      y1_r = inputChannel.peekFloat((n + i)); // FFT2.str:58
      float y1_i; // FFT2.str:59
      y1_i = inputChannel.peekFloat((n + i_plus_1)); // FFT2.str:59
      float weight_real; // FFT2.str:63
      weight_real = w[i]; // FFT2.str:63
      float weight_imag; // FFT2.str:64
      weight_imag = w[i_plus_1]; // FFT2.str:64
      float y1w_r; // FFT2.str:66
      y1w_r = ((y1_r * weight_real) - (y1_i * weight_imag)); // FFT2.str:66
      float y1w_i; // FFT2.str:67
      y1w_i = ((y1_r * weight_imag) + (y1_i * weight_real)); // FFT2.str:67
      results[i] = (y0_r + y1w_r); // FFT2.str:69
      results[(i + 1)] = (y0_i + y1w_i); // FFT2.str:70
      results[(n + i)] = (y0_r - y1w_r); // FFT2.str:72
      results[((n + i) + 1)] = (y0_i - y1w_i); // FFT2.str:73
    }; // FFT2.str:42
    for (i = 0; (i < (2 * n)); i++) { // FFT2.str:77
      inputChannel.popFloat(); // FFT2.str:78
      outputChannel.pushFloat(results[i]); // FFT2.str:79
    }; // FFT2.str:76
  }
  public void init(final int _param_n) { // FFT2.str:22
    n = _param_n; // FFT2.str:22
    w = new float[n]; // FFT2.str:20
    setIOTypes(Float.TYPE, Float.TYPE); // FFT2.str:17
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // FFT2.str:38
    float wn_r = ((float)((float)Math.cos(((2 * 3.141592654f) / n)))); // FFT2.str:23
    float wn_i = ((float)((float)Math.sin(((-2 * 3.141592654f) / n)))); // FFT2.str:24
    float real = 1; // FFT2.str:25
    float imag = 0; // FFT2.str:26
    float next_real, next_imag; // FFT2.str:27
    for (int i = 0; (i < n); i += 2) { // FFT2.str:28
      w[i] = real; // FFT2.str:29
      w[(i + 1)] = imag; // FFT2.str:30
      next_real = ((real * wn_r) - (imag * wn_i)); // FFT2.str:31
      next_imag = ((real * wn_i) + (imag * wn_r)); // FFT2.str:32
      real = next_real; // FFT2.str:33
      imag = next_imag; // FFT2.str:34
    }; // FFT2.str:28
  }
}
class FFTReorderSimple extends Filter // FFT2.str:86
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
  int totalData; // FFT2.str:88
  int n; // FFT2.str:86
  public void work() { // FFT2.str:94
    int i; // FFT2.str:95
    for (i = 0; (i < totalData); i += 4) { // FFT2.str:98
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // FFT2.str:99
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // FFT2.str:100
    }; // FFT2.str:97
    for (i = 2; (i < totalData); i += 4) { // FFT2.str:104
      outputChannel.pushFloat(inputChannel.peekFloat(i)); // FFT2.str:105
      outputChannel.pushFloat(inputChannel.peekFloat((i + 1))); // FFT2.str:106
    }; // FFT2.str:103
    for (i = 0; (i < n); i++) { // FFT2.str:110
      inputChannel.popFloat(); // FFT2.str:111
      inputChannel.popFloat(); // FFT2.str:112
    }; // FFT2.str:109
  }
  public void init(final int _param_n) { // FFT2.str:90
    n = _param_n; // FFT2.str:90
    setIOTypes(Float.TYPE, Float.TYPE); // FFT2.str:86
    addSteadyPhase((2 * n), (2 * n), (2 * n), "work"); // FFT2.str:94
    totalData = (2 * n); // FFT2.str:91
  }
}
class FFTReorder extends Pipeline // FFT2.str:118
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
  public void init(final int n) { // FFT2.str:118
    for (int i = 1; (i < (n / 2)); i *= 2) { // FFT2.str:121
      add(FFTReorderSimple.__construct((n / i))); // FFT2.str:121
    }; // FFT2.str:120
  }
}
class FFTKernel1 extends Pipeline // FFT2.str:126
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
  public void init(final int n) { // FFT2.str:126
    if ((n > 2)) { // FFT2.str:128
      add(AnonFilter_a0.__construct(n)); // FFT2.str:129
    } // FFT2.str:128
    add(CombineDFT.__construct(n)); // FFT2.str:136
  }
}
class FFTKernel2 extends SplitJoin // FFT2.str:140
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
  public void init(final int n) { // FFT2.str:140
    setSplitter(ROUND_ROBIN((2 * n))); // FFT2.str:142
    for (int i = 0; (i < 2); i++) { // FFT2.str:143
      add(AnonFilter_a1.__construct(n)); // FFT2.str:144
    }; // FFT2.str:143
    setJoiner(ROUND_ROBIN((2 * n))); // FFT2.str:150
  }
}
class FFTTestSource extends Filter // FFT2.str:154
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
  int N; // FFT2.str:154
  public void work() { // FFT2.str:156
    int i; // FFT2.str:157
    outputChannel.pushFloat(0.0f); // FFT2.str:158
    outputChannel.pushFloat(0.0f); // FFT2.str:159
    outputChannel.pushFloat(1.0f); // FFT2.str:160
    outputChannel.pushFloat(0.0f); // FFT2.str:161
    for (i = 0; (i < (2 * (N - 2))); i++) { // FFT2.str:164
      outputChannel.pushFloat(0.0f); // FFT2.str:164
    }; // FFT2.str:163
  }
  public void init(final int _param_N) { // FFT2.str:154
    N = _param_N; // FFT2.str:154
    setIOTypes(Void.TYPE, Float.TYPE); // FFT2.str:154
    addSteadyPhase(0, 0, (2 * N), "work"); // FFT2.str:156
  }
}
class FloatPrinter extends Filter // FFT2.str:169
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
  float x; // FFT2.str:171
  public void work() { // FFT2.str:172
    x = inputChannel.popFloat(); // FFT2.str:174
    System.out.println(x); // FFT2.str:175
  }
  public void init() { // FFT2.str:169
    setIOTypes(Float.TYPE, Void.TYPE); // FFT2.str:169
    addSteadyPhase(1, 1, 0, "work"); // FFT2.str:172
  }
}
class AnonFilter_a0 extends SplitJoin // FFT2.str:129
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
  public void init(final int n) { // FFT2.str:129
    setSplitter(ROUND_ROBIN(2)); // FFT2.str:130
    add(FFTKernel1.__construct((n / 2))); // FFT2.str:131
    add(FFTKernel1.__construct((n / 2))); // FFT2.str:132
    setJoiner(ROUND_ROBIN(n)); // FFT2.str:133
  }
}
class AnonFilter_a1 extends Pipeline // FFT2.str:144
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
  public void init(final int n) { // FFT2.str:144
    add(FFTReorder.__construct(n)); // FFT2.str:145
    for (int j = 2; (j <= n); j *= 2) { // FFT2.str:147
      add(CombineDFT.__construct(j)); // FFT2.str:147
    }; // FFT2.str:146
  }
}
public class FFT2 extends StreamItPipeline // FFT2.str:1
{
  public void init() { // FFT2.str:1
    add(RandomSource.__construct()); // FFT2.str:3
    add(FFTKernel2.__construct(16)); // FFT2.str:4
    add(FloatPrinter.__construct()); // FFT2.str:5
  }
  public static void main(String[] args) {
    FFT2 program = new FFT2();
    program.run(args);
    FileWriter.closeAll();
  }
}
