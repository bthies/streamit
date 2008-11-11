#!/usr/uns/bin/python

import sys

filename = sys.argv[1]
print( 'Reading: ' + filename )
f = open( filename )
lines = f.readlines()
f.close()
print( 'Reading complete!' )

geometry = sys.argv[2]
g = geometry.split( 'x' )
width = int( g[0] )
height = int( g[1] )

print( 'size: ' + str( width ) + 'x' + str( height ) )

print( 'Allocating array...' )
sBuffer = [0] * ( width * height )
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
    x = int( l0[2] )
    y = int( l1[2] )
    s = int( l2[2] )
    sBuffer[ y * width + x ] = s;
    i = i + 3
print( 'Done!' )

output_filename = filename + '.arr'
f = open( output_filename, 'w' )

print( 'Writing to: ' + output_filename + ' (Read in for next pass)' )
i = 0.0;
milestone = 10.0
for y in range( 0, height ):
    for x in range( 0, width ):
        percent = ( 100 * i ) / ( width * height )
        if percent > milestone:
            print( str( percent ) + "%" )
            milestone = milestone + 10
        f.write( str( sBuffer[ y * width + x ] ) + '\n' )
        i = i + 1

f.close()

ppm_filename = filename + '.ppm'
f = open( ppm_filename, 'w' )

print( 'Writing to: ' + ppm_filename + ' (Visualization)' )

f.write( 'P3\n' )
f.write( str( width ) + ' ' + str( height ) + '\n' )
f.write( '255\n' )

i = 0.0;
milestone = 10.0
for y in range( 0, height ):
    for x in range( 0, width ):
        percent = ( 100 * i ) / ( width * height )
        if percent > milestone:
            print( str( percent ) + "%" )
            milestone = milestone + 10
        s = sBuffer[ y * width + x ]
        red = '0'
        green = '0'
        blue = '0'
        if s > 0:
            red = '255'
        elif s < 0:
            green = '255'
        f.write( red + ' ' + green + ' ' + blue + ' ' )
        i = i + 1
    f.write( '\n' )

f.close()

