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
 * $Id: Table.java,v 1.3 2006-03-24 20:48:35 dimock Exp $
 */

package at.dms.compiler.tools.jperf;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

/**
 * This class is the internal representation of the tables T1 and T2.
 * The meaning of T1 and T2 could be found in explanations of GGPerf's
 * algorithm.
 */

public class Table {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates an instance of table representing one of T1 and T2.
     *
     * @param   tableName       the name of the table
     * @param   maxWordLength       the maximum length of a keyword
     * @param   minCharValue        the smallest ASCII value in all keys
     * @param   maxCharValue        the largest ASCII value in all keys?
     */
    public Table(String tableName,
                 int maxWordLength,
                 char minCharValue,
                 char maxCharValue)
    {
        this.tableName = tableName;
        this.maxWordLength = maxWordLength;
        this.minCharValue = minCharValue;
        this.maxCharValue = maxCharValue;
    }

    /**
     * Initialises the internal structures
     */
    public void init() {
        heads = new Hashtable[maxWordLength];
        for (int i = 0; i < maxWordLength; i++) {
            heads[i] = new Hashtable();
        }
    }

    /**
     * Inserts the key into the table.
     *
     * A randomly generated value is assigned to be
     * the table value correspondent to each character.
     *
     * @param   key     the key to insert
     * @param   max     the maximum value
     * @return the sum of the values assigned to each character
     */
    public long insertKey(String key, long max) {
        long    sum = 0;

        for (int i = 0; i < key.length(); i++) {
            Character       c = new Character(key.charAt(i));
            Long        assigned = (Long)heads[i].get(c);

            if (assigned != null) {
                // if it is already there, just use the old value
                sum += assigned.longValue();
            } else {
                // otherwise, call the random generator
                long    value = Math.abs(random.nextLong()) % max;

                heads[i].put(c, new Long(value));
                sum += value;
            }
        }

        return sum % max;
    }

    /**
     * Returns the value previously assigned to the key
     *
     * @param   key     the key
     */
    public long getKeyValue(String key) {
        long    sum = 0;

        for (int i = 0; i < key.length(); i++) {
            Character       c = new Character(key.charAt(i));
            Long        assigned = (Long)heads[i].get(c);

            // it should be there
            if (assigned == null) {
                System.err.println("Internal fatal error: can't find table items for " + key);
                System.exit(-1);
            }
            sum += assigned.longValue();
        }

        return sum;
    }

    // --------------------------------------------------------------------
    // CODE GENERATION
    // --------------------------------------------------------------------

    /**
     * Outputs the contents of the table as a data structure,
     * normally an array.
     * @param   out     the output stream.
     */
    public void genCode(PrintWriter out) {
        out.println("    private static final int[][] " + tableName + " = {");

        for (int i = 0; i < heads.length; i++) {
            Hashtable       items = heads[i];

            out.print("    {");
            for (char c = minCharValue; c <= maxCharValue; c++) {
                Character   ch = new Character(c);

                if (c != minCharValue) {
                    out.print(",");
                }

                if (items.containsKey(ch)) {
                    out.print(((Long)items.get(ch)).longValue());
                } else {
                    out.print("-1");
                }
            }

            if (i < heads.length - 1) {
                out.println("},");
            } else {
                out.println("}");
            }
        }

        out.println("    };");
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    /**
     * The random number generator for generating values of T1 and T2.
     * It is shared by all tables, thus this variable is a class variable.
     */
    private static Random       random = new Random(new Date().getTime());

    private final String        tableName;
    private final int       maxWordLength;
    private final char      minCharValue;
    private final char      maxCharValue;

    /**
     * Variable holding all table heads for T1 and T2, respectively.
     * Thus this variable is an instance variable.  A table head is a
     * Vector holding character-value pairs for a whole row.
     */
    private Hashtable[]     heads;
}
