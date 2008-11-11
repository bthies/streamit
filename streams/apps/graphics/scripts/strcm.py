#!/usr/uns/bin/python

import re
import sys

def load_array( array_name, array_file_name, out ):
	try:
		array_file = open( array_file_name, 'r' )
		in_lines = array_file.readlines()
		array_file.close()
		in_lines = [ s.rstrip() for s in in_lines ]
		array_length = len( in_lines )
		out.write( '\tfloat[' + str( array_length ) + '] ' + array_name + ' =\n\t{\n' )
		for i in range( 0, array_length - 1 ):
			out.write( '\t\t' + in_lines[i] + ',\n' )
		out.write( '\t\t' + in_lines[ array_length - 1 ] + '\n\t};' )
	except IOError:
		print( 'Unable to open file: ' + array_file_name )

def process_input_file( f, out ):
	in_lines = f.readlines()
	in_lines = [ s.rstrip() for s in in_lines ]
	for line in in_lines:
		out.write( line + '\n' )
		m = load_array_pattern.match( line )
		if m:
			remainder = line[ m.end(): ]
			remainder_split = remainder.split( ' ' )
			array_name = remainder_split[0]
			array_file_name = remainder_split[1]
			print( 'Loading array: ' + array_name + ' from '\
			       + array_file_name )
			load_array( array_name, array_file_name, out )

# Loading Array
load_array_pattern = re.compile( r"(\s)*// LOADARRAY " )

f = open( sys.argv[1], 'r' )
lines = f.readlines()
f.close()

if len( lines ) <= 1:
	print( 'Insufficient number of inputs' )
	exit( -1 )

lines = [ s.rstrip() for s in lines ]
output_filename = lines[0]

out = open( output_filename, 'w' )
for filename in lines[ 1: len( lines ) ]:
	try:
		if filename != '':
			print( 'Processing: ' + filename )
			f = open( filename, 'r' )
			out.write( '\n\n\n// ############### ' );
			out.write( filename );
			out.write( ' ###############\n' );
			process_input_file( f, out )
			f.close()
	except IOError:
		print( 'Unable to open: ' + filename )

out.close()

print( "Done!" )
