#ifndef __VR_WEIGHTED_SPLIT_H__
#define __VR_WEIGHTED_SPLIT_H__ 

#include <VrSigProc.h>

template <class dataType>
class VrWeightedSplit : public VrSigProc<dataType, dataType>
{
    int roundInput;
	int nInputs;
	int *outputWeights;

	VrWeightSplit () : roundInput (0), nInputs (0), outputWeights (NULL) { }

    void connect_dest (VrSigProc<dataType> *dst, int weight)
    {
		nInputs++;
		outputWeights = (int*) realloc (outputWeights, nInputs * sizeof (int));
		outputWeights [nInputs - 1] = weight;
		roundInput += weight;

		// connect the output to this stream
		dst->connect (getOutputBuffer ());
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
