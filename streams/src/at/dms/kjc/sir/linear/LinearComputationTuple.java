package at.dms.kjc.sir.linear;




/** class that represents tuples of (input position, coefficent)
 * which represent the terms that are calculated to produce linear output. **/
public class LinearComputationTuple {
    private int position;
    private ComplexNumber coefficient; 
    /** make a new tuple with the specified input position and coefficient. **/
    LinearComputationTuple(int inputPosition, ComplexNumber computationCoefficient) {
	this.position = inputPosition;
	this.coefficient = computationCoefficient;
    }
    /** two tuples are equal if their position and coefficient are equal. **/
    public boolean equals(Object o) {
	if(!(o instanceof LinearComputationTuple)) {return false;}
	LinearComputationTuple other = (LinearComputationTuple)o;
	return ((this.position == other.position) &&
		(this.coefficient.equals(other.coefficient)));
    }
    /** reimplement hashcode so that if two tuples are equal, their
     * hashcodes are also equal. **/
    public int hashCode() {
	return this.position;
    }
    /** pretty print **/
    public String toString() {
	return ("<" + this.position +
		"," + this.coefficient + ">");
    }
}
