/*
  NOTES/QUESTIONS:

  1. Is it ok to signify source and sink streams by indicating blocks
  that take zero input and output, respectively?  Alternatively, we
  could have separate class to simplify analysis.

  2. I replaced brackets with parentheses so that splitter and joiner
  blocks could cleanly output to multiple channels.  I hope this
  doesn't make reading from a block look too much like a function
  call?  x = this(1,3)

  3. Not considering splitters with multiple output types yet, joiners
  with multiple input types.  Should we add this?

  4. Making all input and output types a generic packet for now.
  Otherwise it becomes complicated to hold onto templated types inside
  of the pipe and parallel constructs, since we don't know what types
  we have.

  5. Should we add the means of specifying the input/output units of a
  pipe, parallel, or feedback loop section?  Presumably we can build
  these from those of the components.
  
  6. The operator overloading doesn't really work, since you can't
  have parentheses onto this, and brakckets can't take multiple
  arguments.  For now I just made them into function calls.

  */

// to indicate that nothing is input or output to a given channel
#define UNUSED -1

// here's a packet type
typedef void* Packet;

// class for 1-input, 1-output components
class Block {
public:
  // initiliaze this block before any data comes through
  virtual void initialize () = 0;
  // do the work for this block, taking input and producting output
  virtual void do_work() = 0;
  // construct a block who's input/output ratio should be easily analyzable
  Block();
  // optional constructor to specify input/output ratio.  That is, this 
  // block requires no more than <num_in> to execute do_work to completion, 
  // and outputs no more than <num_out> every time do_work is run
  Block(int num_in, int num_out);
  
protected:
  // for () appearing on the right hand side of equation (reading from
  // input).  Reading from (0) should signify the first element of the
  // input stream that hasn't been seen before, as of the start of a
  // call to do_work.  Negative number have been seen before; positive
  // numbers haven't.
  //Packet operator[] (const Packet &input) const;
  Packet input(int index);

  // for () appearing on the left hand side of equation (writing to
  // output).  Writing to (0) should signify the first element of the
  // output stream that hasn't been written before, as of the start of
  // a call to do_work.  Writing to negative numbers should be
  // illegal.  Writing to positive numbers indicates filling up buffer
  // with new values since the call to do_work.
  //Packet& operator() (const Packet &input);
  void output(int index, Packet output);
};

// component with one input stream and two or more output streams
class Splitter {
public:
  // initiliaze this block before any data comes through
  virtual void initialize () = 0;
  // do the work for this block, taking input and producting output
  virtual void do_work() = 0;
  // return number of outputs of this
  int get_num_outputs();
  // construct a splitter with <num_outputs> outputswho's input/output
  // ratio should be easily analyzable
  Splitter(int num_outputs);
  // optional constructor to specify input/output ratio.  That is,
  // this block requires no more than <num_in> to execute do_work to
  // completion, and outputs no more than <num_out_n> on output #n
  // every time do_work is run.
  Splitter(int num_outputs, 
	   int input_unit, 
	   int output_unit_0,
	   int output_unit_1,
	   int output_unit_2 = UNUSED,
	   int output_unit_3 = UNUSED,
	   int output_unit_4 = UNUSED);
  
protected:
  // for () appearing on the right hand side of equation (reading from
  // input).  Reading from (0) should signify the first element of the
  // input stream that hasn't been seen before, as of the start of a
  // call to do_work.  Negative number have been seen before; positive
  // numbers haven't.
  //Packet operator() (const Packet &input) const;
  Packet input(int index);

  // for () appearing on the left hand side of equation (writing to
  // output).  Writing to (n,0) should signify the first element of
  // the n'th output stream that hasn't been written before, as of the
  // start of a call to do_work.  Writing to negative numbers should
  // be illegal.  Writing to positive numbers indicates filling up
  // buffer with new values since the call to do_work.
  //Packet& operator() (int channel_num, const Packet &output);
  void output(int channel_num, int index, Packet output);
};

