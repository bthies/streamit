/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: ClassPath.java,v 1.5 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.classfile;

import java.io.*;
import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import java.util.zip.ZipEntry;

import at.dms.util.Utils;

/**
 * This class implements the conceptual directory structure for .class files
 */
public class ClassPath {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Constructs a class path object.
     *
     * @param   path        the directory names defining the class path
     */
    public ClassPath(String path) {
        if (path == null) {
            // no path specified, use default
            path = System.getProperty("java.class.path");
        }
        if (path == null) {
            // last resort, use current directory
            path = ".";
        }

        this.dirs = loadClassPath(path);
    }

    /**
     * Loads the conceptual directories defining the class path.
     *
     * @param   classPath   the directory names defining the class path
     */
    private static ClassDirectory[] loadClassPath(String classPath) {
        Vector      container = new Vector();

        // load specified class directories
        StringTokenizer entries;

        entries = new StringTokenizer(classPath, File.pathSeparator);
        while (entries.hasMoreTokens()) {
            ClassDirectory  dir;

            dir = loadClassDirectory(entries.nextToken());
            if (dir != null) {
                container.addElement(dir);
            }
        }

        // add system directories
        if (System.getProperty("sun.boot.class.path") != null) {
            // ??? graf 010508 : can there be more than one entry
            entries = new StringTokenizer(System.getProperty("sun.boot.class.path"),
                                          File.pathSeparator);
            while (entries.hasMoreTokens()) {
                ClassDirectory  dir;

                dir = loadClassDirectory(entries.nextToken());
                if (dir != null) {
                    container.addElement(dir);
                }
            }
        } else {
            String  version = System.getProperty("java.version");

            if (version.startsWith("1.2") || version.startsWith("1.3")) {
                ClassDirectory  dir;

                dir = loadClassDirectory(System.getProperty("java.home")
                                         + File.separatorChar + "lib"
                                         + File.separatorChar + "rt.jar");
                if (dir != null) {
                    container.addElement(dir);
                }
            }
        }

        return (ClassDirectory[])Utils.toArray(container, ClassDirectory.class);
    }

