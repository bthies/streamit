/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Utils.java,v 1.5 2006-03-24 20:48:35 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.io.File;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import at.dms.compiler.tools.antlr.runtime.FileLineFormatter;

public class Utils extends at.dms.compiler.tools.common.Utils {

    /**
     * Returns the pathname string of this abstract pathname's parent.
     *
     * This method handles the cases, where the file is specified without
     * a directory or is in the root directory.
     */
    public static File getParent(File f) {
        String  dirname = f.getParent();

        if (dirname == null) {
            if (f.isAbsolute()) {
                dirname = File.separator;
            } else {
                dirname = System.getProperty("user.dir");
            }
        }
        return new File(dirname);
    }

    /**
     * given a filename, strip off the directory prefix (if any)
     *  and return it.  Return "./" if f has no dir prefix.
     */
    public static String pathToFile(String f) {
        String  separator = System.getProperty("file.separator");
        int     endOfPath = f.lastIndexOf(separator);

        if (endOfPath == -1) {
            // no path, use current directory
            return "." + separator;
        } else {
            return f.substring(0, endOfPath+1);
        }
    }

    public static String fileMinusPath(String f) {
        String  separator = System.getProperty("file.separator");
        int     endOfPath = f.lastIndexOf(separator);

        if (endOfPath == -1) {
            return f;   // no path found
        } else {
            return f.substring(endOfPath+1);
        }
    }

    /**
     * This example is from the book _Java in a Nutshell_ by David
     * Flanagan.  Written by David Flanagan.  Copyright (c) 1996
     * O'Reilly &amp; Associates.  You may study, use, modify, and
     * distribute this example for any purpose.  This example is
     * provided WITHOUT WARRANTY either expressed or implied.  */
    public static void copyFile(String sourceName, String destName)
        throws IOException
    {
        File sourceFile = new File(sourceName);
        File destinationFile = new File(destName);
        FileReader source = null; // SAS: changed for proper text io
        FileWriter destination = null;
        char[] buffer;

        try {
            // First make sure the specified source file
            // exists, is a file, and is readable.
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                throw new FileCopyException("FileCopy: no such source file: " + sourceName);
            }
            if (!sourceFile.canRead()) {
                throw new FileCopyException("FileCopy: source file is unreadable: " + sourceName);
            }

            // If the destination exists, make sure it is a writeable file
            // and ask before overwriting it.  If the destination doesn't
            // exist, make sure the directory exists and is writeable.
            if (destinationFile.exists()) {
                if (destinationFile.isFile()) {
                    DataInputStream in = new DataInputStream(System.in);
                    String response;

                    if (!destinationFile.canWrite()) {
                        throw new FileCopyException("FileCopy: destination file is unwriteable: " + destName);
                    }
                } else {
                    throw new FileCopyException("FileCopy: destination is not a file: " +  destName);
                }
            } else {
                File parentdir = Utils.getParent(destinationFile);
                if (!parentdir.exists()) {
                    throw new FileCopyException("FileCopy: destination directory doesn't exist: " + destName);
                }
                if (!parentdir.canWrite()) {
                    throw new FileCopyException("FileCopy: destination directory is unwriteable: " + destName);
                }
            }

            // If we've gotten this far, then everything is okay; we can copy the file.
            source = new FileReader(sourceFile);
            destination = new FileWriter(destinationFile);
            buffer = new char[1024];

            while (true) {
                int bytesRead;

                bytesRead = source.read(buffer,0,1024);
                if (bytesRead == -1) {
                    break;
                }
                destination.write(buffer, 0, bytesRead);
            }
        } finally {
            // No matter what happens, always close any streams we've opened.
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * General-purpose utility function for removing
     * characters from back of string
     * @param s The string to process
     * @param c The character to remove
     * @return The resulting string
     */
    static public String stripBack(String s, char c) {
        while (s.length() > 0 && s.charAt(s.length()-1) == c) {
            s = s.substring(0, s.length()-1);
        }
        return s;
    }

    /**
     * General-purpose utility function for removing
     * characters from back of string
     * @param s The string to process
     * @param remove A string containing the set of characters to remove
     * @return The resulting string
     */
    static public String stripBack(String s, String remove) {
        boolean changed;

        do {
            changed = false;
            for (int i = 0; i < remove.length(); i++) {
                char c = remove.charAt(i);
                while (s.length() > 0 && s.charAt(s.length()-1) == c) {
                    changed = true;
                    s = s.substring(0, s.length()-1);
                }
            }
        } while (changed);
        return s;
    }

    /**
     * General-purpose utility function for removing
     * characters from front of string
     * @param s The string to process
     * @param c The character to remove
     * @return The resulting string
     */
    static public String stripFront(String s, char c) {
        while (s.length() > 0 && s.charAt(0) == c) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * General-purpose utility function for removing
     * characters from front of string
     * @param s The string to process
     * @param remove A string containing the set of characters to remove
     * @return The resulting string
     */
    static public String stripFront(String s, String remove) {
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < remove.length(); i++) {
                char c = remove.charAt(i);
                while (s.length() > 0 && s.charAt(0) == c) {
                    changed = true;
                    s = s.substring(1);
                }
            }
        } while (changed);
        return s;
    }

    /**
     * General-purpose utility function for removing
     * characters from the front and back of string
     * @param src The string to process
     * @param head exact string to strip from head
     * @param tail exact string to strip from tail
     * @return The resulting string
     */
    public static String stripFrontBack(String src, String head, String tail) {
        int h = src.indexOf(head);
        int t = src.lastIndexOf(tail);
        if ( h==-1 || t==-1 ) {
            return src;
        }
        return src.substring(h+1,t);
    }

    // --------------------------------------------------------------------

    /**
     * Issue an unknown fatal error
     */
    public static void panic() {
        panic("");
    }

    /**
     * Issue a fatal error message
     * @param s The message
     */
    public static void panic(String s) {
        System.err.println("panic: "+s);
        System.exit(1);
    }

    /**
     * Issue an error; used for general tool errors not for grammar stuff
     * @param s The message
     */
    public static void toolError(String s) {
        System.err.println("error: "+s);
    }

    /**
     * Issue a warning
     * @param s the message
     */
    public static void warning(String s) {
        System.err.println("warning: "+s);
    }

    /**
     * Issue a warning with line number information
     * @param s The message
     * @param file The file that has the warning
     * @param line The grammar file line number on which the warning occured
     */
    public static void warning(String s, String file, int line) {
        if ( file!=null ) {
            System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+"warning:"+s);
        } else {
            System.err.println("warning; line "+line+": "+s);
        }
    }

    /**
     * Issue a warning with line number information
     * @param s The lines of the message
     * @param file The file that has the warning
     * @param line The grammar file line number on which the warning occured
     */
    public static void warning(String[] s, String file, int line) {
        if ( s==null || s.length==0 ) {
            panic("bad multi-line message to Tool.warning");
        }
        if ( file!=null ) {
            System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+"warning:"+s[0]);
            for (int i=1; i<s.length; i++) {
                System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+s[i]);
            }
        } else {
            System.err.println("warning: line "+line+": "+s[0]);
            for (int i=1; i<s.length; i++) {
                System.err.println("warning: line "+line+": "+s[i]);
            }
        }
    }

    public static void setFileLineFormatter(FileLineFormatter formatter) {
        FileLineFormatter.setFormatter(formatter);
    }
}
