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
 * $Id: CompilerMessages.msg,v 1.1 2001-08-30 16:32:31 thies Exp $
 */

{
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
 * $Id: CompilerMessages.msg,v 1.1 2001-08-30 16:32:31 thies Exp $
 */
}

package at.dms.compiler

prefix Compiler

// display pre-formatted messages as is
error	FORMATTED_ERROR
	"{0}"

caution	FORMATTED_CAUTION
	"{0}"

warning	FORMATTED_WARNING
	"{0}"

notice	FORMATTED_NOTICE
	"{0}"

info	FORMATTED_INFO
	"{0}"

// driver errors
error	NO_INPUT_FILE
	"No input file given"
	
error	FILE_NOT_FOUND
	"File \"{0}\" not found"

error	IO_EXCEPTION
	"I/O Exception on file \"{0}\": {1}"

error	UNSUPPORTED_ENCODING
	"Character encoding \"{0}\" is not supported on this platform"

// general errors

error	CANNOT_CREATE
	"Cannot create file \"{0}\""

error	INVALID_LIST_FILE
	"Invalid list file \"{0}\" : {1}"

// lexer and parser errors
error	NO_VIABLE_ALT_FOR_CHAR
	"Unexpected char \"{0}\""

error	UNEXPECTED_EOF
	"Unexpected end of file"

error	EOF_IN_TRADITIONAL_COMMENT
	"End of file in comment"

caution	EOF_IN_ENDOFLINE_COMMENT
	"End of file in comment"

error	ILLEGAL_CHAR
	"Unexpected char \"{0}\""

error	BAD_ESCAPE_SEQUENCE
	"Illegal escape sequence \"{0}\""

error	BAD_END_OF_LINE
	"Unexpected end of line in {0}"

error	SYNTAX_ERROR
	"Syntax error: {0}"

// compiler progress messages
info	COMPILATION_STARTED
	"[ start compilation in verbose mode ]"

info	FILE_PARSED
	"[ parsed {0} in {1} ms ]"

info	INTERFACES_CHECKED
	"[ checked interfaces in {0} ms ]"

info	BODY_CHECKED
	"[ checked body of {0} in {1} ms ]"

info	CLASSFILE_GENERATED
	"[ optimized and generated {0} in {1} ms ]"

info	JAVA_CODE_GENERATED
	"[ generated {0} ]"

info	CLASS_LOADED
	"[ loaded {0} ]"

info	COMPILATION_ENDED
	"[ compilation ended ]"

