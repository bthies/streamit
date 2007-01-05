import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
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
class BitStream2IntStream extends Filter // VideoProcessor.str:95
{
  public BitStream2IntStream()
  {
  }
  public void work() { // VideoProcessor.str:96
    int some_int = 0; // VideoProcessor.str:97
    int two_power = 1; // VideoProcessor.str:98
    System.out.println("printing bits"); // VideoProcessor.str:99
    for (int i = 0; (i < 8); i++) { // VideoProcessor.str:100
      int add_int; // VideoProcessor.str:102
      add_int = (inputChannel.peekInt((8 - i)) * two_power); // VideoProcessor.str:102
      two_power = (two_power * 2); // VideoProcessor.str:103
      some_int += add_int; // VideoProcessor.str:104
      System.out.print(inputChannel.peekInt(i)); // VideoProcessor.str:105
    }; // VideoProcessor.str:100
    System.out.println("now to int value"); // VideoProcessor.str:107
    outputChannel.pushInt(some_int); // VideoProcessor.str:108
  }
  public void init() { // VideoProcessor.str:95
    setIOTypes(Integer.TYPE, Integer.TYPE); // VideoProcessor.str:95
    addSteadyPhase(8, 8, 1, "work"); // VideoProcessor.str:96
  }
}
class IntStreamReorder extends Filter // VideoProcessor.str:115
{
  public IntStreamReorder()
  {
  }
  public void work() { // VideoProcessor.str:116
    int some_int = inputChannel.popInt(); // VideoProcessor.str:117
    int b0 = ((some_int >> 24) & 255); // VideoProcessor.str:118
    int b1 = ((some_int & 16711680) >> 8); // VideoProcessor.str:119
    int b2 = ((some_int & 65280) << 8); // VideoProcessor.str:120
    int b3 = ((some_int & 255) << 24); // VideoProcessor.str:121
    some_int = (((b0 | b1) | b2) | b3); // VideoProcessor.str:122
    outputChannel.pushInt(some_int); // VideoProcessor.str:123
  }
  public void init() { // VideoProcessor.str:115
    setIOTypes(Integer.TYPE, Integer.TYPE); // VideoProcessor.str:115
    addSteadyPhase(1, 1, 1, "work"); // VideoProcessor.str:116
  }
}
class MacroblockMaker extends Filter // VideoProcessor.str:143
{
  public MacroblockMaker(int _param_width, int _param_height, int _param_numPictures)
  {
  }
  int width; // VideoProcessor.str:143
  int height; // VideoProcessor.str:143
  int numPictures; // VideoProcessor.str:143
  public void work() { // VideoProcessor.str:152
    System.out.println(inputChannel.popInt()); // VideoProcessor.str:154
  }
  public void init(final int _param_width, final int _param_height, final int _param_numPictures) { // VideoProcessor.str:143
    width = _param_width; // VideoProcessor.str:143
    height = _param_height; // VideoProcessor.str:143
    numPictures = _param_numPictures; // VideoProcessor.str:143
    setIOTypes(Integer.TYPE, Void.TYPE); // VideoProcessor.str:143
    addSteadyPhase(1, 1, 0, "work"); // VideoProcessor.str:152
  }
}
public class VideoProcessor extends StreamItPipeline // VideoProcessor.str:35
{
  public void init() { // VideoProcessor.str:35
    int width = 352; // VideoProcessor.str:38
    int height = 288; // VideoProcessor.str:39
    int numPictures = 300; // VideoProcessor.str:40
    add(new FileReader("../testvideos/news.qcif", Bit.TYPE, Bit.TREAT_AS_BITS)); // VideoProcessor.str:44
    add(new BitStream2IntStream()); // VideoProcessor.str:45
    add(new MacroblockMaker(width, height, numPictures)); // VideoProcessor.str:46
  }
}
