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

import java.lang.reflect.*;
import java.util.*;

import streamit.misc.DestroyedClass;

// an operator takes N inputs and produces N outputs.
// Never explicitly instantiated
public class Operator extends DestroyedClass
{
    ParameterContainer initParams;
    boolean initialized = false;

    public Operator(float x1, float y1, int z1)
    {
        initParams = new ParameterContainer ("float-float-int")
            .add("x1", x1)
            .add("y1", y1)
            .add("z1", z1);
    }

    public Operator(int a, float b)
    {
        initParams = new ParameterContainer("int-float")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float[] a)
    {
        initParams = new ParameterContainer("float[]")
            .add("a", copyFloatArray1D(a));
    }

    public Operator(int a, float[] b)
    {
        initParams = new ParameterContainer("int-float[]")
            .add("a", a)
            .add("b", copyFloatArray1D(b));
    }

    public Operator(int a, int[] b)
    {
        initParams = new ParameterContainer("int-int[]")
            .add("a", a)
            .add("b", copyIntArray1D(b));
    }

    public Operator(int a, float[][] b)
    {
        initParams = new ParameterContainer("int-float[][]")
            .add("a", a)
            .add("b", copyFloatArray2D(b));
    }

    public Operator(int i1, int i2, float f)
    {
        initParams = new ParameterContainer("int-int-float")
            .add("i1", i1)
            .add("i2", i2)
            .add("f", f);
    }

    public Operator(boolean b1) 
    {
        initParams = new ParameterContainer("boolean")
            .add("b1", b1);
    }
  
    public Operator(int i1, boolean b1) 
    {
        initParams = new ParameterContainer("int-boolean")
            .add("i1", i1)
            .add("b1", b1);
    }

    public Operator(int i1, int i2, boolean b1) 
    {
        initParams = new ParameterContainer("int-int-boolean")
            .add("i1", i1)
            .add("i2", i2)
            .add("b1", b1);
    }

