package streamit.library;

/**
 * This exception is thrown when a work function invocation neither
 * pushes nor pops anything.  It was introduced to detect when a given
 * source has been exhausted; if we are executing a pull schedule
 * (-nosched) we switch to pulling from a different sink when the
 * current sink experiences a NoPushPopException.
 */
class NoPushPopException extends RuntimeException {

    public NoPushPopException() { super(); }
    public NoPushPopException(String str) { super(str); }
}
