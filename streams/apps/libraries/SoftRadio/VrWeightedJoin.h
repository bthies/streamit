#ifndef __VR_WEIGHTED_JOIN_H__
#define __VR_WEIGHTED_JOIN_H__

#include <VrSigProc.h>

template <class dataType>
class VrWeightedJoin : public VrSigProc<dataType, dataType>
{
    int roundOutput;
	int nInputs;
	int nInput;
	int *inputWeights;

public:

	VrWeightJoin () : roundOutput (0), nInputs (0), inputWeights (NULL),
   					  nInput (0) { }

    void connect_dest (VrSigProc<dataType> *src, int weight)
    {
		nInputs++;
		inputWeights = (int*) realloc (inputWeights, nInputs * sizeof (int));
		inputWeights [nInputs - 1] = weight;
		roundInput += weight;

		// connect the output to this stream
		connect (src->getOutputBuffer ());
		setHistoryN (nInputs - 1, weight);
    }

    void work (int n)
    {
		while (validUnitsN (nInput) >= inputWeights [nInput])
		{
			dataType *data = inputReadN (nInput, inputWeights [nInput]);
			outputWrite (data, inputWeights [nInput]);
			incInputN (nInput, inputWeights [nInput]);

			nInput++;
			if (nInput == nInputs) nInput = 0;
		}
	}
};

#endif // __VR_WEIGHTED_JOIN_H__
