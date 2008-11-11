#!/usr/uns/bin/python

# takes colorBuffer.ppm
# makes a bunch of pngs and ppms dividing it up

import os

rows = 2

filename = 'colorBuffer.ppm'

kernelSize = 3
halfKernelSize = kernelSize / 2
padding = 2 * halfKernelSize;

class Image:

    def __init__( self, filename, width, height ):
        if filename == "":
            self.width = width
            self.height = height
            self.red = [0] * ( width * height )
            self.green = [0] * ( width * height )
            self.blue = [0] * ( width * height )

        else:
            f = open( filename, 'r' )
            lines = f.readlines()
            f.close()
            
            ( self.width, self.height ) = lines[1].rstrip().split()
            
            self.width = int( self.width )
            self.height = int( self.height )
            
            self.red = [0] * ( self.width * self.height )
            self.green = [0] * ( self.width * self.height )
            self.blue = [0] * ( self.width * self.height )
            
            milestone = 10
            i = 3
            while i < len( lines ):
                line = lines[i].rstrip()
                tokens = line.split()
                y = i - 3
                if ( 100.0 * y / self.height ) > milestone:
                    print( str( 100.0 * y / self.height ) + '%' )
                    milestone = milestone + 10
                j = 0
                while j < ( len( tokens ) - 3 ):
                    # print( 'token length = ' + str( len( tokens ) ) )
                    x = j / 3
                    self.red[ y * self.width + x ] = int( tokens[j] )
                    self.green[ y * self.width + x ] = int( tokens[j+1] )
                    self.blue[ y * self.width + x ] = int( tokens[j+2] )
                    j = j + 3
                i = i + 1;

    def get( self, x, y ):
        if x < 0:
            x = 0
        elif x >= self.width:
            x = self.width - 1
        if y < 0:
            y = 0
        elif y >= self.height:
            y = self.height - 1
            
        r = self.red[ y * self.width + x ]
        g = self.green[ y * self.width + x ]
        b = self.blue[ y * self.width + x ]
        return ( r, g, b )

    def set( self, x, y, r, g, b ):
        self.red[ y * self.width + x ] = r
        self.green[ y * self.width + x ] = g
        self.blue[ y * self.width + x ] = b

    def savePPM( self, filename ):
        f = open( filename, 'w' )
        f.write( 'P3\n' )
        f.write( str( self.width ) + ' ' + str( self.height ) + '\n' )
        f.write( '255\n' )
        for y in range( 0, self.height ):
            for x in range( 0, self.width ):
                ( r, g, b ) = self.get( x, y )
                f.write( str( r ) + ' ' + str( g ) + ' ' + str( b ) )
                if x != self.width - 1:
                    f.write( ' ' )
            f.write( '\n' )

        f.close()

def makeTile( fullRes, tileX, tileY, tileWidth, tileHeight ):
    startX = tileX * tileWidth;
    startY = tileY * tileHeight;
    tileImage = Image( "", tileWidth + padding, tileHeight + padding )

    for y in range( 0, tileHeight + padding ):
        for x in range( 0, tileWidth + padding ):
            fullResX = startX + x - halfKernelSize
            fullResY = startY + y - halfKernelSize
            ( r, g, b ) = fullRes.get( fullResX, fullResY )
            tileImage.set( x, y, r, g, b )
            
    return tileImage


fullRes = Image( filename, 0, 0 )
numTiles = rows * rows
tiles = [0] * numTiles

tileWidth = fullRes.width / rows
tileHeight = tileWidth

for y in range( 0, rows ):
    for x in range( 0, rows ):
        print( 'Computing tile: (' + str( x ) + ', ' + str( y ) + ')' )
        tiles[ y * rows + x ] = makeTile( fullRes, x, y, tileWidth, tileHeight )
        print( 'Writing tile: (' + str( x ) + ', ' + str( y ) + ')' )
        name = 'tile_' + str( x ) + '_' + str( y )
        ppmName = name + '.ppm'
        pngName = name + '.png'
        tiles[ y * rows + x ].savePPM( ppmName )
        os.system( 'convert ' + ppmName + ' ' + pngName )
