#!/usr/uns/bin/python

import math
import sys

f = open( sys.argv[1] )
lines = f.readlines()
f.close()

class Vector3f:

    def __init__( self, x, y, z ):
        self.x = x
        self.y = y
        self.z = z

    def __str__( self ):
        return( '( ' + str( self.x ) + ', ' + str( self.y ) + ', ' + str( self.z ) + ' )' )
    
    def dot( self, v ):
        return( self.x * v.x + self.y * v.y + self.z * v.z )

    def add( self, v ):
        return Vector3f( self.x + v.x, self.y + v.y, self.z + v.z )

    def sub( self, v ):
        return Vector3f( self.x - v.x, self.y - v.y, self.z - v.z )

    def cross( self, v ):
        return Vector3f( self.y * v.z - self.z * v.y, self.z * v.x - self.x * v.z, self.x * v.y - self.y * v.x )

    def norm( self ):
        return math.sqrt( self.x * self.x + self.y * self.y + self.z * self.z )

    def normalize( self ):
        norm = self.norm()
        return Vector3f( self.x / norm, self.y / norm, self.z / norm )

    def scale( self, s ):
        return Vector3f( self.x * s, self.y * s, self.z * s )

    def serialize( self ):
        return( str( self.x ) + '\n' + str( self.y ) + '\n' + str( self.z ) + '\n' )

class Triangle3f:

    def __init__( self, v0, v1, v2 ):
        self.v0 = v0
        self.v1 = v1
        self.v2 = v2

    def __str__( self ):
        return( self.v0.__str__() + ' | ' + self.v1.__str__() + ' | ' + self.v2.__str__() )

    def normal( self ):
        e0 = self.v1.sub( self.v0 )
        e1 = self.v2.sub( self.v1 )
        return e0.cross( e1 )

    def serialize( self ):
        return( self.v0.serialize() + self.v1.serialize() + self.v2.serialize() )

#  class Vector4f:

#      def __init__( self, x, y, z, w ):
#          self.x = x
#          self.y = y
#          self.z = z
#          self.w = w

#      def __init__( self, v, w ):
#          self.x = v.x
#          self.y = v.y
#          self.z = v.z
#          self.w = w

#      def serialize( self ):
#          return( str( self.x ) + '\n' + str( self.y ) + '\n' + str( self.z ) + '\n' + str( self.w ) + '\n' )

#  class Triangle4f:

#      def __init__( self, v0, v1, v2 ):
#          self.v0 = v0
#          self.v1 = v1
#          self.v2 = v2

#      def serialize( self ):
#          return( self.v0.serialize() + self.v1.serialize() + self.v2.serialize() )

triangles = []

i = 0
while i < len( lines ):
    temp_lines = []
    for j in range( i, i + 9 ):
        temp_lines.append( float( lines[j].rstrip() ) )
    v0 = Vector3f( temp_lines[0], temp_lines[1], temp_lines[2] )
    v1 = Vector3f( temp_lines[3], temp_lines[4], temp_lines[5] )
    v2 = Vector3f( temp_lines[6], temp_lines[7], temp_lines[8] )
    triangles.append( Triangle3f( v0, v1, v2 ) )
    i = i + 9

light_cap = []
dark_cap = []
sides = []

light_position = Vector3f( 0.0, 3.0, 0.0 )

for t in triangles:
    normal = t.normal()
    light_vector = light_position.sub( t.v0 )
    # light cap
    if normal.dot( light_vector ) > 0:
        light_cap.append( t )
    # dark cap
    v0_minus_light = t.v0.sub( light_position ).normalize().scale( 4.0 )
    v1_minus_light = t.v1.sub( light_position ).normalize().scale( 4.0 )
    v2_minus_light = t.v2.sub( light_position ).normalize().scale( 4.0 )

    b0 = t.v0.add( v0_minus_light )
    b1 = t.v1.add( v1_minus_light )
    b2 = t.v2.add( v2_minus_light )
    
#    dark_cap.append( Triangle3f( b0, b1, b2 ) )
    # sides - todo: only silhouette edges?
    print( 't.v0 = ' + str( t.v0 ) )
    sides.append( Triangle3f( t.v0, b0, b1 ) )
    sides.append( Triangle3f( t.v0, b1, t.v1 ) )

    sides.append( Triangle3f( t.v1, b1, b2 ) )
    sides.append( Triangle3f( t.v1, b2, t.v2 ) )

    sides.append( Triangle3f( t.v2, b2, b0 ) )
    sides.append( Triangle3f( t.v2, b0, t.v0 ) )

print( 'Num Input Triangles: ' + str( len( triangles ) ) )
print( 'Light Cap # Triangles: ' + str( len( light_cap ) ) )
print( 'Dark Cap # Triangles: ' + str( len( dark_cap ) ) )
print( 'Sides # Triangles: ' + str( len( sides ) ) )

f = open( 'shadow_volume_geometry.va', 'w' )

for t in light_cap:
    f.write( t.serialize() )

#for t in dark_cap:
#    f.write( t.serialize() )

for t in sides:
    f.write( t.serialize() )

f.close()
