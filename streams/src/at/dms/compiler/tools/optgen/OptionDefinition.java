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
 * $Id: OptionDefinition.java,v 1.4 2003-06-05 11:40:39 jasperln Exp $
 */

package at.dms.compiler.tools.optgen;

import java.io.PrintWriter;
import java.util.Hashtable;

class OptionDefinition {

  /**
   * Constructs an option definition
   */
  public OptionDefinition(String longname,
			  String shortname,
			  String type,
			  String defaultValue,
			  String argument,
			  String help)
  {
    this.longname = trail(longname);
    this.shortname = trail(shortname);
    this.type = type;
    this.defaultValue = trail(defaultValue);
    this.argument = trail(argument);
    this.help = trail(help);
  }

  private static final String trail(String s) {
    // strip leading and trailing quotes
    if (s == null) {
      return null;
    } else if (s.length() < 2) {
      return s;
    } else {
      return s.substring(1, s.length() - 1);
    }
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Check for duplicate identifiers
   * @param	identifiers	a table of all token identifiers
   * @param	prefix		the literal prefix
   * @param	id		the id of the token
   * @param	sourceFile	the file where the token is defined
   */
  public void checkIdentifiers(Hashtable identifiers, String sourceFile)
    throws OptgenError
  {
    String		stored = (String)identifiers.get(longname);

    if (stored != null) {
      throw new OptgenError(OptgenMessages.DUPLICATE_DEFINITION, new Object[] { longname, sourceFile, stored });
    }

    identifiers.put(longname, sourceFile);
  }

  /**
   * Check for duplicate identifiers
   * @param	identifiers	a table of all token identifiers
   * @param	prefix		the literal prefix
   * @param	id		the id of the token
   * @param	sourceFile	the file where the token is defined
   */
  public void checkShortcuts(Hashtable shortcuts, String sourceFile)
    throws OptgenError
  {
    String		stored = (String)shortcuts.get(shortname);

    if (stored != null) {
      throw new OptgenError(OptgenMessages.DUPLICATE_SHORTCUT, new Object[] { shortname, sourceFile, stored });
    }

    shortcuts.put(shortname, sourceFile);
  }

  /**
   * Prints the token definition to interface file (java)
   * @param	out		the output stream
   * @param	prefix		the literal prefix
   */
  public void printParseArgument(PrintWriter out) {
    out.print("    case ");
    if(shortname.length()>1) 
	out.print(shortname);
    else {
	out.print("\'");
	out.print(shortname);
	out.print("\'");
    }
    out.println(":");

    out.print("      ");
    out.print(longname);
    out.print(" = ");
    if (argument == null) {
      out.print("!" + defaultValue);
      out.print(";");
    } else {
      String	methodName;
      String	argument = this.argument;

      if (type.equals("int")) {
	methodName = "getInt";
	if (argument == null || argument.equals("")) {
	  argument = "0";
	}
      } else {
	methodName = "getString";
	argument = "\"" + argument + "\"";
      }

      out.print(methodName + "(g, " + argument + ")");
      out.print(";");
    }
    out.println(" return true;");
  }

  /**
   * Prints the token definition to interface file (java)
   * @param	out		the output stream
   */
  public void printFields(PrintWriter out) {
    out.print("  public static ");
    out.print(type);
    out.print(" ");
    out.print(longname);
    out.print(" = ");
    if (!type.equals("String") || defaultValue.equals("null")) {
      out.print(defaultValue);
    } else {
      out.print("\"" + defaultValue + "\"");
    }
    out.println(";");
  }

  /**
   * Prints the token definition to interface file (java)
   * @param	out		the output stream
   */
  public void printUsage(PrintWriter out) {
    String	prefix ="\"  --";

    prefix += longname;
    if(shortname.length()<2) {
	prefix += ", -";
	prefix += shortname;
	if (argument != null) {
	    prefix += "<" + type + ">";
	}
    }
    prefix += ": ";
    for (int i = prefix.length(); i < 25; i++) {
      prefix += " ";
    }
    out.print(prefix + help);
    if (!defaultValue.equals("null")) {
      out.print(" [");
      out.print(defaultValue);
      out.print("]");
    }
    out.print("\"");
  }

  public void printLongOpts(PrintWriter out) {
    out.print("    new LongOpt(\"");
    out.print(longname);
    out.print("\", ");
    if (argument == null) {
      out.print("LongOpt.NO_ARGUMENT");
    } else if (argument.equals("")) {
      out.print("LongOpt.REQUIRED_ARGUMENT");
    } else {
      out.print("LongOpt.OPTIONAL_ARGUMENT");
    }
    out.print(", null, ");
    if(shortname.length()>1)
	out.print(shortname);
    else {
	out.print("\'");
	out.print(shortname);
	out.print("\'");
    }
    out.print(")");
  }

  public void printShortOption(PrintWriter out) {
      if(shortname.length()<2) {
	  out.print(shortname);
	  if (argument != null) {
	      if (argument.equals("")) {
		  out.print(":");
	      } else {
		  out.print("::");
	      }
	  }
      }
  }
    
  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private final String longname;
  private final String shortname;
  private final String type;
  private final String defaultValue;
  private final String argument;
  private final String help;
}
