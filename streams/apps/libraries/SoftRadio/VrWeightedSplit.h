#ifndef __VR_WEIGHTED_SPLIT_H__
#define __VR_WEIGHTED_SPLIT_H__ 

#include <VrSigProc.h>

template <class dataType>
class VrWeightedSplit : public VrSigProc<dataType, dataType>
{
    int roundInput;
	int nOutputs;
	int *outputWeights;

public:

	VrWeightSplit () : roundInput (0), nOutputs (0), outputWeights (NULL),
   					   VrSigProc (0) { }

    void connect_dest (VrSigProc<dataType> *dst, int weight)
    {
		nOutputs++;
		outputWeights = (int*) realloc (outputWeights, nOutputs * sizeof (int));
		outputWeights [nOutputs - 1] = weight;
		roundInput += weight;

		// connect the output to this stream
		// first add a new output buffer to this filter
		{
			oldOutBuffer = outBuffer;
			outBuffer = new (VrBuffer<oType> *[nOutputs]);

			memcpy (outBuffer, oldOutBuffer, (nOutputs-1) * sizeof (dataType*));
			delete [] oldOutBuffer;

			outBuffer[i] = new VrBuffer<dataType>(this);
#ifdef PERFMON
			outBuffer[i]->us_cycles=cycles;
#endif
		}

		// setup the connection and increase the necessary buffering
		dst->connect (getOutputBufferN (nOutputs - 1));
		setHistory (roundInput);
    }

    void work (int n)
    {
		while (n >= roundInput)
		{
	        int dataLeft = 0;
        	int nOutput = 0;

			dataType *inputData = inputReadPtr (roundInput);

			for (dataLeft = roundInput, nOutput = 0; 

				dataLeft != 0;

				outputData += outputWeights [nOutput],
				dataLeft -= outputWeights [nOutput],
				nOutput++)
			{
		    	outputWrite (inputData, outputWeights [nOuptut]);
			}

			incInput (roundInput);

			n -= roundInput;
		}
    }
};

#endif // __VR_WEIGHTED_SPLIT_H__
