#!/usr/uns/bin/python

import re
import sys

def remove_extension( filename, extension ):
    return filename[ 0 : len( filename ) - len( extension ) - 1 ]

if len( sys.argv ) != 2:
    print( 'Supply a filename!' )
    sys.exit( -1 )

filename = sys.argv[1]
print( 'Adjusting: ' + filename )

top_level_name = remove_extension( filename, 'java' )

# print( remove_extension( filename, 'java' ) )
input = open( filename, 'r' )
lines = input.readlines()
input.close()

output = open( filename, 'w' )

def adjust_input_workfunction():
    class_declaration_linenum = 0
    pattern_input_filter = re.compile( r"(\s*class Input extends Filter)" )

    work_function_linenum = 0
    tell_toplevel_dump_string = 'if( i >= vertexDataSize )\n{\n\t'
    tell_toplevel_dump_string = tell_toplevel_dump_string + top_level_name + '.dumpFrameBuffers();\n}\n'
    
    # find filter
    for i in range( 0, len( lines ) ):
        if pattern_input_filter.match( lines[i] ):
            class_declaraction_linenum = i
            break

    # find work function
    for i in range( class_declaration_linenum + 1, len( lines ) ):
        if lines[i].find( 'public void work()' ) != -1:
            work_function_linenum = i
            break

    # insert dump statement
    lines.insert( work_function_linenum + 1, tell_toplevel_dump_string )

def adjust_raster_ops_constructor():
    class_declaration_linenum = 0
    pattern_raster_ops = re.compile( r"(\s*class RasterOps extends Filter)" )

    return_obj_linenum = 0
    add_self_string = '\t' + top_level_name + '.rasterOps.add( __obj );\n'

    # find class declaration
    for i in range( 0, len( lines ) ):
        if pattern_raster_ops.match( lines[i] ):
            class_declaration_linenum = i
            break

    # add dumpframebuffer method
    dump_framebuffer_string = \
                            """public void dumpFrameBuffer()
                            {
                            System.err.println( "dumping framebuffer: " + __param__param_offset );
                            for( int y = 0; y < __param__param_screenHeight; ++y )
                            {
                            for( int x = 0; x < width; ++x )
                            {
                            int xx = __param__param_offset + x * __param__param_numUnits;
                            System.out.println( "x = " + xx );
                            System.out.println( "y = " + y );
                            System.out.println( "r = " + red[ y * width + x ] );
                            System.out.println( "g = " + green[ y * width + x ] );
                            System.out.println( "b = " + blue[ y * width + x ] );
                            System.out.println( "eyeZ = " + eyeZBuffer[ y * width + x ] );
                            }
                            }
                            }
                            """

    lines.insert( i + 2, dump_framebuffer_string )

    # find "return __obj"
    for i in range( class_declaration_linenum + 1, len( lines ) ):
        if lines[i].find( 'return __obj;' ) != -1:
            # print( lines[i] )
            return_obj_linenum = i
            break

    # insert add self statement
    lines.insert( return_obj_linenum, add_self_string )

def add_toplevel_arraylist():

    top_level_pipeline_linenum = 0

    toplevel_string = '\tpublic static ArrayList rasterOps = new ArrayList();\n'
    toplevel_string = toplevel_string + '\tpublic static void dumpFrameBuffers()\n\t{\n'
    toplevel_string = toplevel_string + '\t\tfor( Iterator itr = rasterOps.iterator(); itr.hasNext(); )\n'
    toplevel_string = toplevel_string + '\t\t{\n\t\t\t( ( RasterOps )( itr.next() ) ).dumpFrameBuffer();\n'
    toplevel_string = toplevel_string + '\t\t}\n\t\tSystem.exit( 0 );\n\t}\n'
    
    # find top level pipeline
    for i in range( 0, len( lines ) ):
        if lines[i].find( top_level_name + ' extends StreamItPipeline' ) != -1:
            # print( lines[i] )
            top_level_pipeline_linenum = i
            break

    lines.insert( top_level_pipeline_linenum + 2, toplevel_string )

def insert_import_util():
    lines.insert( 0, 'import java.util.*;\n' )
    
insert_import_util()
adjust_input_workfunction()
adjust_raster_ops_constructor()
add_toplevel_arraylist()

def add_top_level_notify_method( numUnits ):
    class_declaration_linenum = 0
    regexp_toplevel = r"^(\s*public class " + top_level_name + ' extends StreamItPipeline)'
    pattern_toplevel = re.compile( regexp_toplevel )

    print( "toplevelname = " + top_level_name )
    
    # find toplevel class declaration
    for i in range( 0, len( lines ) ):
        if( pattern_toplevel.match( lines[i] ) ):
            class_declaration_linenum = i
            break

    toplevel_write = '\tpublic static int numNotifies = 0;\n'
    toplevel_write = toplevel_write + '\tpublic static void notifyDone()\n\t{\n'
    toplevel_write = toplevel_write + '\t\t++numNotifies;\n'
    toplevel_write = toplevel_write + '\t\tif( numNotifies == ' + str( numUnits ) + ' )\n'
    toplevel_write = toplevel_write + '\t\t{\n'
    toplevel_write = toplevel_write + '\t\t\tSystem.exit( -1 );\n'
    toplevel_write = toplevel_write + '\t\t}\n'
    toplevel_write = toplevel_write + '\t}\n'

    # find open brace
    open_brace_linenum = class_declaration_linenum + 1

    # insert stuff
    lines.insert( open_brace_linenum + 1, toplevel_write )

def adjust_raster_ops_work_function():
    class_declaration_linenum = 0
    pattern_input_filter = re.compile( r"class RasterOps extends Filter" )

    isDone_equals_true_linenum = 0
    pattern_isDone = re.compile( r"(\s*isDone = true)" )
            
    # find filter
    for i in range( 0, len( lines ) ):
        if( pattern_input_filter.match( lines[i] ) ):
            class_declaration_linenum = i
            break

    # find work function
    for i in range( class_declaration_linenum + 1, len( lines ) ):
        if( pattern_isDone.match( lines[i] ) ):
            isDone_equals_true_linenum = i
            break

    lines.insert( isDone_equals_true_linenum + 1, '\t' + top_level_name + '.notifyDone();\n' )

def replace_system_out_with_system_err():
    for i in range( 0, len( lines ) ):
        line = lines[i].rstrip().lstrip();
        if line.find( 'System.out.println((\"i = \" + i));' ) != -1:
            lines[i] = lines[i].replace( 'System.out.println', 'System.err.println' )
            return

#  print( 'replacing system.out with system.err' )
replace_system_out_with_system_err()
#  print( 'adding top level notify method' )
#  add_top_level_notify_method( 1 )
#  print( 'adjusting raster ops work function' )
#  adjust_raster_ops_work_function()

for line in lines:
    output.write( line )

output.close()
