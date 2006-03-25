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

package streamit.misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Misc extends AssertedClass
{
    public static int MAX (int a, int b)
    {
        return (a > b ? a : b);
    }

    public static int MIN (int a, int b)
    {
        return (a < b ? a : b);
    }

    /**
     * FILE READING FUNCTIONS -----------------------------------------------------------
     *
     * These functions live here so that they can be shared by
     * frontend and library without introducing a dependence from one
     * to the other.
     */

    /**
     * Returns int[size] array of elements read from <pre>filename</pre>, which
     * should be text with one value per line.
     */
    public static int[] init_array_1D_int(String filename, int size) {
        int[] result = new int[size];
        try {
            BufferedReader reader = new BufferedReader(new java.io.FileReader(filename));
            for (int i=0; i<size; i++) {
                String line = reader.readLine();
                if (line==null) {
                    throw new RuntimeException("File \"" + filename + "\" contains fewer than " + size + " elements.");
                }
                result[i] = Integer.parseInt(line);
            }
            if (reader.readLine()!=null) {
                System.err.println("WARNING: file \"" + filename + "\" contains more elements than were read (" + size + " were read)");
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file \"" + filename + "\", needed to initialize an array.");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * Returns float[size] array of elements read from <pre>filename</pre>,
     * which should be text with one value per line.
     */
    public static float[] init_array_1D_float(String filename, int size) {
        float[] result = new float[size];
        try {
            BufferedReader reader = new BufferedReader(new java.io.FileReader(filename));
            for (int i=0; i<size; i++) {
                String line = reader.readLine();
                if (line==null) {
                    throw new RuntimeException("File \"" + filename + "\" contains fewer than " + size + " elements.");
                }
                result[i] = Float.parseFloat(line);
            }
            if (reader.readLine()!=null) {
                System.err.println("WARNING: file \"" + filename + "\" contains more elements than were read (" + size + " were read)");
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file \"" + filename + "\", needed to initialize an array.");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * END FILE READING FUNCTIONS -----------------------------------------------------------
     */
}
