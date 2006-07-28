/**
 * 
 */
package at.dms.kjc.common;

import java.util.HashMap;
import java.util.Random;

/**
 * This is a abstract class that any simulated annealing assignment
 * algorithm can inherit and use. 
 * 
 * @author mgordon
 *
 */
public abstract class SimulatedAnnealing {

    private Random random;
       
    // simualted annealing constants
    public static int MINTEMPITERATIONS = 200;

    public static int MAXTEMPITERATIONS = 200;

    public static int ANNEALITERATIONS = 10000;

    public static double TFACTR = 0.9;
    
    /** the assignment that we arrive at and use during execution */
    protected HashMap assignment;

    protected SimulatedAnnealing() {
        assignment = new HashMap();
        random = new Random(17);
    }
    
    /**
     * Use this method to print stats for the layout...
     *
     */
    public  void printLayoutStats() {
        
    }
    
    /** 
     * Use this function to reassign the assignment to <newAssign>, update
     * anything that needs to be updated on a new assignment.
     * @param newAssign
     */
    public abstract void setAssignment(HashMap newAssign);
    
    /**
     * Called by perturbConfiguration() to perturb the configuration.
     * perturbConfiguration() decides if we should keep the new assignment.
     * 
     */
    public abstract void swapAssignment();
    
    /**
     * The initial assignment, this must set up 
     * assignment so that is can be called by 
     * placemenCost() and perturbConfiguration(T);
     *
     */
    public abstract void initialPlacement();
    
    /**
     * The placement cost (energy) of the configuration.
     * 
     * @param debug Might want to do some debugging...
     * @return placement cost
     */
    public abstract double placementCost(boolean debug); 
    
    /** 
     * Perform any initialization that has to be done before
     * simulated annealing. This does not include the initial placement. 
     *
     */
    public abstract void initialize();
    
    /**
     * Set the annealing constants.
     * 
     * @param minTempIts
     * @param maxTempIts
     * @param annealIts
     * @param tFactor
     */
    protected final void setConstants(int minTempIts, int maxTempIts, 
            int annealIts, double tFactor) {
        
        MINTEMPITERATIONS = minTempIts;
        MAXTEMPITERATIONS = maxTempIts;
        ANNEALITERATIONS = annealIts;
        TFACTR = tFactor;
    }
    
