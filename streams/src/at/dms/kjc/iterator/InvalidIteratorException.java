package at.dms.kjc.iterator;

/**
 * This exception represents a failure where one is trying to perform
 * an operation on an iterator that has become obsolete, i.e., the
 * root node has been replaced by a more recent version.
 */

class InvalidIteratorException extends RuntimeException {

    public InvalidIteratorException() { super(); }
    public InvalidIteratorException(String str) { super(str); }
    
}
