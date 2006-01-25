/**
 * This is a server to support CPLEX operations over RMI.
 */

package at.dms.kjc.linprog;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class CPLEXServer extends UnicastRemoteObject implements RMISolver {
    // base name of host
    private static final String hostname = "//cagfarm-40.lcs.mit.edu/RMISolver";
    // next open port to connect to
    private static long openPort = 0;

    public CPLEXServer() throws RemoteException {
        super();
    }

    public static String getBaseHostName() {
        return hostname + "-" + "base";
    }

    public String getOpenPort() throws RemoteException {
        openPort++;
        String name = hostname + "-" + openPort;
        bindServer(name);
        return name;
    }

    public void clearPort(String name) throws RemoteException {
        try {
            Naming.unbind(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns solution to <model>
     */
    public double[] solveOverRMI(CPLEXSolve model) throws RemoteException {
        try {
            return model.solve();
        } catch (LPSolverFailedException e) {
            throw new RemoteException(e.toString());
        }
    }

    public static void main(String[] args) {
        bindServer(getBaseHostName());
    }

    private static void bindServer(String name) {
        try {
            RMISolver server = new CPLEXServer();
            Naming.rebind(name, server);
            System.out.println("Bound new CPLEXServer:  " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