// component with one output stream and two or more input streams
class Joiner {
public:
  // initiliaze this block before any data comes through
  virtual void initialize () = 0;
  // do the work for this block, taking input and producting output
  virtual void do_work() = 0;
  // return number of inputs to this
  int get_num_inputs(int num_inputs);
  // construct a joiner with <num_inputs> inputs who's input/output
  // ratio should be easily analyzable
  Joiner(int num_inputs);
  // optional constructor to specify input/output ratio.  That is,
  // this block requires no more than <input_unit_n> on input #n to
  // execute do_work to completion, and outputs no more than <ouput_unit>
  // to its output channel every time do_work is run
  Joiner(int num_inputs, 
	 int output_unit, 
	 int input_unit_0,
	 int input_unit_1,
	 int input_unit_2 = UNUSED,
	 int input_unit_3 = UNUSED,
	 int input_unit_4 = UNUSED);
  
protected:
  // for () appearing on the right hand side of equation (reading from
  // input).  Reading from (n,0) should signify the first element of the
  // n'th input stream that hasn't been seen before, as of the start of a
  // call to do_work.  Negative numbers have been seen before;
  // positive numbers haven't.
  //Packet operator() (int channel_num, const Packet &input) const;
  Packet input(int channel_num, int index);

  // for () appearing on the left hand side of equation (writing to
  // output).  Writing to () should signify the first element of the
  // output stream that hasn't been written before, as of the start of
  // a call to do_work.  Writing to negative numbers should be
  // illegal.  Writing to positive numbers indicates filling up buffer
  // with new values since the call to do_work.
  //Packet& operator() (const Packet &output);
  void output(int index, Packet output);

};

// pipe class for cascading blocks into pipeline
class Pipe: public Block {
private:
  // the blocks that are piped in this
  Block* blocks[10];
  // blocks[num_blocks] is the last non-null component of <blocks>
  int num_blocks;

public:
  // initialize all the blocks in this
  void initialize() {
    // initialize piped components
    for (int i=0; i<num_blocks; i++) {
      blocks[i]->initialize();
    }
  }

  // do work 
  void do_work() {
    // it looks something like this... have all the components do work
    for (int i=0; i<num_blocks; i++) {
      blocks[i]->do_work();
    }
  }

  // consruct block pipeline.
  Pipe(Block* b0, 
       Block* b1 = 0,
       Block* b2 = 0,
       Block* b3 = 0,
       Block* b4 = 0,
       Block* b5 = 0,
       Block* b6 = 0,
       Block* b7 = 0,
       Block* b8 = 0,
       Block* b9 = 0
       ) {

    // initialize component blocks
    blocks[0] = b0;
    blocks[1] = b1;
    blocks[2] = b2;
    blocks[3] = b3;
    blocks[4] = b4;
    blocks[5] = b5;
    blocks[6] = b6;
    blocks[7] = b7;
    blocks[8] = b8;
    blocks[9] = b9;

    // determine how many non-null blocks we have
    num_blocks = 1;
    while (blocks[num_blocks] && num_blocks<10) {
      num_blocks++;
    }
  }

};

// parallel class for constructing split and join
class Parallel: public Block {
public:
  typedef enum {SPLIT_DUPLICATE, SPLIT_ROUND_ROBIN} SPLITTER_TYPE;
  typedef enum {JOIN_ROUND_ROBIN} JOINER_TYPE;
private:
  // the split block
  Splitter* splitter;
  // the join block
  Joiner* joiner;
  // the blocks that run in parallel between the split and join
  Block* blocks[10];
  // blocks[num_blocks] is the last non-null component of <blocks>
  int num_blocks;

public:
  // initialize all the blocks in this
  void initialize() {
    // initialize splitter & joiner
    splitter->initialize();
    joiner->initialize();
    // initialize parallel components
    for (int i=0; i<num_blocks; i++) {
      blocks[i]->initialize();
    }
  }
  
