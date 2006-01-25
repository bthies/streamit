/**
 * This is a client to support CPLEX operations over RMI.
 */

package at.dms.kjc.linprog;

import java.rmi.*;
import java.rmi.server.*;

public class CPLEXClient extends UnicastRemoteObject implements Remote {
    public CPLEXClient() throws RemoteException {
        super();
    }

    public static double[] solveOverRMI(CPLEXSolve model) {
        try {
            // first get name to connect to
            RMISolver nameServer = (RMISolver)Naming.lookup(CPLEXServer.getBaseHostName());
            String openHost = nameServer.getOpenPort();
            // now solve the model with this host
            RMISolver solverServer = (RMISolver)Naming.lookup(openHost);
            double[] result = solverServer.solveOverRMI(model);
            // unbind the host we used
            nameServer.clearPort(openHost);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

