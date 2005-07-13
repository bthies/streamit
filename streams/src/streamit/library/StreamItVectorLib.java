/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library;

public class StreamItVectorLib
{
    public static float2 add2(float2 a, float2 b){
        float2 result = new float2();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        return result;
    }
    
    public static float3 add3(float3 a, float3 b){
        float3 result = new float3();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;
        return result;
    }
    
    public static float4 add4(float4 a, float4 b){
        float4 result = new float4();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;
        result.w = a.w + b.w;
        return result;
    }
    
    public static float2 sub2(float2 a, float2 b){
        float2 result = new float2();
        result.x = a.x - b.x;
        result.y = a.y - b.y;
        return result;
    }
    
    public static float3 sub3(float3 a, float3 b){
        float3 result = new float3();
        result.x = a.x - b.x;
        result.y = a.y - b.y;
        result.z = a.z - b.z;
        return result;
    }
    
    public static float4 sub4(float4 a, float4 b){
        float4 result = new float4();
        result.x = a.x - b.x;
        result.y = a.y - b.y;
        result.z = a.z - b.z;
        result.w = a.w - b.w;
        return result;
    }
    
    public static float2 mul2(float2 a, float2 b){
        float2 result = new float2();
        result.x = a.x * b.x;
        result.y = a.y * b.y;
        return result;
    }
    
    public static float3 mul3(float3 a, float3 b){
        float3 result = new float3();
        result.x = a.x * b.x;
        result.y = a.y * b.y;
        result.z = a.z * b.z;
        return result;
    }
    
    public static float4 mul4(float4 a, float4 b){
        float4 result = new float4();
        result.x = a.x * b.x;
        result.y = a.y * b.y;
        result.z = a.z * b.z;
        result.w = a.w * b.w;
        return result;
    }
    
    public static float2 div2(float2 a, float2 b){
        float2 result = new float2();
        result.x = a.x / b.x;
        result.y = a.y / b.y;
        return result;
    }
    
    public static float3 div3(float3 a, float3 b){
        float3 result = new float3();
        result.x = a.x / b.x;
        result.y = a.y / b.y;
        result.z = a.z / b.z;
        return result;
    }
    
    public static float4 div4(float4 a, float4 b){
        float4 result = new float4();
        result.x = a.x / b.x;
        result.y = a.y / b.y;
        result.z = a.z / b.z;
        result.w = a.w / b.w;
        return result;
    }
    public static float2 neg2(float2 a){
        float2 result = new float2();
        result.x = -a.x;
        result.y = -a.y;
        return result;
    }
    
    public static float3 neg3(float3 a){
        float3 result = new float3();
        result.x = -a.x;
        result.y = -a.y;
        result.z = -a.z;
        return result;
    }
    
    public static float4 neg4(float4 a){
        float4 result = new float4();
        result.x = -a.x;
        result.y = -a.y;
        result.z = -a.z;
        result.w = -a.w;
        return result;
    }
    
    public static float2 floor2(float2 a){
        float2 result = new float2();
        result.x = (float)Math.floor(a.x);
        result.y = (float)Math.floor(a.y);
        return result;
    }
    
    public static float3 floor3(float3 a){
        float3 result = new float3();
        result.x = (float)Math.floor(a.x);
        result.y = (float)Math.floor(a.y);
        result.z = (float)Math.floor(a.z);
        return result;
    }
    
    public static float4 floor4(float4 a){
        float4 result = new float4();
        result.x = (float)Math.floor(a.x);
        result.y = (float)Math.floor(a.y);
        result.z = (float)Math.floor(a.z);
        result.w = (float)Math.floor(a.w);
        return result;
    }
    
    public static float2 normalize2(float2 a){
        float t = (float)Math.sqrt(a.x*a.x + a.y*a.y);
        float2 result = new float2();
        result.x = a.x/t;
        result.y = a.y/t;
        return result;
    }
    
