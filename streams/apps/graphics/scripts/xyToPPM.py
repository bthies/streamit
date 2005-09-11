#!/usr/local/bin/python

import sys

print( 'Reading: ' + sys.argv[1] )
f = open( sys.argv[1] )
lines = f.readlines()
f.close()
print( 'Reading complete!' )

geometry = sys.argv[2]
g = geometry.split( 'x' )
width = int( g[0] )
height = int( g[1] )

print( 'size: ' + str( width ) + 'x' + str( height ) )

print( 'Allocating array...' )
red = [0.0] * ( width * height )
green = [0.0] * ( width * height )
blue = [0.0] * ( width * height )
print( 'Done!' )

print( 'Populating arrays...' )
i = 0
milestone = 10.0
while i < len( lines ):
    percent = ( 100.0 * i ) / ( len( lines ) )
    if percent > milestone:
        print( str( percent ) + "%" )
        milestone = milestone + 10
    l0 = lines[i].rstrip().split( ' ' )
    l1 = lines[i+1].rstrip().split( ' ' )
    l2 = lines[i+2].rstrip().split( ' ' )
    l3 = lines[i+3].rstrip().split( ' ' )
    l4 = lines[i+4].rstrip().split( ' ' )
    try:
        x = int( l0[2] )
        y = int( l1[2] )
        r = float( l2[2] )
        g = float( l3[2] )
        b = float( l4[2] )
        red[ y * width + x ] = r
        green[ y * width + x ] = g
        blue[ y * width + x ] = b
    except:
        print( 'lines[i] = ' + lines[i] )
        print( 'lines[i+1] = ' + lines[i + 1] )
        print( 'lines[i+2] = ' + lines[i + 2] )
        print( 'l0 = ' + str( l0 ) )
        print( 'l1 = ' + str( l1 ) )
        print( 'l2 = ' + str( l2 ) )
        sys.exit()
    i = i + 5
    
print( 'Done!' )

output_filename = sys.argv[1] + '.ppm'
#f = open( sys.argv[1] + '.ppm', 'w' )
f = open( output_filename, 'w' )
f.write( 'P3\n' )
f.write( str( width ) + ' ' + str( height ) + '\n' )
f.write( '255\n' )

print( 'Writing PPM: ' + output_filename )
i = 0.0;
milestone = 10.0
for y in range( 0, height ):
    for x in range( 0, width ):
        percent = ( 100 * i ) / ( width * height )
        if percent > milestone:
            print( str( percent ) + "%" )
            milestone = milestone + 10
        f.write( str( int( 255.0 * red[ ( height - y - 1 ) * width + x ] ) ) + ' ' )
        f.write( str( int( 255.0 * green[ ( height - y - 1 ) * width + x ] ) ) + ' ' )
        f.write( str( int( 255.0 * blue[ ( height - y - 1 ) * width + x ] ) ) + ' ' )
        i = i + 1
    f.write( '\n' )

f.close()