    /**
     * Run the simulated annealing assignment.
     * 
     * @param iterations The number of simulated annealing iterations to
     * run.
     * @param nover The number of paths tried at an iteration (It was 100 before).
     */
    public final void simAnnealAssign(int iterations, int nover) {
        System.out.println("Simulated Annealing Assignment");
        int nsucc = 0, j = 0;
        double currentCost = 0.0, minCost = 0.0;
        // number of paths tried at an iteration
        

        try {
            
//          initialize
            initialize();
            
            // create an initial placement
            initialPlacement();

            int configuration = 0;

            currentCost = placementCost(false);
            assert currentCost >= 0.0;
            System.out.println("Initial Cost: " + currentCost);
            double lastInitialCost = currentCost;
         
            // as a little hack, we will cache the layout with the minimum cost
            // these two hashmaps store this layout
            HashMap assignMin = (HashMap) assignment.clone();
            minCost = currentCost;

            if (currentCost == 0.0) {
                return;
            }

            // The first iteration is really just to get a
            // good initial layout. Some random layouts really kill the
            // algorithm
            for (int two = 0; two < iterations; two++) {
                if (two > 0 && lastInitialCost == currentCost) {
                    System.out.println("Not using last layout.");
                    initialPlacement();
                    currentCost = placementCost(false);
                }
                lastInitialCost = currentCost;
                
                System.out.print("\nRunning Annealing Step (" + currentCost
                                 + ", " + minCost + ")");
                double t = annealMaxTemp();
                double tFinal = annealMinTemp();
                while (true) {
                    int k = 0;
                    nsucc = 0;
                    for (k = 0; k < nover; k++) {
                        // true if config change was accepted
                        boolean accepted = perturbConfiguration(t);
                        currentCost = placementCost(false);
                        // the layout should always be legal
                        assert currentCost >= 0.0 : currentCost;

                        if (accepted) {
                            nsucc++;
                            configuration++;
                            //System.out.println(currentCost);
                        }
                        
                        if (configuration % 500 == 0)
                            System.out.print(".");
                          
                        // keep the layout with the minimum cost
                        // or if this layout has the same minimun cost
                        // call keepNewEqualMin to decide if we should keep it
                        if (currentCost < minCost) { 
                            minCost = currentCost;
                            // save the layout with the minimum cost
                            assignMin = (HashMap)assignment.clone();
                        } 
                       

                        // this will be the final layout
                        if (currentCost == 0.0)
                            break;
                    }

                    t *= TFACTR;

                    if (nsucc == 0)
                        break;
                    if (currentCost == 0.0)
                        break;
                    if (t <= tFinal)
                        break;
                    j++;
                }
                if (currentCost == 0)
                    break;
            }

            currentCost = placementCost(false);
            System.out.println("\nFinal Cost: " + currentCost + " Min Cost : "
                               + minCost + " in  " + j + " iterations.");
            if (minCost < currentCost) {
                setAssignment(assignMin);
                currentCost = minCost;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Decide if we should keep a configuration that has a 
     * cost that is EQUAL to the current minimum of the search.  
     * By default, don't keep it. Override if you want other behavior.
     * 
     * @return Should we set the min config to this config (they have the same
     * cost). 
     */
    protected abstract boolean keepNewEqualMin();
        
    
    private final double annealMaxTemp() throws Exception {
        double T = 1.0;
        int total = 0, accepted = 0;
        HashMap assignInit = (HashMap)assignment.clone();

        for (int i = 0; i < MAXTEMPITERATIONS; i++) {
            T = 2.0 * T;
            total = 0;
            accepted = 0;
            for (int j = 0; j < 100; j++) {
                // c_old <- c_init
                assignment = (HashMap)assignInit.clone();
                if (perturbConfiguration(T))
                    accepted++;
                total++;
            }
            if (((double) accepted) / ((double) total) > .9)
                break;
        }
        // c_old <- c_init
        assignment = (HashMap)assignInit.clone();
        return T;
    }

    private final double annealMinTemp() throws Exception {
        double T = 1.0;
        int total = 0, accepted = 0;
        HashMap assignInit = (HashMap)assignment.clone();

        for (int i = 0; i < MINTEMPITERATIONS; i++) {
            T = 0.5 * T;
            total = 0;
            accepted = 0;
            for (int j = 0; j < 100; j++) {
                // c_old <- c_init
                assignment = (HashMap)assignInit.clone();
                if (perturbConfiguration(T))
                    accepted++;
                total++;
            }
            if (((double) accepted) / ((double) total) > .1)
                break;
        }
        // c_old <- c_init
        assignment = (HashMap)assignInit.clone();
        return T;
    }
    
   
    
    /**
     * Perturb the configuration (assignment) and decide if we are going
     * to keep the perturbation.
     * 
     * @param T the current anneal temperature.
     * @return
     */
    private final boolean perturbConfiguration(double T) {
        // the cost of the new layout and the old layout
        double e_new, e_old = placementCost(false);
        //the old assignment
        HashMap oldAssignment = (HashMap)assignment.clone();
        // find 2 suitable nodes to swap
        while (true) {
            swapAssignment();
            //get the new placement codes
            e_new = placementCost(false);

            if (e_new < 0.0) {
                // illegal tile assignment so revert the assignment
                setAssignment((HashMap)oldAssignment.clone());
                continue;
            } else
                // found a successful new layout
                break;
        }

        double P = 1.0;
        double R = random.nextDouble();

        //System.out.println(" old: " + e_old + " new: " + e_new);
        
        if (e_new > e_old)
            P = Math.exp((((double) e_old) - ((double) e_new)) / T);

        if (R < P) {
            return true;
        } else {
            //don't accept the new state,
            //revert..
            //System.out.println("  Don't accept: calling set assignment()");
            setAssignment(oldAssignment);
            return false;
        }
    }
    
    /**
     * Get the nex random number from [0, max).
     * 
     * @param max
     * @return next random int in range
     */
    public int getRandom(int max) {
        return random.nextInt(max);
    }

}
