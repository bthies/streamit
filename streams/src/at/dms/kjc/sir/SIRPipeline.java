package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a pipeline of stream structures, as would be
 * declared with a Stream construct in StreaMIT.
 */
public class SIRPipeline extends SIRStream {
    /**
     * The elements of the pipeline.
     */
    private SIRStream[] elements;
}
