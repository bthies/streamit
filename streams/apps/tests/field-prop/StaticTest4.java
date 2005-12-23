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
class vlc_table_entry extends Structure implements Serializable {
  int code;
  int len;
  int value;
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
class source extends Filter // StaticTest4.str:7
{
  public source()
  {
  }
  int i; // StaticTest4.str:8
  public void work() { // StaticTest4.str:9
    output.pushInt(i++); // StaticTest4.str:10
  }
  public void init() { // StaticTest4.str:7
    i = 0; // StaticTest4.str:8
    setIOTypes(Void.TYPE, Integer.TYPE); // StaticTest4.str:7
    addSteadyPhase(0, 0, 1, "work"); // StaticTest4.str:9
  }
}
class doit extends Filter // StaticTest4.str:14
{
  public doit(int _param_pushrate, int _param_poprate)
  {
  }
  int i; // StaticTest4.str:15
  int pushrate; // StaticTest4.str:14
  int poprate; // StaticTest4.str:14
  public void work() { // StaticTest4.str:16
    i = 0; // StaticTest4.str:17
    int j; // StaticTest4.str:18
    for (j = 0; (j < poprate); j++) { // StaticTest4.str:19
      i += (input.popInt() + TheGlobal.blocks_per_macroblock[pushrate]); // StaticTest4.str:20
    }; // StaticTest4.str:19
    int k; // StaticTest4.str:22
    for (k = 0; (k < pushrate); k++) { // StaticTest4.str:23
      output.pushInt(i); // StaticTest4.str:24
    }; // StaticTest4.str:23
  }
  public void init(final int _param_pushrate, final int _param_poprate) { // StaticTest4.str:14
    pushrate = _param_pushrate; // StaticTest4.str:14
    poprate = _param_poprate; // StaticTest4.str:14
    setIOTypes(Integer.TYPE, Integer.TYPE); // StaticTest4.str:14
    addSteadyPhase(poprate, poprate, pushrate, "work"); // StaticTest4.str:16
  }
}
class sink extends Filter // StaticTest4.str:29
{
  public sink()
  {
  }
  public void work() { // StaticTest4.str:30
    System.out.println(input.popInt()); // StaticTest4.str:31
  }
  public void init() { // StaticTest4.str:29
    setIOTypes(Integer.TYPE, Void.TYPE); // StaticTest4.str:29
    addSteadyPhase(1, 1, 0, "work"); // StaticTest4.str:30
  }
}
class TheGlobal extends Global // StaticTest4.str:41
{
  public static int[4] blocks_per_macroblock; // StaticTest4.str:42
  public static int[4] bpmb2 = {0,6,8,12}; // StaticTest4.str:43
  public static vlc_table_entry[3] const_macroblock_address_inc; // StaticTest4.str:44
  public static int const_macroblock_address_inc_len; // StaticTest4.str:45
  public static int n; // StaticTest4.str:47
  public static int m; // StaticTest4.str:48
  public void init() { // StaticTest4.str:49
    blocks_per_macroblock = new int[4]; // StaticTest4.str:42
    const_macroblock_address_inc = new vlc_table_entry[3]; // StaticTest4.str:44
    for (int __sa0 = 0; (__sa0 < 3); __sa0++) { // StaticTest4.str:44
      const_macroblock_address_inc[__sa0] = new vlc_table_entry(); // StaticTest4.str:44
    }; // StaticTest4.str:44
    const_macroblock_address_inc_len = 3; // StaticTest4.str:45
    n = 3; // StaticTest4.str:47
    m = 2; // StaticTest4.str:50
    blocks_per_macroblock[0] = 0; // StaticTest4.str:52
    blocks_per_macroblock[1] = 6; // StaticTest4.str:53
    blocks_per_macroblock[2] = 8; // StaticTest4.str:54
    blocks_per_macroblock[3] = 12; // StaticTest4.str:55
    const_macroblock_address_inc[0].code = 1; // StaticTest4.str:56
    const_macroblock_address_inc[0].len = 1; // StaticTest4.str:57
    const_macroblock_address_inc[0].value = 1; // StaticTest4.str:58
    const_macroblock_address_inc[1].code = 3; // StaticTest4.str:59
    const_macroblock_address_inc[1].len = 3; // StaticTest4.str:60
    const_macroblock_address_inc[1].value = 2; // StaticTest4.str:61
    const_macroblock_address_inc[2].code = 2; // StaticTest4.str:62
    const_macroblock_address_inc[2].len = 3; // StaticTest4.str:63
    const_macroblock_address_inc[2].value = 3; // StaticTest4.str:64
  }
}
public class StaticTest4 extends StreamItPipeline // StaticTest4.str:1
{
  public void init() { // StaticTest4.str:1
    add(new source()); // StaticTest4.str:2
    add(new doit(TheGlobal.m, TheGlobal.n)); // StaticTest4.str:3
    add(new sink()); // StaticTest4.str:4
  }
}
