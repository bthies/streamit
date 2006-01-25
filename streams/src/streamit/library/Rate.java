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

import java.util.*;

/**
 * Represents an I/O rate, possibly having some range of values.
 * Dynamic rates * are represented by the field DYNAMIC_RATE and can
 * appear in any of the positions.
 */
public class Rate extends streamit.misc.DestroyedClass {
    public static int DYNAMIC_RATE = -1; // don't make final or it will be inlined!

    // min, ave, max rates
    public final int min;
    public final int ave;
    public final int max;

    public Rate(int min, int ave, int max) {
        this.min = min;
        this.ave = ave;
        this.max = max;
    }

    /**
     * Returns whether or not this is a static rate.  Can be
     * over-ridden by a fixed-rate subclass.
     */
    public boolean isStatic() {
        return false;
    }
}
