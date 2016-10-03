# StreamIt Input/Ouptut Traces for LaminarIR Benchmarks

This directory contains benchmarks and input/output traces that demonstrate StreamIt's ability to eliminate FIFO queues between filters.

## Benchmarks

We copied all benchmarks from the [LaminarIR source repository](https://github.com/LaminarIR/framework/tree/9c63f57fce2f4a61536c8407d91e2e17cc6ae933/examples/streamit/strs) (retrieved on Oct 4, 2016).  The only modifications to the benchmarks were as follows:
  * In JPEGFeed, some bugs were fixed and two feedback loops were converted to filters (see "current workarounds" below)
  * Stateful filters were declared with a "stateful" keyword, following the latest StreamIt specification.
  * Print statements were uncommented in order to simplify correctness checking.  (For performance comparisons, print statements should be removed.)
  * The input was randomized to avoid static evaluation.
  * fft2.256 was renamed to fft2_256

In addition, we included a copy of Figure 1 in the paper (`figure1`), an example of a peeking filter (`simple-peek`), and an example of a two-stage filter (`simple-twostage`).

## Elimination of FIFO Queues

The benchmarks were compiled with the latest StreamIt infrastructure (Oct 4, 2016) using the flags "-O2 -unroll 10000".  The file `logfile` contains a log of compiler messages, correctness checks, and checks for remaining FIFO queues in the generated code.

FIFO queues are completely eliminated from all programs, with the exception of RadixSort.  (While the `logfile` also highlights potential FIFOs in beamfomer and compcount, these are programmer generated and not targeted by either StreamIt or LaminarIR optimizations.)

For RadixSort, the StreamIt compiler and LaminarIR eliminate exactly the same buffers.  Both transformations eliminate the buffer between the source and the first Sort filter.  Neither transformation eliminates the buffers at the output of the Sort filters.  Because there are 11 Sort filters, there are 11 buffers remaining in the outputs of both StreamIt and LaminarIR.  (Interested readers can verify this as follows.  In the [StreamIt output]( https://github.com/bthies/streamit/blob/master/streams/apps/laminarir/radixsort/core0.c), the eliminated buffer has prefix POP_BUFFER_1, which is split into 16 scalar variables; the remaining buffers have prefixes POP_BUFFER_2 through POP_BUFFER_12.  In the [LaminarIR output](https://github.com/LaminarIR/framework/blob/9c63f57fce2f4a61536c8407d91e2e17cc6ae933/examples/laminarir/direct/RadixSort.rand.sdf), the eliminated buffer is between IntSource_1 and Sort_4, while assignments to the remaining buffers can be found by searching for `@y1 = out_array[0]`.)

## Current Workarounds

There is one benchmark (JPEG) that requires manual modification in order for the StreamIt compiler to match the results of LaminarIR.

The [JPEGFeed benchmark](https://github.com/LaminarIR/framework/blob/9c63f57fce2f4a61536c8407d91e2e17cc6ae933/examples/streamit/strs/JPEGFeed.str) from the LaminarIR repository was adapted from the prior [JPEG benchmark](https://github.com/bthies/streamit/blob/c5c273bfa011d846cc7b28b431ddff67cea3f977/streams/apps/benchmarks/jpeg/streamit/JPEG.str) in the StreamIt repository.  However, the LaminarIR authors made two changes.  First, they converted two-stage filters to feedback loops.  Second, they converted dynamic rates into static rates.  (There were some bugs introduced in this conversion:  the RunLengthDecoder still pushes a variable number of items, the loop bound in RunLengthEncoder should be "64-push_count" instead of "64-push_count+1", and the second push statement in DecoderFeed should be "push(peek(0))" instead of "push(peek(1))".  The first two bugs imply that the program is semantically invalid, and the benchmark cannot be used as-is.)

The workaround needed by the current StreamIt compiler is to convert two feedback loops to filters, since the StreamIt compiler does not yet fully optimize feedback loops.  We also avoided the bugs introduced by the LaminarIR authors by commenting out the RunLengthEncoder and RunLengthDecoder.  Our modifications appear as the [JPEGFilt benchmark]( https://github.com/bthies/streamit/blob/master/streams/apps/laminarir/jpeg/JPEGFilt.str).

It bears noting that there is nothing fundamental that prevents elimination of buffers in feedback loops.  Due to the rarity of feedback loops in real benchmarks, we never implemented fusion for feedback loops.  However, the fusion technique is exactly the same and the implementation would be very similar to that of [pipelines](https://github.com/bthies/streamit/blob/master/streams/src/at/dms/kjc/sir/lowering/fusion/ShiftPipelineFusion.java) and [splitjoins](https://github.com/bthies/streamit/blob/master/streams/src/at/dms/kjc/sir/lowering/fusion/FuseSplit.java).

## Prior Bug Fixes and Workarounds

After the publication of the LaminarIR paper, we learned from the LaminarIR authors that StreamIt optimizations caused compiler errors, runtime errors, and/or performance bugs for several of the benchmarks in this directory.  For every bug report, we provided a fix or workaround within a few days.  For historical interest, these fixes and workarounds were as follows:

### Prior bug fixes

   * [Bug fix 1](https://github.com/bthies/streamit/commit/0108886d9922a206d269277c1d68e9c951836294) on Jun 17, 2015.
   * [Bug fix 2](https://github.com/bthies/streamit/commit/1470669779869af1e061a9383c1d042503447c45) on Nov 13, 2015.
   * In 2016, there were additional fixes that removed the need for the workarounds below.

### Prior workarounds

For DES and Serpent, we previously recommended two manual modifications to the benchmarks.  These modifications are no longer needed, and have not been applied to the benchmarks in this directory.

   * We originally recommended moving a push statement outside of an if/else block (see [here](https://github.com/bthies/streamit/commit/e6ee24d6046b765a70fe6f358bebde5bc84c4777) and [here](https://github.com/bthies/streamit/commit/0779309b4571c92ead971717a3b20ce06f92ea59)).  Later, we found the bug was because the push statement was the only statement in a block.  We fixed this bug [here](https://github.com/bthies/streamit/commit/6aa069aabf9b395dbd2b670a4824d5a6c1c6d3b9).

   * We originally recommended converting static (global) constants into local constants.  (Global variables are a relatively recent feature in StreamIt, and not fully supported by all optimization passes.)  We later added support for constant propagation of global variables [here]( https://github.com/bthies/streamit/commit/d825b07eff9f674ebedc5458b68d42cb19ac7133).
