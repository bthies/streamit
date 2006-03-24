/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library;

import java.lang.reflect.*;

/**
 * This class represents a message that is sent from one filter to
 * another using the out-of-band messaging system (it is not for
 * steady-state dataflow).  The messages are timed relative to data
 * items flowing in the stream, as formulated with the SDEP relation.
 * They invoke a void method in the receiver upon arriving.
 *
 * There is a distinct message for every receiver object.
 *
 * @version $Id: Message.java,v 1.6 2006-03-24 16:31:46 dimock Exp $
 */
public class Message {
    /**
     * The execution step of the receiver when this should be
     * delivered.  The message should be delivered to the receiver
     * immediately BEFORE it executes this iteration.
     */
    private int deliveryTime;
    /**
     * Whether or not the message travels downstream.
     */
    private boolean downstream;
    /**
     * The name of the message handler to be invoked.
     */
    private String methodName;
    /**
     * The arguments to be passed to the message handler.
     */
    private Object[] args;
    
    public Message(int _deliveryTime, boolean _downstream, String _methodName, Object[] _args) {
        this.deliveryTime = _deliveryTime;
        this.downstream = _downstream;
        this.methodName = _methodName;
        this.args = _args;
    }

    /**
     * Returns the execution step of the receiver BEFORE which this
     * should be delivered.
     */
    public int getDeliveryTime() {
        return deliveryTime - (isDownstream()?1:0);
    }

    /**
     * Returns whether or not the message travels downstream.
     */
    public boolean isDownstream() {
        return downstream;
    }

    /**
     * Delivers this message to <pre>receiver</pre>.  The receiver must
     * implement the appropriate method.
     */
    public void deliver(Object receiver) {
        try {
            //System.err.println("Delivering message " + methodName + " to " + receiver);
            // get parameter types
            Class[] paramTypes = new Class[args.length];
            for (int i=0; i<args.length; i++) {
                paramTypes[i] = extractPrimitive(args[i]);
            }
            // get receiver class
            Class receiverClass = receiver.getClass();
            // get receiver method
            Method receiverMeth = receiverClass.getMethod(methodName, paramTypes);
            // invoke method
            receiverMeth.setAccessible(true);
            receiverMeth.invoke(receiver, args);
        } catch (Exception e) {
            System.err.println("Message delivery failed while using reflection.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * If <pre>c</pre> represents a primitive type, return that primitive type.
     */
    public Class extractPrimitive(Object o) { 
        if (o instanceof Byte) {
            return Byte.TYPE;
        } else if (o instanceof Boolean) {
            return Boolean.TYPE;
        } else if (o instanceof Character) {
            return Character.TYPE;
        } else if (o instanceof Float) {
            return Float.TYPE;
        } else if (o instanceof Double) {
            return Double.TYPE;
        } else if (o instanceof Integer) {
            return Integer.TYPE;
        } else if (o instanceof Long) {
            return Long.TYPE;
        } else if (o instanceof Short) {
            return Short.TYPE;
        } else {
            return o.getClass();
        }
    }
}