    public static float3 normalize3(float3 a){
        float t = (float)Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z);
        float3 result = new float3();
        result.x = a.x/t;
        result.y = a.y/t;
        result.z = a.z/t;
        return result;
    }
    
    public static float4 normalize4(float4 a){
        float t = (float)Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z + a.w*a.w);
        float4 result = new float4();
        result.x = a.x/t;
        result.y = a.y/t;
        result.z = a.z/t;
        result.w = a.w/t;
        return result;
    }
    
    public static float2 addScalar2(float2 a, float sc){
        float2 result = new float2();
        result.x = a.x + sc;
        result.y = a.y + sc;
        return result;
    }
    
    public static float3 addScalar3(float3 a, float sc){
        float3 result = new float3();
        result.x = a.x + sc;
        result.y = a.y + sc;
        result.z = a.z + sc;
        return result;
    }
    
    public static float4 addScalar4(float4 a, float sc){
        float4 result = new float4();
        result.x = a.x + sc;
        result.y = a.y + sc;
        result.z = a.z + sc;
        result.w = a.w + sc;
        return result;
    }

    public static float2 subScalar2(float2 a, float sc){
        float2 result = new float2();
        result.x = a.x - sc;
        result.y = a.y - sc;
        return result;
    }
    
    public static float3 subScalar3(float3 a, float sc){
        float3 result = new float3();
        result.x = a.x - sc;
        result.y = a.y - sc;
        result.z = a.z - sc;
        return result;
    }
    
    public static float4 subScalar4(float4 a, float sc){
        float4 result = new float4();
        result.x = a.x - sc;
        result.y = a.y - sc;
        result.z = a.z - sc;
        result.w = a.w - sc;
        return result;
    }
    
    public static float2 scale2(float2 a, float sc){
        float2 result = new float2();
        result.x = a.x * sc;
        result.y = a.y * sc;
        return result;
    }
    
    public static float3 scale3(float3 a, float sc){
        float3 result = new float3();
        result.x = a.x * sc;
        result.y = a.y * sc;
        result.z = a.z * sc;
        return result;
    }
    
    public static float4 scale4(float4 a, float sc){
        float4 result = new float4();
        result.x = a.x * sc;
        result.y = a.y * sc;
        result.z = a.z * sc;
        result.w = a.w * sc;
        return result;
    }
    
    public static float2 scaleInv2(float2 a, float sc){
        float2 result = new float2();
        result.x = a.x / sc;
        result.y = a.y / sc;
        return result;
    }
    
    public static float3 scaleInv3(float3 a, float sc){
        float3 result = new float3();
        result.x = a.x / sc;
        result.y = a.y / sc;
        result.z = a.z / sc;
        return result;
    }
    
    public static float4 scaleInv4(float4 a, float sc){
        float4 result = new float4();
        result.x = a.x / sc;
        result.y = a.y / sc;
        result.z = a.z / sc;
        result.w = a.w / sc;
        return result;
    }
    
    public static float sqrtDist2(float2 a, float2 b){
        float x1 = a.x - b.x;
        float y1 = a.y - b.y;
        return (float)Math.sqrt(x1*x1 + y1*y1);
    }
    
    public static float sqrtDist3(float3 a, float3 b){
        float x1 = a.x - b.x;
        float y1 = a.y - b.y;
        float z1 = a.z - b.z;
        return (float)Math.sqrt(x1*x1 + y1*y1 + z1*z1);
    }
    
    public static float sqrtDist3(float4 a, float4 b){
        float x1 = a.x - b.x;
        float y1 = a.y - b.y;
        float z1 = a.z - b.z;
        float w1 = a.w - b.w;
        return (float)Math.sqrt(x1*x1 + y1*y1 + z1*z1 + w1*w1);
    }
    
    public static float3 cross3(float3 a, float3 b){
        float3 result = new float3();
        result.x = a.y * b.z - b.y * a.z;
        result.y = a.z * b.x - b.z * a.x;
        result.z = a.x * b.y - b.x * a.y;
        return result;
    }
    
    public static float dot3(float3 a, float3 b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
    }
    
    public static float2 max2(float2 a, float2 b) {
        float2 result = new float2();
        result.x = (b.x < a.x) ? a.x : b.x;
        result.y = (b.y < a.y) ? a.y : b.y;
        return result;
    }
    
    public static float3 max3(float3 a, float3 b) {
        float3 result = new float3();
        result.x = (b.x < a.x) ? a.x : b.x;
        result.y = (b.y < a.y) ? a.y : b.y;
        result.z = (b.z < a.z) ? a.z : b.z;
        return result;
    }
    
    public static float2 min2(float2 a, float2 b) {
        float2 result = new float2();
        result.x = (b.x > a.x) ? a.x : b.x;
        result.y = (b.y > a.y) ? a.y : b.y;
        return result;
    } 
    
    public static float3 min3(float3 a, float3 b) {
        float3 result = new float3();
        result.x = (b.x > a.x) ? a.x : b.x;
        result.y = (b.y > a.y) ? a.y : b.y;
        result.z = (b.z > a.z) ? a.z : b.z;
        return result;
    }
    
    public static boolean greaterThan3(float3 a, float3 b) {
        if (b.x < a.x) return true;
        if (b.y < a.y) return true;
        if (b.z < a.z) return true;
        return false;
    }
    
    public static boolean lessThan3(float3 a, float3 b) {
        if (b.x > a.x) return true;
        if (b.y > a.y) return true;
        if (b.z > a.z) return true;
        return false;
    }
    
    public static boolean equals3(float3 a, float3 b) {
        if (b.x != a.x) return false;
        if (b.y != a.y) return false;
        if (b.z != a.z) return false;
        return true;
    }

}