    /**
     * Loads a conceptual class directory.
     *
     * @param   name        the name of the directory
     */
    private static ClassDirectory loadClassDirectory(String name) {
        try {
            File    file = new File(name);

            if (file.isDirectory()) {
                return new DirClassDirectory(file);
            } else if (file.isFile()) {
                // check if file is zipped (.zip or .jar)
                if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
                    try {
                        return new ZipClassDirectory(new ZipFile(file));
                    } catch (ZipException e) {
                        // it was not a zip file, ignore it
                        return null;
                    } catch (IOException e) {
                        // ignore it
                        return null;
                    }
                } else {
                    // wrong suffix, ignore it
                    return null;
                }
            } else {
                // wrong file type, ignore it
                return null;
            }
        } catch (SecurityException e) {
            // unreadable file, ignore it
            return null;
        }
    }


    // ----------------------------------------------------------------------
    // CLASS LOADING
    // ----------------------------------------------------------------------

    /**
     * Loads the class with specified name.
     *
     * @param   name        the qualified name of the class
     * @param   interfaceOnly   do not load method code ?
     * @return  the class info for the specified class,
     *      or null if the class cannot be found
     */
    public ClassInfo loadClass(String name, boolean interfaceOnly) {
        for (int i = 0; i < dirs.length; i++) {
            ClassInfo       info;

            info = dirs[i].loadClass(name, interfaceOnly);
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    // ----------------------------------------------------------------------
    // SINGLETON
    // ----------------------------------------------------------------------

    /**
     * initialization from a string that represents the class path
     * @param path      the classpath
     */
    public static void init(String path) {
        self = new ClassPath(path);
    }

    /**
     * @return a class file that contain the class named name
     * @param name the name of the class file
     */
    public static ClassInfo getClassInfo(String name, boolean interfaceOnly) {
        return self.loadClass(name, interfaceOnly);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private static ClassPath    self = null;

    private ClassDirectory[]    dirs;   // list of directories in class path

    /**
     * This class represents a conceptual directory which may hold
     * Java class files. Since Java can use archived class files found in
     * a compressed ('zip') file, this entity may or may not correspond to
     * an actual directory on disk.
     */
    abstract static class ClassDirectory {

        /**
         * Loads the class with specified name from this directory.
         *
         * @param   name        the qualified name of the class
         * @param   interfaceOnly   do not load method code ?
         * @return  the class info for the specified class,
         *      or null if the class cannot be found in this directory
         */
        public abstract ClassInfo loadClass(String name, boolean interfaceOnly);
    }

    static class DirClassDirectory extends ClassDirectory {
        /**
         * Constructs a class directory representing a real directory
         */
        public DirClassDirectory(File dir) {
            this.dir = dir;
        }

        /**
         * Loads the class with specified name from this directory.
         *
         * @param   name        the qualified name of the class
         * @param   interfaceOnly   do not load method code ?
         * @return  the class info for the specified class,
         *      or null if the class cannot be found in this directory
         */
        public ClassInfo loadClass(String name, boolean interfaceOnly) {
            File        file;

            file = new File(dir.getPath(),
                            name.replace('/', File.separatorChar) + ".class");
            if (!file.canRead()) {
                return null;
            } else {
                try {
                    Data        data = new Data(new FileInputStream(file));

                    try {
                        return new ClassInfo(data.getDataInput(), interfaceOnly);
                    } catch (ClassFileFormatException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        data.release();
                    }
                } catch (FileNotFoundException e) {
                    return null; // really bad : file exists but is not accessible
                }
            }
        }

        // ----------------------------------------------------------------------
        // DATA MEMBERS
        // ----------------------------------------------------------------------

        private File        dir;        // non null iff is a real directory
    }

    static class ZipClassDirectory extends ClassDirectory {
        /**
         * Constructs a class directory representing a zipped file
         */
        public ZipClassDirectory(ZipFile zip) {
            this.zip = zip;
        }

        /**
         * Loads the class with specified name from this directory.
         *
         * @param   name        the qualified name of the class
         * @param   interfaceOnly   do not load method code ?
         * @return  the class info for the specified class,
         *      or null if the class cannot be found in this directory
         */
        public ClassInfo loadClass(String name, boolean interfaceOnly) {
            ZipEntry        entry;

            entry = zip.getEntry(name + ".class");
            if (entry == null) {
                return null;
            } else {
                try {
                    Data        data = new Data(zip.getInputStream(entry));

                    try {
                        return new ClassInfo(data.getDataInput(), interfaceOnly);
                    } catch (ClassFileFormatException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        data.release();
                    }
                } catch (IOException e) {
                    return null; // really bad : file exists but is not accessible
                }
            }
        }

        // ----------------------------------------------------------------------
        // DATA MEMBERS
        // ----------------------------------------------------------------------

        private ZipFile zip;        // non null iff is a zip or jar file
    }

    // optimization
    static class Data {
        public Data(InputStream is) {
            this.is = is;
        }

        public DataInput getDataInput() throws IOException {
            ba = getByteArray();
            int     n = 0;
            int     available;
            while (true) {
                int count = is.read(ba, n, ba.length - n);
                if (count < 0) {
                    break;
                }
                available = is.available();
                n += count;
                if (n + available > ba.length) {
                    byte[] temp = new byte[ba.length * 2];
                    System.arraycopy(ba, 0, temp, 0, ba.length);
                    ba = temp;
                } else if (available == 0) {
                    break;
                }
            }

            is.close();
            is = null;

            return new DataInputStream(new ByteArrayInputStream(ba, 0, n));
        }

        public void release() {
            release(ba);
        }

        private static byte[] getByteArray() {
            if (!Constants.ENV_USE_CACHE || stack.empty()) {
                return new byte[10000];
            }
            return stack.pop();
        }

        private static void release(byte[] arr) {
            if (Constants.ENV_USE_CACHE) {
                stack.push(arr);
            }
        }

        // ----------------------------------------------------------------------
        // DATA MEMBERS
        // ----------------------------------------------------------------------

        private InputStream is;
        private byte[]  ba;
        private static Stack<byte[]>    stack = new Stack<byte[]>();
    }
}
