/*
 * Copyright 2007 by the Massachusetts Institute of Technology.
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
#ifndef __DYNTAPE
#define __DYNTAPE
#include <cstdlib>	// malloc
#include <string>	// memcpy
#include <cassert>      // assert
#include <cstdarg>      // va_* macros
/**
 * Representation of a tape as a dynamic buffer that can grow to meet
 * demands of upstream end.  Created with a pointer to the upstream work
 * function so that the buffer may be filled on demand.
 *
 * If the upstream worker tries to push more items than the buffer can fit,
 * the buffer will be expanded to accomodate them.
 *
 * If the downstream worker tries to pop an item off the tape and the tape 
 * is empty, the upstream worker is called repeatedly until it puts an item
 * on the tape.
 *
 * The implementation can fit n-1 items in a buffer of size n so it bumps
 * the caller's size estimate by 1.  
 * head == tail                ==> buffer is empty
 * head == tail -1  mod size   ==> buffer is full
 *
 * The program actually uses varargs for an arbitrary number of work functions
 * this is to allow variable rates to joiners so long as all connections to
 * the joiner are variable rate.
 */

/* Implementation note: buffer size is always power of 2 in number of elements
 * because &= is less expensive than %=.  If this creates too much waste, 
 * make a subclass to use %=.
 */

template <class T>
class dynTape {
  typedef void (*worker_type)(int); 
  typedef void (*preworker_type)(); 

  T* bufp;			/* pointer to buffer */
  int size;			/* current buffer size, power of 2, > 1 */
  int mask;			/* mask for modular arithmetic == size - 1 */
  int head;			/* position to push. */
  int tail;			/* position to pop */
  int num_workers;	        /* number of functions to call upstream */
  preworker_type* preworkers;   /* upstream prework functions */
  worker_type* workers;         /* upstream work functions */
  int* done_with_prework;       /* whether we have executed prework yet */

public:
 
  /**
   * Constructor: pass initial size estimate, and pass the upstream
   * prework and work function(s).
   */
  dynTape(int _size, int _num_workers, preworker_type f_prework, worker_type f_work, ...) {
    size = p2ceil(_size + 1);	// power of 2 && >= _size && > 1
    mask = size - 1;		// mask: all 1 bits.
    bufp = (T*)malloc(size * sizeof(T));	
    head = 0;			// head == tail ==> buffer is empty
    tail = 0;

    assert (_num_workers > 0);
    num_workers = _num_workers;
    preworkers = (preworker_type*)malloc(num_workers * sizeof(preworker_type));
    workers = (worker_type*)malloc(num_workers * sizeof(worker_type));
    done_with_prework = (int*)malloc(num_workers * sizeof(int));
    va_list va;
    va_start(va,f_work);
    
    preworkers[0] = f_prework;
    workers[0] = f_work;
    // if a prework is null, mark that we are already done with it
    if (preworkers[0] == NULL) {
        done_with_prework[0] = 1;
    } else {
        done_with_prework[0] = 0;
    }

    for (int i = 1; i < _num_workers; i++) {
      preworkers[i] = va_arg(va, void (*)());
      workers[i] = va_arg(va, void (*)(int));  
      // if a prework is null, mark that we are already done with it
      if (preworkers[i] == NULL) {
          done_with_prework[i] = 1;
      } else {
          done_with_prework[i] = 0;
      }
    }
    va_end(va);
  }


  /**
   * Default constructor if declared but not initialized.
   * Always use other constructor before using a dynTape.
   * This constructor was once used for creating a place holder.
   * TODO: Remove when no longer needed.
   */
  dynTape() {
  }

  /**
   * push an item onto the tape
   */
  inline void push(T item) {
    if ((tail == 0 && head == mask) || head == tail - 1) {
      makeRoom();
      push(item);
    } else {
      bufp[head++] = item;
      head &= mask;
    }
  }

  /**
   * Executes one iteration of the i'th upstream work or prework function.
   */
  inline void upstream_work(int i) {
      if (done_with_prework[i]) {
          // if done with prework, execute 1 iter of steady state
          (*(workers[i]))(1);
      } else {
          // otherwise execute prework
          (*(preworkers[i]))();
          done_with_prework[i] = 1;
      }
  }

  /**
   * pop an item from the tape
   */
  inline T pop() {
    while (head == tail) {	// while buffer empty
      for (int i = 0; i < num_workers; i++) {
          upstream_work(i);
      }
    }
    T tmp = bufp[tail++];
    tail &= mask;
    return tmp;
  }

  /**
   * pop N elements from buffer
   */
  inline void popN(int n) {
    for (int i = 0; i < n; i++) {
        while (head == tail) {	// while buffer empty
	  for (int i = 0; i < num_workers; i++) {
              upstream_work(i);
	  }
	}
	tail++;
	tail &= mask;
    }
  }

  inline T peek(int n) {
    // make sure sufficient in buffer.
    while ((head >= tail && head - tail <= n)
	   || size - tail + head <= n) {
      for (int i = 0; i < num_workers; i++) {
          upstream_work(i);
      }
    } 
    return bufp[(tail + n) & mask];
  }
private:

  /* power of 2 not < A, 2 is min for circular buffer to work.
   */
  int p2ceil(int A) {
    return ((A<=2)?2:((A<=4)?4:((A<=8)?8:((A<=16)?16:((A<=32)?32:((A<=64)?64:((A<=128)?128:((A<=256)?(256):(((A<=1024)?(1024):(((A<=4096)?(4096):(((A<=16384)?(16384):(((A<=65536)?(65536):(((A<=131072)?(131072):(((A<=262144)?(262144):(((A<=524288)?(524288):(((A<=1048576)?(1048576):(((A<=2097152)?(2097152):(((A<=4194304)?(4194304):(((A<=8388608)?(8388608):(((A<=16777216)?(16777216):(((A<=33554432)?(33554432):(((A<=67108864)?(67108864):(((A<=134217728)?(134217728):(((A<=268435456)?(268435456):(((A<=536870912)?(536870912):(1073741824)))))))))))))))))))))))))))))))))))))))))));
  }

  /* makeRoom is called when the buffer is full
   * (head - tail) % size == 1  if % always returned a non-negative number)
   * We need to enlarge the buffer.
   * To using masking for modular arithmetic, we require that the buffer
   * only grow by powers of 2.
   *
   * Cases: 
   *  head == tail - 1
   *    copy from tail to end of buffer into new buffer.
   *    follow by a copy from befinning of buffer to head.
   *  tail == 0 and head == size-1
   *    copy from tail to head into new buffer.
   */
  void makeRoom() {
    int newSize = size * 2;	// maintain size == power of 2
    T* newBufp = (T*)malloc(newSize * sizeof(T));
    if (newBufp ==0) {
      perror("malloc returned 0 in dynTape");
    }
    int afterCopy = 0;
    /* copy tail to end.  if head == size - 1 then copy one too many.*/
    memcpy(newBufp, &(bufp[tail]), (size - tail) * sizeof(T));
    if (head != mask) {
      /* copy beginning to head if have not done so already */
      memcpy(&((newBufp)[size - tail]), bufp, head * sizeof(T));
    }
    head = size - 1;
    tail = 0;
    bufp = newBufp;
    size = newSize;
    mask = size - 1;
  }
};
#endif
