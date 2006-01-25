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

/*
 * A RateStatic is an I/O rate that is known at compile time.  That
 * is, min==ave==max and the value is not the dynamic token *.
 */
public class RateStatic extends Rate {
    // the rate qualifier to be used for a dynamic rate
    public static final int DYNAMIC_RATE = -1;

    public RateStatic(int rate) {
        super(rate, rate, rate);
    }

    /**
     * Returns whether or not this is a statically known rate.
     */
    public boolean isStatic() {
        return true;
    }

    public int getRate() {
        return super.min; // == super.ave == super.max
    }
}
