package streamit.eclipse.grapheditor.editor.utils;

import java.awt.geom.Point2D;

/**
 * @author winkler
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */


public abstract class MathExtensions {

/******************************************************************************/
/**
 * Extracts the leading sign of x.
 * 
 * @param x Any double value.
 * @return If x has a positive value <code>-1.0</code>, for <code>x = 0.0</code>
 * here comes <code>0.0</code> and if x has a negative the method returns 
 * <code>-1.0</code>.
 */
    public static double sgn(double x){
        if( x < 0.0 ) {
            return -1.0;
        }
        else if( x > 0.0 ){
            return 1.0;
        }
        else {
            return 0.0;
        }               
    }

/******************************************************************************/
/**
 * Computes the absolute value of <code>v</code>. Assuming <code>v</code> 
 * is a mathematical Vector, pointing from Point Zero to the Point, represented
 * by <code>x</code> and <code>y</code> in v, then this method returns the
 * length of v.
 * <p><blockquote><blockquote><code>
 * return sqrt( v.x² + v.y² )
 * </code></blockquote></blockquote>
 * @param v Point the Vector is pointing at, coming from the point 
 * <code>(0;0)</code>.
 * @return Length of the mathematical Vector v, computed by Pytagoras's 
 * theorem.
 */
    public static double abs(Point2D.Double v){
        return Math.sqrt(getTransposed(v,v));
    }

/******************************************************************************/
/**
 * Computes the absolute value of a Vector, running from the
 * Point <code>(0;0)</code> to the Point <code>(x;y)</code>. This is the length
 * of that Vector.
 * <p><blockquote><blockquote><code>
 * return sqrt( v.x² + v.y² )
 * </code></blockquote></blockquote>
 * @param x Length of one Karthese. Between x and y is an Angle of 90°
 * @param y Length of the other Karthese. Between x and y is an Angle of 90°
 * @return Length of the Hypothenuse.
 */
    public static double abs(double x, double y){
        return (double) Math.sqrt( (x*x) + (y*y) );
    }

/******************************************************************************/
/**
 * Calculates the angle between v1 and v2. Assuming that v1 and v2 are
 * mathematical Vectors, leading from the Point <code>(0;0)</code> to
 * their coordinates, the angle in <code>(0;0)</code> is calculated.
 * <p><blockquote><blockquote><code>
 * return arccos( ( v1.x*v2.x + v1.y*v2.y ) / ( sqrt( v1.x² + v1.y² ) * 
 * sqrt( v2.x² + v2.y² ) ) )
 * </code></blockquote></blockquote>
 * @param v1 One of two Vectors leading from <code>(0;0)</code> to 
 * <code>(v1.x;v1.y)</code>
 * @param v2 One of two Vectors leading from <code>(0;0)</code> to 
 * <code>(v2.x;v2.y)</code>
 * @return The Angle between the two vectors
 */ 
    public static double angleBetween(Point2D.Double v1, Point2D.Double v2){
        double xty = getTransposed(v1,v2);
        double lx  = Math.sqrt(getTransposed(v1,v1));
        double ly  = Math.sqrt(getTransposed(v2,v2));
        double result = xty/(lx*ly);
        if( result > 1.0 ) result = 1.0;   //gleicht rundungsfehler aus
        if( result < -1.0 ) result = -1.0; //gleicht rundungsfehler aus
        return Math.acos(result);
    }

/******************************************************************************/
/**
 * Calculates the Transposed of v1 and v2. It is assumed, that v1 and v2 are
 * mathematical Vectors, leading from the Point <code>(0;0)</code> to
 * their coordinates.
 * <p><blockquote><blockquote><code>
 * return v1.x * v2.x + v1.y * v2.y
 * </code></blockquote></blockquote>
 * @param v1 Vector, leading from <code>(0;0)</code> to the coordinates of
 * the point.
 * @param v2 Vector, leading from <code>(0;0)</code> to the coordinates of
 * the point.
 * @return Transposed from v1 and v2.
 */
    public static double getTransposed(Point2D.Double v1, Point2D.Double v2){
        return v1.getX() * v2.getX() + v1.getY() * v2.getY();
    }

/******************************************************************************/
/**
 * Returns the euclidean Distance between two Points in a 2D cartesian 
 * coordinate system. The euclidean Distance is calculated in the following way:
 * <p>
 * <blockquote><blockquote><code>
 * sqrt( (p1.x - p2.x)² + (p1.y - p2.y)² )
 * </code></blockquote></blockquote>
 * @param p1 First of two Points, the Distance should be calculated between.
 * @param p2 Second of two Points, the Distance should be calculated between.
 * @return Distance between p1 and p2, calculated in the euclidean way.
 */
    public static double getEuclideanDistance(Point2D.Double p1, 
                                                Point2D.Double p2){
                                                    
        return Math.sqrt(((p1.x-p2.x)*(p1.x-p2.x))+((p1.y-p2.y)*(p1.y-p2.y)));
    }
    
/******************************************************************************/

    public static Point2D.Double getNormalizedVector(Point2D.Double v){
        double length = abs(v);
        return new Point2D.Double(v.x / length, v.y / length );
    }

/******************************************************************************/

    

}
