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

package streamit.library.io;

import streamit.library.Filter;
import streamit.library.Channel;

import java.io.*;

public class FileWriter extends Filter
{
    Class fileType;
    File outputFile;
    java.io.FileOutputStream fileOutputStream;
    DataOutputStream outputStream;

    public FileWriter (String fileName, Class type)
    {
        fileType = type;

        try
        {
            outputFile = new File(fileName);
            fileOutputStream = new java.io.FileOutputStream (outputFile);
            outputStream = new DataOutputStream (fileOutputStream);
        }
        catch(Throwable e)
        {
            ERROR (e);
        }
    }

    public void init ()
    {
        input = new Channel (fileType, 1);
    }

    int endianFlip (int x)
    {
        int x0, x1, x2, x3;
        x0 = (x >> 24) & 0xff;
        x1 = (x >> 16) & 0xff;
        x2 = (x >> 8) & 0xff;
        x3 = (x >> 0) & 0xff;

        return (x0 | (x1 << 8) | (x2 << 16) | (x3 << 24));
    }

    short endianFlip (short x)
    {
        int x0, x1, x2, x3;
        x0 = (x >> 8) & 0xff;
        x1 = (x >> 0) & 0xff;

        return (short)(x0 | (x1 << 8));
    }

    public void work ()
    {
        try
        {
            if (fileType == Integer.TYPE)
            {
                outputStream.writeInt (endianFlip (input.popInt ()));
            } else
            if (fileType == Short.TYPE)
            {
                outputStream.writeShort (endianFlip (input.popShort ()));
            } else
            if (fileType == Character.TYPE)
            {
                outputStream.writeChar (input.popChar ());
            } else
            if (fileType == Float.TYPE)
            {
                outputStream.writeInt (endianFlip (Float.floatToIntBits (input.popFloat ())));
            } else
            {
                ERROR ("You must define a writer for your type here.\nObject writing isn't supported right now (for compatibility with the\nC library).");
            }
        }
        catch (Throwable e)
        {
            ERROR (e);
        }
    }

    /**
     * Destructor closes the file written.
     */
    public void DELETE ()
    {
        try
        {
            outputStream.close ();
        }
        catch (Throwable e)
        {
            ERROR (e);
        }
    }
}
