package grapheditor;

public class NamePair {
    /** Name of the first node in an object. */
    String first;
    /** Name of the last node in an object. */
    String last;
    /** Create a new NamePair with both fields null. */
    public NamePair() { first = null; last = null; }
    /** Create a new NamePair with both fields the same. */
    public NamePair(String s) { first = s; last = s; }
    /** Create a new NamePair with two different names. */
    public NamePair(String f, String l) { first = f; last = l; }
}