    public Operator(int a, int b, float[] c)
    {
        initParams = new ParameterContainer("int-int-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray1D(c));
    }

    public Operator(int a, int b, int c, float[] d)
    {
        initParams = new ParameterContainer("int-int-int-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", copyFloatArray1D(d));
    }

    public Operator (int a, float[] c, float[] d) 
    { 
        initParams = new ParameterContainer("int-float[]-float[]")
            .add("a",a)
            .add("c",copyFloatArray1D(c))
            .add("d",copyFloatArray1D(d));
    }

    public Operator(int a, int b, float[][] c)
    {
        initParams = new ParameterContainer("int-int-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray2D(c));
    }

    public Operator(int i1, int i2, float f1, float f2)
    {
        initParams = new ParameterContainer("int-int-float-float")
            .add("i1", i1).add("i2", i2).add("f1", f1).add("f2", f2);
    }
 
    public Operator(int i1, int i2, float f1, float f2, float f3) {
        initParams = new ParameterContainer("int-int-float-float-float")
            .add("i1", i1).add("i2", i2).add("f1", f1).add("f2", f2).add("f3", f3);
    }

   public Operator(int i1, int i2, int i3, int i4, int i5, float f)
    {
	initParams = new ParameterContainer("int-int-int-int-int-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("i4",i4)
            .add("i5",i5)
            .add("f",f);
    }

    public Operator(int i1, int i2, int i3, int i4, int i5, int i6, float f1, float f2)
    {
	initParams = new ParameterContainer("int-int-int-int-int-int-float-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("i4",i4)
            .add("i5",i5)
            .add("i6",i6)
            .add("f1",f1)
            .add("f2",f2);
    }

    public Operator(int i1, int i2, int i3, int i4, int i5, int i6, int i7, float f1, float f2)
    {
	initParams = new ParameterContainer("int-int-int-int-int-int-int-float-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("i4",i4)
            .add("i5",i5)
            .add("i6",i6)
            .add("i7",i7)
            .add("f1",f1)
            .add("f2",f2);
    }

    public Operator(int a, int b, int c, int d, float[][] e)
    {
        initParams = new ParameterContainer("int-int-int-int-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", copyFloatArray2D(e));
    }

    public Operator(int a, int b, float[][] c, float[] d) {
        initParams = new ParameterContainer("int-int-float[][]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray2D(c))
            .add("d", copyFloatArray1D(d));
    }

    public Operator(int a, int b, int c, float[][] d, float[] e) {
        initParams = new ParameterContainer("int-int-int-float[][]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", copyFloatArray2D(d))
            .add("e", copyFloatArray1D(e));
    }

    public Operator(int a, boolean b, float c, float d, float[][] e, float[] f) {
        initParams = new ParameterContainer("int-boolean-float-float-float[][]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", copyFloatArray2D(e))
            .add("f", copyFloatArray1D(f));
    }

    public Operator(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        initParams = new ParameterContainer("int-int-int-int-float[][]-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", copyFloatArray2D(e))
            .add("f", copyFloatArray2D(f));
    }

    public Operator(int a, int b, int c, float[][] x, float[][] y)
    {
        initParams = new ParameterContainer("int-int-int-float[][]-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("x", copyFloatArray2D(x))
            .add("y", copyFloatArray2D(y));
    }

    public Operator(float a, int b)
    {
        initParams = new ParameterContainer("float-int")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float a, float b)
    {
        initParams = new ParameterContainer("float-float")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float a, float b, float c)
    {
        initParams = new ParameterContainer("float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c);
    }

    public Operator(float a, float b, float c, int d, int e)
    {
        initParams = new ParameterContainer("float-float-float-int-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
	    .add("d", d)
	    .add("e", e);
    }

    public Operator(float a, float b, float c, int d, int e, int f)
    {
        initParams = new ParameterContainer("float-float-float-int-int-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
	    .add("d", d)
	    .add("e", e)
	    .add("f", f);
    }

    public Operator(float a, float b, float c, float d, int e, int f)
    {
        initParams = new ParameterContainer("float-float-float-float-int-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
	    .add("d", d)
	    .add("e", e)
	    .add("f", f);
    }

    public Operator(float a, float b, float c, float d)
    {
        initParams = new ParameterContainer("float-float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d);
    }

    public Operator(float a, float b, float c, float d, float e, float f, float g) {
        initParams = new ParameterContainer("float-float-float-float-float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e)
            .add("f", f)
            .add("g", g);
    }

    public Operator(float x3, float y3, int z3, int a3)
    {
        initParams = new ParameterContainer ("float-float-int-int")
            .add("x3", x3)
            .add("y3", y3)
            .add("z3", z3)
            .add("a3", a3);
    }

    public Operator(float x3, float y3, int z3, int a3, int b3)
    {
        initParams = new ParameterContainer ("float-float-int-int-int")
            .add("x3", x3)
            .add("y3", y3)
            .add("z3", z3)
            .add("a3", a3)
            .add("b3", b3);
    }

    public Operator(float f1, float f2, int i1, int i2, int i3, int i4)
    {
        initParams = new ParameterContainer("float-float-int-int-int-int")
            .add("f1", f1)
            .add("f2", f2)
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4);
    }

    public Operator(float f1, float f2, int i1, int i2, int i3, int i4, int i5)
    {
        initParams = new ParameterContainer("float-float-int-int-int-int-int")
            .add("f1", f1)
            .add("f2", f2)
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4)
            .add("i5", i5);
    }

    public Operator(float f1, int i1, float[] f2, float[] f3, int i2) {
	initParams = new ParameterContainer("float-int-float[]-float[]-int")
	    .add("f1", f1)
	    .add("i1", i1)
	    .add("f2", copyFloatArray1D(f2))
	    .add("f3", copyFloatArray1D(f3))
	    .add("i2", i2);
    }

    public Operator (int i1, int i2, int i3,
		   int i4, int i5, int i6, int i7, int i8, 
		   int i9, int i10, float f)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-int-int-int-float")
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4)
            .add("i5", i5)
            .add("i6", i6)
            .add("i7", i7)
            .add("i8", i8)
            .add("i9", i9)
            .add("i10", i10)
            .add("f", f);
    }

   public Operator (int i1, int i2, int i3, int i4, 
                    int i5, int i6, int i7, int i8, int i9)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-int-int")
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4)
            .add("i5", i5)
            .add("i6", i6)
            .add("i7", i7)
            .add("i8", i8)
            .add("i9", i9);
    }

    public Operator(int a, int b, float c, int d, float e)
    {
        initParams = new ParameterContainer ("int-int-float-int-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(int a, int b, int c, float d, float e)
    {
        initParams = new ParameterContainer ("int-int-int-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(int a, int b, int c, float[][] d)
    {
        initParams = new ParameterContainer ("int-int-int-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", copyFloatArray2D(d));
    }

    public Operator(int a, int b, int c, float d, int e)
    {
        initParams = new ParameterContainer ("int-int-int-float-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(float x2, float y2, float z2, int a2, float b2)
    {
        initParams = new ParameterContainer ("float-float-float-int-float")
            .add("x2", x2)
            .add("y2", y2)
            .add("z2", z2)
            .add("a2", a2)
            .add("b2", b2);
    }

    public Operator(float x2, float y2, float z2, int a2)
    {
        initParams = new ParameterContainer ("float-float-float-int")
            .add("x2", x2)
            .add("y2", y2)
            .add("z2", z2)
            .add("a2", a2);
    }

    public Operator(int i1,int i2,int i3,float f1) {
	initParams = new ParameterContainer("int-int-int-float")
	    .add("i1",i1)
	    .add("i2",i2)
	    .add("i3",i3)
	    .add("f1",f1);
    }

    public Operator()
    {
        initParams = new ParameterContainer ("");
    }

    public Operator(int n)
    {
        initParams = new ParameterContainer ("int").add ("n", n);
    }

    public Operator(char c)
    {
        initParams = new ParameterContainer ("char").add ("c", c);
    }

    public Operator (int x, int y)
    {
        initParams = new ParameterContainer ("int-int").add ("x", x).add ("y", y);
    }

    public Operator (int x, int y, int z)
    {
        initParams = new ParameterContainer ("int-int-int").add ("x", x).add ("y", y).add("z", z);
    }

    public Operator (int x, int y, int z, int a)
    {
        initParams = new ParameterContainer ("int-int-int-int").add ("x", x).add ("y", y).add("z", z).add ("a", a);
    }

    public Operator (int a, int b, int c,
                     int d, int e)
    {
        initParams = new ParameterContainer ("int-int-int-int-int").add ("a", a).add ("b", b).add("c", c).add ("d", d).add ("e", e);
    }

    public Operator (int x, int y, int z,
                     int a, float b)
    {
        initParams = new ParameterContainer ("int-int-int-int-float").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b);
    }

    public Operator (int x, int y, int z,
                     int a, int b, int c)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b).add("c", c);
    }

    public Operator (int i1, int i2, int i3,
                     int i4, int i5, int i6, int i7)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int") .add("i1", i1) .add("i2", i2). add("i3", i3) .add("i4", i4) .add("i5", i5) .add("i6", i6) .add("i7",i7);
    }


    public Operator (int x, int y, int z,
                     int a, int b, int c, int d, float f)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-float").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b).add("c", c).add("d",d).add("f",f);
    }

    public Operator(float f)
    {
        initParams = new ParameterContainer ("float").add ("f", f);
    }

    public Operator(String str)
    {
        initParams = new ParameterContainer ("String").add ("str", str);
    }

    public Operator(ParameterContainer params)
    {
        initParams = new ParameterContainer ("ParameterContainer").add ("params", params);
    }

    public Operator(int i1,
		    int i2, 
		    int i3, 
		    int i4, 
		    int i5, 
		    int i6, 
		    int i7, 
		    int i8, 
		    int i9, 
		    float f) {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-int-int-float").add ("i1", i1).add ("i2", i2).add ("i3", i3).add ("i4", i4).add ("i5", i5).add ("i6", i6).add ("i7", i7).add ("i8", i8).add ("i9", i9).add ("f", f);
	    }


    public Operator(int i1,
		    int i2, 
		    int i3, 
		    int i4, 
		    int i5, 
		    int i6, 
		    float f) {
        initParams = new ParameterContainer ("int-int-int-int-int-int-float").add ("i1", i1).add ("i2", i2).add ("i3", i3).add ("i4", i4).add ("i5", i5).add ("i6", i6).add ("f", f);
    }

    public Operator(int a, int b, float[] c, float[] d)
    {
        initParams = new ParameterContainer("int-int-float[]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray1D(c))
            .add("d", copyFloatArray1D(d));
    }

    public Operator(short s1, short s2, short s3) {
	initParams = new ParameterContainer("short-short-short")
            .add("s1", s1)
            .add("s2", s2)
            .add("s3", s3);
    }

    public Operator(Object o1) {
	initParams = new ParameterContainer("Object")
	    .add("o1", o1);
    }

    public Operator(Object o1, int i1) {
        initParams = new ParameterContainer("Object-int")
            .add("o1", o1)
            .add("i1", i1);
    }

    public Operator(int i1, int i2, Object o1) {
        initParams = new ParameterContainer("int-int-Object")
            .add("i1", i1)
            .add("i2", i2)
            .add("o1", o1);
    }

    public Operator(Object o1, Object o2) {
	initParams = new ParameterContainer("Object-Object")
	    .add("o1", o1)
	    .add("o2", o2);
    }

    public Operator(Object o1, Object o2, Object o3) {
	initParams = new ParameterContainer("Object-Object-Object")
	    .add("o1", o1)
	    .add("o2", o2)
	    .add("o3", o3);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------

    void invalidInitError ()
    {
        ERROR ("You didn't provide a valid init function in class " + getClass ().getName () + ".\nFilters now need init functions, even if they're empty.\nPerhaps you're passing parameters that don't have a valid prototype yet?\nCheck streams/docs/implementation-notes/library-init-functions.txt for instructions on adding new signatures to init functions.\n" + 
                "The init string you passed is: (" + initParams.getParamName() + ")." );
    }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, float z, int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float[] x) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, float z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int a, int b, int c, int d)
    { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int a, int b, int c, int d, int e)
    { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int a, int b, int c)
    { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int z) { invalidInitError (); }

    public void init(float a, float b, float c) { invalidInitError(); }

    public void init(float a, float b, float c, float d) { invalidInitError(); }

    public void init(float a, float b, float c, float d, float e, float f, float g) { invalidInitError(); }

    public void init(float a, float b, float c, int d, int e) { invalidInitError(); }

    public void init(float a, float b, float c, int d, int e, int f) { invalidInitError(); }

    public void init(float a, float b, float c, float d, int e, int f) { invalidInitError(); }

    public void init(float a, int b, float[] c, float[] d, int e) { invalidInitError(); }

    // initializatoin functions, to be over-ridden
    public void init(boolean b1) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, boolean b1) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, boolean b1) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, float f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, float f1, float f2) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, float f1, float f2, float f3) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float[][] c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, float b[], float c[]) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float[][] c, float[] d) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[][] d, float[] e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, boolean b, float c, float d, float[][] e, float[] f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[] d) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[][] d) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, float[][] e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, float[][] e, float[][] f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float c, int d, float e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, int i3, int i4, int i5, float f) { invalidInitError (); }

    public void init(int i1, int i2, int i3, int i4, int i5, int i6, float f1, float f2) { invalidInitError (); }

    public void init(int i1, int i2, int i3, int i4, int i5, int i6, int i7, float f1, float f2) { invalidInitError (); }

    public void init(int i1, int i2, int i3, int i4, int i5, int i6, int i7) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float d, float e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float d, int e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, int e, int f, int g, float h){ invalidInitError (); }

    public void init (int n1, int n2, int n3,
		   int n4, int n5, int n6, int n7, int n8, 
	int n9, int n10, float f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init() { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int n) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, float[] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int[] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, float[][] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, float[] z) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, float[] z1, float[] z2) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[][] x, float[][] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, int y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, float f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, int e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a, int b, int c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(short s1, short s2, short s3) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float f) { invalidInitError (); }

    // initialization functions, to be over-ridden
    public void init(char c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(String str) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init (int i1,
		      int i2, 
		      int i3, 
		      int i4, 
		      int i5, 
		      int i6, 
		      int i7, 
		      int i8, 
		      int i9, 
		      float f)
    { 
	invalidInitError (); 
    }

    // initialization functions, to be over-ridden
    public void init (int i1,
		      int i2, 
		      int i3, 
		      int i4, 
		      int i5, 
		      int i6, 
		      int i7, 
		      int i8, 
		      int i9)
    { 
	invalidInitError (); 
    }


    // initializatoin functions, to be over-ridden
    public void init (int i1,
		      int i2, 
		      int i3, 
		      int i4, 
		      int i5, 
		      int i6, 
		      float f)
    { 
	invalidInitError (); 
    }

    public void init(Object o1) {
	invalidInitError();
    }


    public void init(Object o1, int i1) {
        invalidInitError();
    }

    public void init(int i1, int i2, Object o) {
        invalidInitError();
    }

    public void init(Object o1, Object o2) {
	invalidInitError();
    }

    public void init(Object o1, Object o2, Object o3) {
	invalidInitError();
    }

    // initializatoin functions, to be over-ridden
    public void init(ParameterContainer params) { invalidInitError (); }

    public static MessageStub MESSAGE_STUB;

    // initialize the MESSAGE_STUB
    static {
        MESSAGE_STUB = MessageStub.STUB;
    }

    // allSinks is used for scheduling the streaming graph
    public static LinkedList allSinks;
    public static LinkedList allFilters;
    public static HashSet fullChannels;

    static {
        allSinks = new LinkedList ();
        allFilters = new LinkedList ();
        fullChannels = new HashSet ();
    }

    void addSink ()
    {
        allSinks.add (this);
    }

    void addFilter ()
    {
        allFilters.add (this);
    }

    void runSinks ()
    {
        ListIterator iter;

        if (!allSinks.isEmpty ())
            iter = allSinks.listIterator ();
        else
            iter = allFilters.listIterator ();

        // go over all the sinks
        while (iter.hasNext ())
        {
            Operator sink;
            sink = (Operator) iter.next ();
            assert sink != null;

            // do bunch of work
            int i;
            for (i = 0; i < 10; i++)
            {
                sink.work ();
            }
        }
    }

    void drainChannels ()
    {
        while (!fullChannels.isEmpty ())
        {
                // empty any full channels:
            Iterator fullChannel;
                fullChannel = fullChannels.iterator ();

            Channel ch = (Channel) fullChannel.next ();
            assert ch != null;

            ch.getSink ().work ();
             }
    }


    void addFullChannel (Channel channel)
    {
        fullChannels.add (channel);
    }

    void removeFullChannel (Channel channel)
        {
                fullChannels.remove (channel);
        }

    public static void passOneData (Channel from, Channel to)
    {
        Class type = from.getType ();
        assert type == to.getType ();

        if (type == Integer.TYPE)
        {
            to.pushInt (from.popInt ());
        } else
        if (type == Short.TYPE)
        {
            to.pushShort (from.popShort ());
        } else
        if (type == Character.TYPE)
        {
            to.pushChar (from.popChar ());
        } else
        if (type == Float.TYPE)
        {
            to.pushFloat (from.popFloat ());
        } else
        if (type == Double.TYPE)
        {
            to.pushDouble (from.popDouble ());
        } else {
            to.push (from.pop ());
        }
    }

    public static void duplicateOneData (Channel from, Channel [] to)
    {
        Class type = from.getType ();
        assert to != null && type == to[0].getType ();

        if (type == Integer.TYPE)
        {
            int data = from.popInt ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushInt (data);
            }
        } else
        if (type == Short.TYPE)
        {
            short data = from.popShort ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushShort (data);
            }
        } else
        if (type == Character.TYPE)
        {
            char data = from.popChar ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushChar (data);
            }
        } else
        if (type == Float.TYPE)
        {
            float data = from.popFloat ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushFloat (data);
            }
        } else
        if (type == Double.TYPE)
        {
            double data = from.popDouble ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .pushDouble (data);
            }
        } else {
            Object data = from.pop ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .push (data);
            }
        }
    }

    // send a message to a handler that returns <stub> within <delay>
    // units of my input/output (to be specified more clearly...)
    public void sendMessage(MessageStub stub, int delay) {}

    // send a message to a handler that returns <stub> at the earliest
    // convenient time.
    public void sendMessage(MessageStub stub) {}

    protected static class MessageStub
    {
        private static MessageStub STUB = new MessageStub();
        private MessageStub() {}
    }

    // a prototype work function
    void work () { }

    // this function will take care of all appropriate calls to init:
    void setupOperator ()
    {
        // don't re-initialize
        if (initialized) return;

        callInit();
        connectGraph ();
        initialized = true;
    }

    // this function calls the init function, based on state in
    // the object
    protected void callInit ()
    {
        assert initParams != null;

        if(initParams.getParamName().equals(("int-int-float-float")))
	    init (initParams.getIntParam("i1"),
		  initParams.getIntParam("i2"),
		  initParams.getFloatParam("f1"),
		  initParams.getFloatParam("f2"));
	else
        if(initParams.getParamName().equals(("int-int-float-float-float")))
	    init (initParams.getIntParam("i1"),
		  initParams.getIntParam("i2"),
		  initParams.getFloatParam("f1"),
		  initParams.getFloatParam("f2"),
		  initParams.getFloatParam("f3"));
	else
        if(initParams.getParamName().equals(("int-int-float")))
	    init (initParams.getIntParam("i1"),
		  initParams.getIntParam("i2"),
		  initParams.getFloatParam("f"));
	else
        if(initParams.getParamName().equals(("boolean")))
	    init (initParams.getBoolParam("b1"));
	else
        if(initParams.getParamName().equals(("int-boolean")))
	    init (initParams.getIntParam("i1"),
		  initParams.getBoolParam("b1"));
	else
        if(initParams.getParamName().equals(("int-int-boolean")))
	    init (initParams.getIntParam("i1"),
		  initParams.getIntParam("i2"),
		  initParams.getBoolParam("b1"));
	else
        if(initParams.getParamName().equals("int-int-int-int-float"))
            init (initParams.getIntParam("x"),
                  initParams.getIntParam("y"),
                  initParams.getIntParam("z"),
                  initParams.getIntParam("a"),
                  initParams.getFloatParam("b"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-float"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
                  initParams.getFloatParam("f"));
	else

       if(initParams.getParamName().equals("int-int-int-int-int-int-float-float"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
		  initParams.getIntParam("i6"),
                  initParams.getFloatParam("f1"),
		  initParams.getFloatParam("f2"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-int-float-float"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
		  initParams.getIntParam("i6"),
		  initParams.getIntParam("i7"),
                  initParams.getFloatParam("f1"),
		  initParams.getFloatParam("f2"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-int"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
                  initParams.getIntParam("i6"),
                  initParams.getIntParam("i7"));
	else
        if(initParams.getParamName().equals("int-int-int-int-float[][]-float[][]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getIntParam("d"),
                  (float[][])initParams.getObjParam("e"),
                  (float[][])initParams.getObjParam("f"));
        else
        if(initParams.getParamName().equals("int-int-int-int-int-int"))
            init (initParams.getIntParam("x"),
                  initParams. getIntParam("y"),
                  initParams.getIntParam("z"),
                  initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"));
        else
        if(initParams.getParamName().equals("float-float-float-int-int-int"))
            init (initParams.getFloatParam("a"),
                  initParams. getFloatParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getIntParam("d"),
                  initParams.getIntParam("e"),
                  initParams.getIntParam("f"));
        else
        if(initParams.getParamName().equals("float-float-float-int-int"))
            init (initParams.getFloatParam("a"),
                  initParams. getFloatParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getIntParam("d"),
                  initParams.getIntParam("e"));
        else
        if(initParams.getParamName().equals("int-float"))
            init (initParams.getIntParam("a"),
                  initParams.getFloatParam("b"));
        else
        if(initParams.getParamName().equals("int-float[]"))
            init (initParams.getIntParam("a"),
                  (float[])initParams.getObjParam("b"));
        else
        if(initParams.getParamName().equals("int-int[]"))
            init (initParams.getIntParam("a"),
                  (int[])initParams.getObjParam("b"));
        else
        if(initParams.getParamName().equals("float[]"))
            init ((float[])initParams.getObjParam("a"));
        else
        if(initParams.getParamName().equals("int-float[][]"))
            init (initParams.getIntParam("a"),
                  (float[][])initParams.getObjParam("b"));
        else
        if(initParams.getParamName().equals("float-int"))
            init (initParams.getFloatParam("a"),
                  initParams.getIntParam("b"));
        else
        if(initParams.getParamName().equals("float-float"))
            init (initParams.getFloatParam("a"),
                  initParams.getFloatParam("b"));
        else
        if(initParams.getParamName().equals("float-float-int"))
            init (initParams.getFloatParam("x1"),
                  initParams.getFloatParam("y1"),
                  initParams.getIntParam("z1"));
        else
        if(initParams.getParamName().equals("float-float-float-float"))
            init (initParams.getFloatParam("a"),
                  initParams.getFloatParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getFloatParam("d"));
        else
        if(initParams.getParamName().equals("float-float-float-float-float-float-float"))
            init (initParams.getFloatParam("a"),
                  initParams.getFloatParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getFloatParam("d"),
                  initParams.getFloatParam("e"),
                  initParams.getFloatParam("f"),
                  initParams.getFloatParam("g"));
        else
        if(initParams.getParamName().equals("float-float-float-float-int-int"))
            init (initParams.getFloatParam("a"),
                  initParams.getFloatParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getFloatParam("d"),
                  initParams.getIntParam("e"),
                  initParams.getIntParam("f"));
        else
        if(initParams.getParamName().equals("float-float-int-int"))
            init (initParams.getFloatParam("x3"),
                  initParams.getFloatParam("y3"),
                  initParams.getIntParam("z3"),
                  initParams.getIntParam("a3"));
        else
            if(initParams.getParamName().equals("float-float-int-int-int-int"))
                init(initParams.getFloatParam("f1"),
                     initParams.getFloatParam("f2"),
                     initParams.getIntParam("i1"),
                     initParams.getIntParam("i2"),
                     initParams.getIntParam("i3"),
                     initParams.getIntParam("i4"));
        else
            if(initParams.getParamName().equals("float-float-int-int-int-int-int"))
                init(initParams.getFloatParam("f1"),
                     initParams.getFloatParam("f2"),
                     initParams.getIntParam("i1"),
                     initParams.getIntParam("i2"),
                     initParams.getIntParam("i3"),
                     initParams.getIntParam("i4"),
                     initParams.getIntParam("i5"));
        else
            if(initParams.getParamName().equals("float-float-int-int-int"))
                init(initParams.getFloatParam("x3"),
                     initParams.getFloatParam("y3"),
                     initParams.getIntParam("z3"),
                     initParams.getIntParam("a3"),
                     initParams.getIntParam("b3"));
        else
        if(initParams.getParamName().equals("float-float-float-int-float"))
            init (initParams.getFloatParam("x2"),
                  initParams.getFloatParam("y2"),
                  initParams.getFloatParam("z2"),
                  initParams.getIntParam("a2"),
                  initParams.getFloatParam("b2"));
        else
        if(initParams.getParamName().equals("float-int-float[]-float[]-int"))
            init (initParams.getFloatParam("f1"),
                  initParams.getIntParam("i1"),
                  (float[])initParams.getObjParam("f2"),
                  (float[])initParams.getObjParam("f3"),
                  initParams.getIntParam("i2"));
        else
        if(initParams.getParamName().equals("int-int-float-int-float"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getFloatParam("c"),
                  initParams.getIntParam("d"),
                  initParams.getFloatParam("e"));
        else
        if(initParams.getParamName().equals("int-int-int-float-float"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getFloatParam("d"),
                  initParams.getFloatParam("e"));
        else
        if(initParams.getParamName().equals("int-int-int-float-int"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getFloatParam("d"),
                  initParams.getIntParam("e"));
        else
        if(initParams.getParamName().equals("int-int-int-int-int"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getIntParam("d"),
                  initParams.getIntParam("e"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-float"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
                  initParams.getIntParam("i6"),
                  initParams.getFloatParam("f"));
	else
        if(initParams.getParamName().equals("int-int-float[]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  (float[])initParams.getObjParam("c"));
	else
        if(initParams.getParamName().equals("int-int-int-float[]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  (float[])initParams.getObjParam("d"));
	else
        if(initParams.getParamName().equals("int-float[]-float[]"))
            init (initParams.getIntParam("a"),
                  (float[])initParams.getObjParam("c"),
                  (float[])initParams.getObjParam("d"));
    else
        if(initParams.getParamName().equals("int-int-float[]-float[]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  (float[])initParams.getObjParam("c"),
                  (float[])initParams.getObjParam("d"));
	else
        if(initParams.getParamName().equals("int-int-int-float[][]-float[][]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  (float[][])initParams.getObjParam("x"),
                  (float[][])initParams.getObjParam("y"));
    else
        if(initParams.getParamName().equals("int-int-float[][]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  (float[][])initParams.getObjParam("c"));
    else
	if(initParams.getParamName().equals("int-int-float[][]-float[]"))
	    init(initParams.getIntParam("a"),
		 initParams.getIntParam("b"),
		 (float[][])initParams.getObjParam("c"),
		 (float[])initParams.getObjParam("d"));
    else
	if(initParams.getParamName().equals("int-int-int-float[][]-float[]"))
	    init(initParams.getIntParam("a"),
		 initParams.getIntParam("b"),
		 initParams.getIntParam("c"),
		 (float[][])initParams.getObjParam("d"),
		 (float[])initParams.getObjParam("e"));
    else
	if(initParams.getParamName().equals("int-boolean-float-float-float[][]-float[]"))
	    init(initParams.getIntParam("a"),
		 initParams.getBoolParam("b"),
		 initParams.getFloatParam("c"),
		 initParams.getFloatParam("d"),
		 (float[][])initParams.getObjParam("e"),
		 (float[])initParams.getObjParam("f"));
    else
	if(initParams.getParamName().equals("int-int-int-float"))
	    init(initParams.getIntParam("i1"),
		 initParams.getIntParam("i2"),
		 initParams.getIntParam("i3"),
		 initParams.getFloatParam("f1"));
	else
        if(initParams.getParamName().equals("int-int-int-float[][]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  (float[][])initParams.getObjParam("d"));
	else
        if(initParams.getParamName().equals("int-int-int-int-float[][]"))
            init (initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getIntParam("d"),
                  (float[][])initParams.getObjParam("e"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-int-float"))
            init (initParams.getIntParam("x"),
                  initParams.getIntParam("y"),
                  initParams.getIntParam("z"),
                  initParams.getIntParam("a"),
                  initParams.getIntParam("b"),
                  initParams.getIntParam("c"),
                  initParams.getIntParam("d"),
                  initParams.getFloatParam("f"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-int-int-int-float"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
                  initParams.getIntParam("i6"),
                  initParams.getIntParam("i7"),
                  initParams.getIntParam("i8"),
                  initParams.getIntParam("i9"),
                  initParams.getFloatParam("f"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-int-int-int"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
                  initParams.getIntParam("i6"),
                  initParams.getIntParam("i7"),
                  initParams.getIntParam("i8"),
                  initParams.getIntParam("i9"));
	else
        if(initParams.getParamName().equals("int-int-int-int-int-int-int-int-int-int-float"))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getIntParam("i3"),
                  initParams.getIntParam("i4"),
                  initParams.getIntParam("i5"),
                  initParams.getIntParam("i6"),
                  initParams.getIntParam("i7"),
                  initParams.getIntParam("i8"),
                  initParams.getIntParam("i9"),
                  initParams.getIntParam("i10"),
                  initParams.getFloatParam("f"));
        else
        if(initParams.getParamName().equals("float-float-float-int"))
            init (initParams.getFloatParam("x2"),
                  initParams.getFloatParam("y2"),
                  initParams.getFloatParam("z2"),
                  initParams.getIntParam("a2"));
        else if(initParams.getParamName().equals("Object"))
	    init(initParams.getObjectParam("o1"));
        else if(initParams.getParamName().equals("Object-int"))
            init(initParams.getObjectParam("o1"),
                 initParams.getIntParam("i1"));

        else if(initParams.getParamName().equals("int-int-Object"))
            init(initParams.getIntParam("i1"),
                 initParams.getIntParam("i2"),
                 initParams.getObjectParam("o1"));

	else if(initParams.getParamName().equals("Object-Object"))
	    init(initParams.getObjectParam("o1"),
		 initParams.getObjectParam("o2"));
	else if(initParams.getParamName().equals("Object-Object-Object"))
	    init(initParams.getObjectParam("o1"),
		 initParams.getObjectParam("o2"),
		 initParams.getObjectParam("o3")); else
        if (initParams.getParamName ().equals("int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y")); else
        if (initParams.getParamName ().equals("int-int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y"), initParams.getIntParam ("z")); else
        if (initParams.getParamName ().equals("int-int-int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y"), initParams.getIntParam ("z"), initParams.getIntParam ("a")); else
        if (initParams.getParamName ().equals("float-float-float")) init (initParams.getFloatParam ("a"), initParams.getFloatParam ("b"), initParams.getFloatParam ("c")); else
        if (initParams.getParamName ().equals("short-short-short")) init (initParams.getShortParam ("s1"), initParams.getShortParam ("s2"), initParams.getShortParam ("s3")); else
        if (initParams.getParamName ().equals("")) init (); else
        if (initParams.getParamName ().equals("int")) init (initParams.getIntParam ("n")); else
        if (initParams.getParamName ().equals("float")) init (initParams.getFloatParam ("f")); else
        if (initParams.getParamName ().equals("char")) init (initParams.getCharParam ("c")); else
        if (initParams.getParamName ().equals("String")) init (initParams.getStringParam ("str")); else
        if (initParams.getParamName ().equals("ParameterContainer")) init ((ParameterContainer) initParams.getObjParam ("params"));
        else {
            // labels don't match - print an error
            ERROR ("You didn't provide a correct if-else statement in setupOperator.\nPlease read streams/docs/implementation-notes/library-init-functions.txt for instructions.\n(paramName=" + initParams.getParamName() + ")");
        }
    }

    public void connectGraph ()
    {
        throw new UnsupportedOperationException();
    }

    // ------------------------------------------------------------------
    // ------------------ all graph related functions -------------------
    // ------------------------------------------------------------------

    // get an IO field (input or output)
    // returns null if none present
    Channel[] getIOFields (String fieldName)
    {
        assert fieldName == "input" || fieldName == "output";

        Channel fieldsInstance [] = null;

        try
        {
            Class thisClass = this.getClass ();
            assert thisClass != null;

            Field ioField;
            ioField  = thisClass.getField (fieldName);

            Object fieldValue = ioField.get (this);

            if (ioField.getType ().isArray ())
            {
                fieldsInstance = (Channel []) fieldValue;
            } else {
                fieldsInstance = new Channel [1];
                fieldsInstance [0] = (Channel) fieldValue;

                if (fieldsInstance [0] == null) fieldsInstance = null;
            }
        }
        catch (NoSuchFieldException noError)
        {
            // do not do anything here, this is NOT an error!
        }
        catch (Throwable error)
        {
            // this is all the other errors:
            error.getClass ();
            assert false : error.toString ();
        }

        return fieldsInstance;
    }

    Channel getIOField (String fieldName, int fieldIndex)
    {
        Channel field = null;

        {
            Channel fieldInstance[];
            fieldInstance = getIOFields (fieldName);

            if (fieldInstance != null)
            {
                assert fieldInstance.length > fieldIndex;
                field = fieldInstance [fieldIndex];
            }
        }

        return field;
    }

    void setIOField (String fieldName, int fieldIndex, Channel newChannel)
    {
        assert fieldName == "input" || fieldName == "output";

        Channel fieldsInstance [];

        try
        {
            Class thisClass = this.getClass ();
            assert thisClass != null;

            Field ioField;
            ioField  = thisClass.getField (fieldName);

            if (ioField.getType () == newChannel.getClass ())
            {
                assert fieldIndex == 0;
                ioField.set (this, newChannel);
            } else {
                fieldsInstance = (Channel []) ioField.get (this);
                assert fieldsInstance != null;
                assert fieldsInstance.length > fieldIndex;

                fieldsInstance [fieldIndex] = newChannel;
            }

        }
        catch (Throwable error)
        {
            // this is all the other errors:
            assert false : error.toString ();
        }
    }


    float[][] copyFloatArray2D(float[][] input) {
	// according to streamit semantics, assume arrays are rectangular
	float[][] result = new float[input.length][input[0].length];
	for (int i=0; i<input.length; i++) {
	    for (int j=0; j<input[0].length; j++) {
		result[i][j] = input[i][j];
	    }
	}
	return result;
    }

    float[] copyFloatArray1D(float[] input) {
	// according to streamit semantics, assume arrays are rectangular
	float[] result = new float[input.length];
	for (int i=0; i<input.length; i++) {
	    result[i] = input[i];
	}
	return result;
    }

    int[] copyIntArray1D(int[] input) {
	// according to streamit semantics, assume arrays are rectangular
	int[] result = new int[input.length];
	for (int i=0; i<input.length; i++) {
	    result[i] = input[i];
	}
	return result;
    }

    /**
     * This is what shows up on nodes in the dot graph output of the
     * library.
     */
    public String toString() {
	return getClass().getName();
    }
}