  // do work 
  void do_work() {
    // it looks something like this... have all the components do work
    splitter->do_work();
    for (int i=0; i<num_blocks; i++) {
      blocks[i]->do_work();
    }
    joiner->do_work();
  }

  Parallel(SPLITTER_TYPE s,
	   JOINER_TYPE j,
	   Block* b0 = 0,
	   Block* b1 = 0,
	   Block* b2 = 0,
	   Block* b3 = 0,
	   Block* b4 = 0,
	   Block* b5 = 0,
	   Block* b6 = 0,
	   Block* b7 = 0,
	   Block* b8 = 0,
	   Block* b9 = 0
	   ) {
    // identify function based on <s> and <j> and call other constructor
  }

  Parallel(SPLITTER_TYPE s,
	   Joiner* j,
	   Block* b0 = 0,
	   Block* b1 = 0,
	   Block* b2 = 0,
	   Block* b3 = 0,
	   Block* b4 = 0,
	   Block* b5 = 0,
	   Block* b6 = 0,
	   Block* b7 = 0,
	   Block* b8 = 0,
	   Block* b9 = 0
	   ) {
    // identify function based on <s> and call other constructor
  }

  Parallel(Splitter* s,
	   JOINER_TYPE j,
	   Block* b0 = 0,
	   Block* b1 = 0,
	   Block* b2 = 0,
	   Block* b3 = 0,
	   Block* b4 = 0,
	   Block* b5 = 0,
	   Block* b6 = 0,
	   Block* b7 = 0,
	   Block* b8 = 0,
	   Block* b9 = 0
	   ) {
    // identify function based on <j> and call other constructor
  }

  Parallel(Splitter* s,
	   Joiner* j,
	   Block* b0 = 0,
	   Block* b1 = 0,
	   Block* b2 = 0,
	   Block* b3 = 0,
	   Block* b4 = 0,
	   Block* b5 = 0,
	   Block* b6 = 0,
	   Block* b7 = 0,
	   Block* b8 = 0,
	   Block* b9 = 0
	   ) {

    // initialize component blocks
    splitter = s;
    joiner = j;
    blocks[0] = b0;
    blocks[1] = b1;
    blocks[2] = b2;
    blocks[3] = b3;
    blocks[4] = b4;
    blocks[5] = b5;
    blocks[6] = b6;
    blocks[7] = b7;
    blocks[8] = b8;
    blocks[9] = b9;

    // determine how many non-null blocks we have
    num_blocks = 1;
    while (blocks[num_blocks] && num_blocks<10) {
      num_blocks++;
    }
  }

};

class FeedbackLoop : public Block {
public:
  void initialize();
  void do_work();
  // initializes this with joiner <joiner> and body <body>.  Requires
  // that <joiner> take two inputs. The last block in <body> feeds
  // back to the second input of <joiner> with a delay of <delay>.
  // The input/output ratio from the feedback loop is therefore
  // governed by <joiner>.  The functional argument is used to
  // initialize the buffer in the feedback loop, given the delay and
  // the buffer.
  FeedbackLoop(Joiner* joiner, Block* body, int delay, void (int, Packet*));
};

////////////////////////////////////////////////////////////////////////////


class Filter : public Block {
  void initialize() {}
  void do_work() {
    int i = (int)input(0);
    output(0, (void*)(i < 10 ? i : 10));
  }
};

class Duplicator : public Splitter {
public:
  void initialize() {}
  void do_work() {
    int i = (int)input(0);
    output(0,0,(void*)i);
    output(0,1,(void*)i);
  }
  Duplicator() : Splitter(2) {}
  Duplicator(int num_outputs) : Splitter(num_outputs) {}
};

class Sinker : public Joiner {
public:
  void initialize() {}
  void do_work() {}
  Sinker() : Joiner(2) {}
  Sinker(int num_inputs) : Joiner(num_inputs) {}
};

int main() {
  Block* b1 = new Filter();
  Block* b2 = new Filter();
  Splitter* b3 = new Duplicator(2);
  new Pipe(b1, b2);
}
