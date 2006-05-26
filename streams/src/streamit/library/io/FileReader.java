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
import streamit.library.Stream;
import streamit.library.Channel;

import java.io.*;

public class FileReader extends Filter
{
    Class fileType;
    File inputFile;
    String fileName;
    DataInputStream inputStream;

    public FileReader (String fileName, Class type, boolean TREAT_AS_BITS)
    {
        // This is part of the hack to make FileReader/Writer<bit> work
        if (TREAT_AS_BITS)
            this.fileType = null;
        else
            this.fileType = type;
        this.fileName = fileName;
        openFile();
    }

    // This is part of the hack to make FileReader/Writer<bit> work
    public FileReader (String fileName, Class type) {
        this(fileName, type, false);
    }

    private void closeFile() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        try
            {
                inputFile = new File(fileName);
                FileInputStream fileInputStream = new java.io.FileInputStream (inputFile);
                inputStream = new DataInputStream (new BufferedInputStream(fileInputStream));
            }
        catch(Throwable e)
            {
                ERROR (e);
            }
    }

    public void init ()
    {
        // Hacked to make FileReader/Writer<bit> work
        if (fileType == null) {
            outputChannel = new Channel (Integer.TYPE, 1);
            bits_to_go = 0;
        } else {
            outputChannel = new Channel (fileType, 1);
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

    // for reading bits one at a time.
    private int bits_to_go = 0;
    private byte the_bits = 0;

    public void work ()
    {
        boolean done = false;
        while (!done) {
            try {
                // Hacked to make FileReader/Writer<bit> work
                if (fileType == null) { // fileType is a bit
                    if (bits_to_go == 0) {
                        the_bits = (byte)(inputStream.readUnsignedByte());
                        bits_to_go = 8;
                    }
                    outputChannel.pushInt((the_bits >> 7) & 1);
                    the_bits <<= 1;
                    bits_to_go--;
                } else if (fileType == Integer.TYPE) {
                    outputChannel.pushInt (endianFlip (inputStream.readInt ()));
                } else if (fileType == Short.TYPE) {
                    outputChannel.pushShort (endianFlip (inputStream.readShort ()));
                } else if (fileType == Character.TYPE) {
                    outputChannel.pushChar (inputStream.readChar ());
                } else if (fileType == Float.TYPE) {
                    outputChannel.pushFloat (Float.intBitsToFloat (endianFlip (inputStream.readInt ())));
                } else {
                    ERROR ("You must define a reader for your type here.\nObjects aren't really supported right now (for compatibility\nwith the C library).");
                }
                done = true;
            }
            catch (EOFException e) {
                if (!Stream.scheduledRun) {
                    // If in -nosched mode, return from work without
                    // having done anything.  This will result in a
                    // NoPushPopException and a switch to the next
                    // sink.
                    break;
                } 
                // otherwise hang, for now...

                // try closing and opening file, to try again
                // closeFile();
                // openFile();
            }
            catch (Throwable e)
                {
                    ERROR (e);
                }
        }
    }

}
