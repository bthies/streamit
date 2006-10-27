
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;

/**
 * Represents a pairwise constraint due to a latency constrained message
 * between a source and destination filters. Note that source will
 * have to send credit messages to the destination. Destination will
 * have to check credit messages and make sure it does not execute past
 * the credit received to guarantee a correct message delivery.
 */

public class LatencyConstraint {

    // int sourceInit

    // for downstream messages this stores how many times source 
    // (upstream node) can execute before it sends the first credit.

    // for upstream messages this stores the initial credit that
    // the destination (upstream node) receives.

    private int sourceInit;

    // steady state execution counts

    private int sourceSteadyExec, destSteadyExec; 
    
    private SIRFilter receiver;
    private int dependencyData[];

    /**
     * Constructs a latency constraint
     * @param sourceInit for downstream messages this represents
     * how many times source can execute before sending first credit.
     * For upstream messages this represents initial credit that
     * the receiver receives. Also creates a dependency data array
     * of size equal to sourceSteadyExec that must be set using
     * method setDependencyData.
     * @param sourceSteadyExec number of times source filter executes
     * in a steady state (this is not the steady state for whole program)
     * @param destSteadyExec number of times destination filter executes
     * in a steady state (this is not the steady state for whole program)
     */

    LatencyConstraint(int sourceInit, 
                      int sourceSteadyExec,
                      int destSteadyExec,
                      SIRFilter receiver) {
        this.sourceInit = sourceInit;
        this.sourceSteadyExec = sourceSteadyExec;
        this.destSteadyExec = destSteadyExec;
        this.receiver = receiver;
        dependencyData = new int[sourceSteadyExec];
        for (int t = 0; t < sourceSteadyExec; t++) {
            dependencyData[t] = 0;
        }
    }

    /**
     * A method for setting the dependency data. The size of dependency
     * array is equal to sourceSteadyExec. This represents what credit
     * can be sent by source during first steady state (For downstream message
     * the phase starts after sourceInit number of source iterations).
     * During subsequent phases source can send a credit that is by
     * destSteadyExec larger each time.
     * @param index index within dependency data array
     * @param value value of credit that can be sent during the first phase
     */

    public void setDependencyData(int index, int value) {
        dependencyData[index] = value;
    }

    /**
     * Returns a credit that can be sent by source to dest during first 
     * steady state.
     * @param index index within dependency array
     * @return the credit
     */
    
    public int getDependencyData(int index) {
        return dependencyData[index];
    }

    public SIRFilter getReceiver() {
        return receiver;
    }

    public int getSourceInit() {
        return sourceInit;
    }

    public int getSourceSteadyExec() {
        return sourceSteadyExec;
    }

    public int getDestSteadyExec() {
        return destSteadyExec;
    }

    public void output() {
        if (ClusterBackend.debugging) {
            System.out.println(" init: "+sourceInit+
                               " source: "+sourceSteadyExec+
                               " dest: "+destSteadyExec);
            System.out.print(" data: ");

            for (int t = 0; t < sourceSteadyExec; t++) {
                System.out.print(dependencyData[t]+" ");
                
            }
            
            System.out.print(" receiver id: "+NodeEnumerator.getSIROperatorId(receiver));
            System.out.println();
        }
    }
}
