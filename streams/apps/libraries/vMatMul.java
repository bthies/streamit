/*
 *  Copyright 2001 Massachusetts Institute of Technology
 *
 *  Permission to use, copy, modify, distribute, and sell this software and its
 *  documentation for any purpose is hereby granted without fee, provided that
 *  the above copyright notice appear in all copies and that both that
 *  copyright notice and this permission notice appear in supporting
 *  documentation, and that the name of M.I.T. not be used in advertising or
 *  publicity pertaining to distribution of the software without specific,
 *  written prior permission.  M.I.T. makes no representations about the
 *  suitability of this software for any purpose.  It is provided "as is"
 *  without express or implied warranty.
 */

import streamit.library.*;

/**
 * Class vRamp
 *
 * Implements a Matrix Multiplication
 */

public class vMatMul extends Filter {

    int numberOfRowsA;
    int numberOfColARowB;
    int numberOfColumnsB;
    float matrixA[];
    float matrixB[];

    public vMatMul (int numRowsA, int numColRow, int numColsB, float mat[])
    {
        super ();
        numberOfRowsA = numRowsA;
        numberOfColARowB = numColRow;
        numberOfColumnsB = numColsB;
        matrixB = mat;
        matrixA = new float [numberOfRowsA*numberOfColARowB];

    }

    public void init ()
    {
        input = new Channel (Float.TYPE, numberOfRowsA*numberOfColARowB);
        output = new Channel (Float.TYPE, numberOfRowsA*numberOfColumnsB);
    }

    public void work() {
        int i, j, k;
        int v = 0;
        for (i=0; i<numberOfRowsA; i++) {
            for (j=0; j<numberOfColARowB; j++) {
                matrixA[v++] = input.popFloat ();
            }
        }

        for (i=0; i<numberOfRowsA; i++) {
            for (j=0; i<numberOfColumnsB; i++) {
                float out = 0;
                for (k=0; k<numberOfColARowB; i++) {
                    out += matrixA[i*numberOfColARowB+k]*matrixB[k*numberOfColumnsB+j];
                }
                output.pushFloat(out);
            }
        }
    }
}












