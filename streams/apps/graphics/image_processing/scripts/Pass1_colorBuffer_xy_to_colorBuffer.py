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
red = [0] * ( width * height )
green = [0] * ( width * height )
blue = [0] * ( width * height )
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
    x = int( l0[2] )
    y = int( l1[2] )
    r = float( l2[2] )
    g = float( l3[2] )
    b = float( l4[2] )
    red[ y * width + x ] = r;
    green[ y * width + x ] = g;
    blue[ y * width + x ] = b;
    i = i + 5
print( 'Done!' )

colorBuffer_position = filename.find( '.colorBuffer' )
prefix = filename[ 0: colorBuffer_position ]

red_filename = prefix + '_red.colorBuffer.arr'
green_filename = prefix + '_green.colorBuffer.arr'
blue_filename = prefix + '_blue.colorBuffer.arr'

redFile = open( red_filename, 'w' )
greenFile = open( green_filename, 'w' )
blueFile = open( blue_filename, 'w' )

print( 'Writing to: ' + red_filename + ' (Read in for next pass)' )
print( 'Writing to: ' + green_filename + ' (Read in for next pass)' )
print( 'Writing to: ' + blue_filename + ' (Read in for next pass)' )
i = 0.0;
milestone = 10.0
for y in range( 0, height ):
    for x in range( 0, width ):
        percent = ( 100 * i ) / ( width * height )
        if percent > milestone:
            print( str( percent ) + "%" )
            milestone = milestone + 10
        redFile.write( str( red[ y * width + x ] ) + '\n' )
        greenFile.write( str( green[ y * width + x ] ) + '\n' )
        blueFile.write( str( blue[ y * width + x ] ) + '\n' )
        i = i + 1

blueFile.close()
greenFile.close()
redFile.close()

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
        r = str( int( 255.0 * red[ ( height - y - 1 ) * width + x ] ) )
        g = str( int( 255.0 * green[ ( height - y - 1 ) * width + x ] ) )
        b = str( int( 255.0 * blue[ ( height - y - 1 ) * width + x ] ) )
        f.write( r + ' ' + g + ' ' + b + ' ' )
        i = i + 1
    f.write( '\n' )

f.close()

