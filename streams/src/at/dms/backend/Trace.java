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
 * $Id: Trace.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is the entry point of the backend, this class constructs the
 * control flow graf and applies optimizations
 */
public class Trace {

  Trace(String fileName) {
    try {
      writer = new FileWriter(new File(fileName + _count++));
    } catch (IOException e) {
      System.err.println("Cannot write " + fileName);
    }
  }

  // --------------------------------------------------------------------
  // PUBLIC UTILITIES
  // --------------------------------------------------------------------

  /**
   * Generates a node
   */
  public static String generateNode(String title, String label) {
    return "node: { title:\"" + title + "\" label: \"" + label + "\"}";
  }

  /**
   * Generates a node
   */
  public static String generateEdge(String from, String to, int nb, int count, boolean arrow) {
    return "edge: { sourcename:\"" + from + "\" targetname:\"" + to + "\" class: 1 " +
      (arrow ? "" : "arrowstyle:none" ) + "}";
  }

  protected void write(String s) throws IOException {
    writer.write(s);
    writer.write("\n");
  }

  protected void close() throws IOException {
    writer.close();
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private FileWriter			writer;
  private static int			_count;
}
