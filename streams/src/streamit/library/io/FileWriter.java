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
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import java.io.*;

public class FileWriter extends Filter
{
    Class fileType;
    File outputFile;
    DataOutputStream outputStream;
    boolean closed = true;
    /**
     * List of all FileWriters that have ever been created.
     */
    private static List<FileWriter> allFileWriters = new LinkedList<FileWriter>();

    public FileWriter (String fileName, Class type, boolean TREAT_AS_BITS)
    {
        allFileWriters.add(this);
        // This is part of the hack to make FileReader/Writer&lt;bit:gt; work
        if (TREAT_AS_BITS)
            fileType = null;
        else 
            fileType = type;

        try
            {
                outputFile = new File(fileName);
                FileOutputStream fileOutputStream = new java.io.FileOutputStream (outputFile);
                outputStream = new DataOutputStream (new BufferedOutputStream(fileOutputStream));
                closed = false;
            }
        catch(Throwable e)
            {
                ERROR (e);
            }
    }

    // This is part of the hack to make FileReader/Writer&lt;bit:gt; work
    public FileWriter (String fileName, Class type) {
        this(fileName, type, false);
    }

    public void init ()
    {
        // This is part of the hack to make FileReader/Writer&lt;bit:gt; work
        if (fileType == null) {
            inputChannel = new Channel (Integer.TYPE, 1);
            bits_to_go = 8;
            the_bits = 0;
        } else {
            inputChannel = new Channel (fileType, 1);
        }
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


    // for writing bits one at a time.
    private int bits_to_go = 8;
    private byte the_bits = 0;

    public void work ()
    {
        try {
            // This is part of the hack to make FileReader/Writer&lt;bit:gt; work
            if (fileType == null) {
                the_bits = (byte) ((the_bits << 1) | (inputChannel.popInt() & 1));
                bits_to_go--;
                if (bits_to_go == 0) {
                    outputStream.writeByte(the_bits);
                    the_bits = 0;
                    bits_to_go = 8;
                }
            } else if (fileType == Integer.TYPE) {
                outputStream.writeInt(endianFlip(inputChannel.popInt()));
            } else if (fileType == Short.TYPE) {
                outputStream.writeShort(endianFlip(inputChannel.popShort()));
            } else if (fileType == Character.TYPE) {
                outputStream.writeChar(inputChannel.popChar());
            } else if (fileType == Float.TYPE) {
                outputStream.writeInt(endianFlip(Float.floatToIntBits(inputChannel
                                                                      .popFloat())));
            } else {
                ERROR("You must define a writer for your type here.\n" +
                      "Object writing isn't supported right now " +
                      "(for compatibility with the\n" +
                      "C library).");
            }
        } catch (Throwable e) {
            ERROR(e);
        }
    }

    /**
     * Closes all FileWriters that have ever been instantiated.
     */
    public static void closeAll() {
        for (Iterator<FileWriter> i = allFileWriters.iterator(); i.hasNext(); ) {
            i.next().close();
        }
    }

    /**
     * Closing is necessary to get last bits out for &lt;bit:gt;
     */
    public void close() 
    {
        if (!closed) {
            closed = true;
            try {
                if (fileType == null && bits_to_go != 8) {
                    the_bits = (byte) (the_bits << bits_to_go);
                    outputStream.writeByte(the_bits);
                    bits_to_go = 8;
                }
                outputStream.flush();
                outputStream.close();
            } catch (Throwable e) {
                ERROR(e);
            }
        }
    }

    /**
     * Destructor should write anything left and close the file.
     * 
     * Should happen on finalize: inherits from DestroyedClass which calls
     * DELETE on finalization. But doesn't work. Presumably
     * DestroyedClass.finalize() is called too early while there are still
     * references to the FileWriter.
     * 
     * Using System.runFinalization(); System.gc(); as last thing in main will
     * work sometimes, but is not consistent. Using
     * System.runFinalizersOnExit(true); will run a finalizer but will close the
     * file first, causing an exception when close is called!
     * 
     */

    public void DELETE() // on finalize: inherits from DestroyedClass
    {
        close();
    }
}
 
