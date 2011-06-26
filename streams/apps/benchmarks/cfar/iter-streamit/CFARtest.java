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
class CFAR extends Pipeline // ../benchmarks//cfar//streamit//CFARtest.str:16
{
  public CFAR(int N_rg, int N_cfar, int G)
  {
  }
  public void init(final int N_rg, final int N_cfar, final int G) { // ../benchmarks//cfar//streamit//CFARtest.str:17
    add(new SquareAndScale(N_cfar)); // ../benchmarks//cfar//streamit//CFARtest.str:19
    add(new CFAR_gather(N_rg, N_cfar, G)); // ../benchmarks//cfar//streamit//CFARtest.str:21
  }
}
class SquareAndScale extends Filter // ../benchmarks//cfar//streamit//CFARtest.str:24
{
  public SquareAndScale(int _param_N_cfar)
  {
  }
  int N_cfar; // ../benchmarks//cfar//streamit//CFARtest.str:24
  public void work() { // ../benchmarks//cfar//streamit//CFARtest.str:27
    Complex c = new Complex(); // ../benchmarks//cfar//streamit//CFARtest.str:28
    c = (Complex)inputChannel.pop(); // ../benchmarks//cfar//streamit//CFARtest.str:28
    float mag = (float)Math.sqrt(((c.real * c.real) + (c.imag * c.imag))); // ../benchmarks//cfar//streamit//CFARtest.str:29
    outputChannel.pushFloat(((mag * mag) / (2 * N_cfar))); // ../benchmarks//cfar//streamit//CFARtest.str:30
  }
  public void init(final int _param_N_cfar) { // ../benchmarks//cfar//streamit//CFARtest.str:24
    N_cfar = _param_N_cfar; // ../benchmarks//cfar//streamit//CFARtest.str:24
    setIOTypes(Complex.class, Float.TYPE); // ../benchmarks//cfar//streamit//CFARtest.str:24
    addSteadyPhase(1, 1, 1, "work"); // ../benchmarks//cfar//streamit//CFARtest.str:26
  }
}
class CFAR_gather extends Filter // ../benchmarks//cfar//streamit//CFARtest.str:37
{
  public CFAR_gather(int _param_N_rg, int _param_N_cfar, int _param_G)
  {
    setStateful(true);
  }
  int pos; // ../benchmarks//cfar//streamit//CFARtest.str:39
  float[(N_cfar + G)] poke; // ../benchmarks//cfar//streamit//CFARtest.str:40
  int N_rg; // ../benchmarks//cfar//streamit//CFARtest.str:37
  int N_cfar; // ../benchmarks//cfar//streamit//CFARtest.str:37
  int G; // ../benchmarks//cfar//streamit//CFARtest.str:37
  public void work() { // ../benchmarks//cfar//streamit//CFARtest.str:46
    float sum = 0; // ../benchmarks//cfar//streamit//CFARtest.str:47
    for (int i = 0; ((i < N_cfar) && (((i + pos) - N_cfar) >= 0)); i++) { // ../benchmarks//cfar//streamit//CFARtest.str:50
      sum += poke[((N_cfar - i) - 1)]; // ../benchmarks//cfar//streamit//CFARtest.str:50
    }; // ../benchmarks//cfar//streamit//CFARtest.str:49
    for (int i = (G + 1); ((i <= (N_cfar + G)) && ((i + pos) < N_rg)); i++) { // ../benchmarks//cfar//streamit//CFARtest.str:53
      sum += inputChannel.peekFloat(i); // ../benchmarks//cfar//streamit//CFARtest.str:53
    }; // ../benchmarks//cfar//streamit//CFARtest.str:52
    outputChannel.pushFloat(sum); // ../benchmarks//cfar//streamit//CFARtest.str:55
    for (int i = 1; (i < (N_cfar + G)); i++) { // ../benchmarks//cfar//streamit//CFARtest.str:58
      poke[(i - 1)] = poke[i]; // ../benchmarks//cfar//streamit//CFARtest.str:58
    }; // ../benchmarks//cfar//streamit//CFARtest.str:57
    poke[((N_cfar + G) - 1)] = inputChannel.popFloat(); // ../benchmarks//cfar//streamit//CFARtest.str:59
    pos++; // ../benchmarks//cfar//streamit//CFARtest.str:61
    if ((pos == N_rg)) { // ../benchmarks//cfar//streamit//CFARtest.str:62
      pos = 0; // ../benchmarks//cfar//streamit//CFARtest.str:62
    } // ../benchmarks//cfar//streamit//CFARtest.str:62
  }
  public void init(final int _param_N_rg, final int _param_N_cfar, final int _param_G) { // ../benchmarks//cfar//streamit//CFARtest.str:42
    N_rg = _param_N_rg; // ../benchmarks//cfar//streamit//CFARtest.str:41
    N_cfar = _param_N_cfar; // ../benchmarks//cfar//streamit//CFARtest.str:41
    G = _param_G; // ../benchmarks//cfar//streamit//CFARtest.str:41
    setIOTypes(Float.TYPE, Float.TYPE); // ../benchmarks//cfar//streamit//CFARtest.str:37
    addSteadyPhase(((N_cfar + G) + 1), 1, 1, "work"); // ../benchmarks//cfar//streamit//CFARtest.str:45
    pos = 0; // ../benchmarks//cfar//streamit//CFARtest.str:43
  }
}
class ComplexSource extends Filter // ../benchmarks//cfar//streamit//CFARtest.str:77
{
  public ComplexSource(int _param_N)
  {
    setStateful(true);
  }
  float theta; // ../benchmarks//cfar//streamit//CFARtest.str:79
  int N; // ../benchmarks//cfar//streamit//CFARtest.str:77
  public void work() { // ../benchmarks//cfar//streamit//CFARtest.str:85
    for (int i = 0; (i < N); i++) { // ../benchmarks//cfar//streamit//CFARtest.str:87
      theta += (3.141592653589793f / 16); // ../benchmarks//cfar//streamit//CFARtest.str:88
      Complex c; // ../benchmarks//cfar//streamit//CFARtest.str:89
      c = new Complex(); // ../benchmarks//cfar//streamit//CFARtest.str:89
      c.real = (((float)Math.sin(theta) * ((float)Math.cos(theta) + ((0.0f * (float)Math.sin(theta)) - (1 * 0.0f)))) - (0.0f * ((0.0f * 0.0f) + (1 * (float)Math.sin(theta))))); // ../benchmarks//cfar//streamit//CFARtest.str:89
      c.imag = (((float)Math.sin(theta) * ((0.0f * 0.0f) + (1 * (float)Math.sin(theta)))) + (0.0f * ((float)Math.cos(theta) + ((0.0f * (float)Math.sin(theta)) - (1 * 0.0f))))); // ../benchmarks//cfar//streamit//CFARtest.str:89
      outputChannel.push(c); // ../benchmarks//cfar//streamit//CFARtest.str:91
    }; // ../benchmarks//cfar//streamit//CFARtest.str:86
  }
  public void init(final int _param_N) { // ../benchmarks//cfar//streamit//CFARtest.str:81
    N = _param_N; // ../benchmarks//cfar//streamit//CFARtest.str:80
    setIOTypes(Void.TYPE, Complex.class); // ../benchmarks//cfar//streamit//CFARtest.str:77
    addSteadyPhase(0, 0, N, "work"); // ../benchmarks//cfar//streamit//CFARtest.str:84
    theta = 0; // ../benchmarks//cfar//streamit//CFARtest.str:82
  }
}
class AnonFilter_a0 extends Filter // ../benchmarks//cfar//streamit//CFARtest.str:72
{
  public AnonFilter_a0()
  {
  }
  public void work() { // ../benchmarks//cfar//streamit//CFARtest.str:72
    System.out.println(inputChannel.popFloat()); // ../benchmarks//cfar//streamit//CFARtest.str:72
  }
  public void init() { // ../benchmarks//cfar//streamit//CFARtest.str:72
    setIOTypes(Float.TYPE, Void.TYPE); // ../benchmarks//cfar//streamit//CFARtest.str:72
    addSteadyPhase(1, 1, 0, "work"); // ../benchmarks//cfar//streamit//CFARtest.str:72
  }
}
public class CFARtest extends StreamItPipeline // ../benchmarks//cfar//streamit//CFARtest.str:68
{
  public void init() { // ../benchmarks//cfar//streamit//CFARtest.str:69
    add(new ComplexSource(64)); // ../benchmarks//cfar//streamit//CFARtest.str:70
    add(new CFAR(64, 5, 4)); // ../benchmarks//cfar//streamit//CFARtest.str:71
    add(new AnonFilter_a0()); // ../benchmarks//cfar//streamit//CFARtest.str:72
  }
}
