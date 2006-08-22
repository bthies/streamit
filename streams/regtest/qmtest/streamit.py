
#
# streamit.py: Python extensions to QMTest for StreamIt
# original author    David Maze <dmaze@cag.lcs.mit.edu>
# maintained by      Allyn Dimock <dimock@csail.mit.edu>
# $Id: streamit.py,v 1.30 2006-08-22 16:43:38 dimock Exp $
#

# TODO: implement own_output to spec:
#   build_regtest.py should only define own_output if in the .xml
#   streamit.py should check if own_output is defined.  If not, just
#   use file "output" to communicate stdout from run phase to verification.
#   If defined, then do not write stdout to a file in run phase and open
#   own_output file for read in verification.


# This file just defines some extra test classes that QMTest can use.
# For now it assumes the standard XML database class, though it doesn't
# explicitly require it.

import os
import os.path
import qm.executable
from   qm.fields import *
import qm.test.test
# not used that I can see.  import signal
# not used that I can see.  import threading
import re
import socket
import shutil
#from time import sleep
import time
import traceback


TIMEOUT = 20 * 60

def dbs_test_to_dir(path_to_dbs,dotted_test_name):
    """Translate path to QMTest ExtensionDatabase and test name to a os.path.

    'path_to_dbs' is path to the ExtensionDatabase.

    'dotted_test_name' is the test name in the database."""
    
    extn = '.qms' #qm.test.file_database.ExtensionDatabase.GetSuiteExtension(?)
    # split, remove test name from directory path
    test_split = dotted_test_name.split('.')[:-1]
    parts = map(lambda p: p + extn, test_split)
    path = os.path.join(path_to_dbs, *parts)
    return path

def context_to_dir(context):
    """Given a QMTest 2.3 context, return a path to the current test directory.

    returns None if can not determine path from context (as in QMTest 2.0)."""

    dbpath = context.get('qmtest.dbpath')
    testid = context.get('qmtest.id')
    if testid:
        return dbs_test_to_dir(dbpath, testid)

    return None

class BackendField(EnumerationField):
    """A field containing a StreamIt compiler backend."""

    backend_names = ['Uniprocessor', 'Library', 'RAW 4x4', 'Cluster 1', 'simpleC']
    backend_tags = ['uni', 'library', 'raw4', 'cluster', 'simpleC']    

    # TODO: think about some way to present the backend_names
    # to the user, but use the backend_tags internally.
    def __init__(self, name, default_value=None, **properties):
        EnumerationField.__init__(self, name, default_value,
                                  self.backend_tags, **properties)

def InterpretExitCode(result, status, expected, component):
    """Interpret the exit code of a program and populate 'result'.

    Populate a QMTest 'result' object, as is passed into a
    'Test' object's 'Run' method.

    'result' -- Result object to populate.

    'status' -- Exit status of the program.

    'component' -- Name of the component to label messages with."""

    # Figure out what happened; crib from the QMTest ExecTest class.
    # (But don't worry about Win32.)
    if os.WIFEXITED(status):            # status && 0xff = 0
        exit_code = os.WEXITSTATUS(status)
        if expected != None and exit_code != expected:
            result[component + '.expected_exit_code'] \
                             = str(expected)
            result[component + '.exit_code'] = str(exit_code)
            result.Fail("Unexpected exit code.")
    elif os.WIFSIGNALED(status):        # 0 < status < 258
        signum = os.WTERMSIG(status)    # status & 0x7f
        result[component + '.signal'] = str(signum)
        result.Fail("Exited with signal.")
    elif os.WIFSTOPPED(status):         # status > 256 && status & 0xff = 127
        signum = os.WSTOPSIG(status)    # status >> 8 & 0xff
        result[component + '.signal'] = str(signum)
        result.Fail("Stopped with signal.")
    else:
        result.Fail("Program terminated unexpectedly.")

# "What about qm.test.test.Test?  That's only got a little test
# in it..."  -- paraphrasing Monty Python
class RunStrcTest(qm.test.test.Test):
    """Run the StreamIt compiler as a QMTest test.

    Invoke the StreamIt compiler, 'strc', within QMTest.  This only
    validates the exit status of 'strc', not its output.  In this way
    it differs from 'qm.test.classes.command.ExecTest', which insists
    on a consistent standard output and standard error."""

    arguments = [
        BackendField(name='backend', title='Backend'),

        SetField(contained=
                 TextField(name='options',
                           title='Compiler flags',
                           description=
                           """Extra compiler flags to pass to 'strc'.
                                     
        Options should not contain spaces, so for example '--raw' and
        '4' should be separate options here.""")),
        
        SetField(contained=
                 TextField(name='filenames',
                           title='Filenames',
                           description=
                           """File names to pass to 'strc'.
                           
        Each of these files should be a StreamIt source file.""")),
        
        IntegerField(name='exit_code',
                     title='Exit Code',
                     description="""The expected exit code.

        The StreamIt compiler should exit with an exit status of 0 if
        compilation is successful.  For the library backend, this includes
        executing the program; for the other backends, this involves
        producing a set of one or more C files."""),
        
        IntegerField(name='iters', title='Iteration Count',
                     description="""The number of steady-state iterations to run.

        This field specifies the number of iterations to run the program.
        Must be available in compile test for library backend.  Must be
        availble in run test for other backends."""),

        TextField(name='own_output', title='Output File',
                  description="""Output file from running program

        This field specifies the file holding the program output to eventually
        be verified.  Must be available in compile test for library backend.
        Must be available in run test for other backends."""),

        IntegerField(name='timeout', title='Timeout', default_value=TIMEOUT,
                     description="""Timeout in seconds.

        Specifies the number of seconds that program can run before
        being killed.  If not specified, defaults to constant TIMEOUT""")
        ]

    def Run(self, context, result):
      """Actually run the StreamIt compiler."""

      test_home_dir = context_to_dir(context)

      path = os.path.join(os.environ['STREAMIT_HOME'], 'strc')
      # Figure out what flags to use for the backend
      if self.backend == 'uni':
          backend = []
      elif self.backend == 'simpleC':
          backend = ['--simpleC']
      elif self.backend == 'library':
          backend = ['--library']
      elif self.backend == 'raw4':
          backend = ['--raw', '4']
      elif self.backend == 'cluster':
          backend = ['--cluster', '1']
      # List of args to the program, starting with the program name,
      # and always including the iteration count:
      arguments = [path] + backend + \
                  ["--iterations", str(self.iters)] + \
                  self.options + self.filenames

#      print >> sys.stderr, "ABOUT TO COMPILE"
#      print >> sys.stderr, "dir = " + test_home_dir
#      print >> sys.stderr, "path = " + path
#      print >> sys.stderr, "arguments: " + (" ".join(arguments))
      e = qm.executable.RedirectedExecutable(self.timeout)

      ###
      ### All this retry and exception printing business:
      ### qmtest will raise an attribute not found error on __Executable__child
      ### if the system is out of file handles, as can be the case when a
      ### large cluster program is running.  Such an exception should be
      ### recoverable by waiting for the program to release the file handles.
      ### Other exceptions require eventual handling.
      ###
      status = None
      hasRun = 0
      attemptsLeft = 20
      while hasRun==0:
          try:
              status = e.Run(arguments, dir=test_home_dir, path=path)
              hasRun = 1
          except:
              exctype, value = sys.exc_info()[:2]
              traceback.print_exception(exctype,value,None,None,sys.stderr)
              time.sleep(60)
              attemptsLeft = attemptsLeft - 1
              if attemptsLeft == 0:
                  raise exctype, value

      # In all cases, save stderr.
      result['RunStrcTest.stderr'] = e.stderr
      # For the library backend, write stdout to a file;
      # for other backends, save it.
      if self.backend == 'library':
          f = open(os.path.join(test_home_dir,self.own_output), 'w')
          f.write(e.stdout)
          f.close()
      else:
          result['RunStrcTest.stdout'] = e.stdout

      InterpretExitCode(result, status, 0, 'RunStrcTest')

                  
class RunProgramTest(qm.test.test.Test):
    """Run a compiled program as a QMTest test.

    This runs the output of the StreamIt compiler.  This is only
    meaningful for the uniprocessor, cluster and RAW backends, since 'strc'
    runs the program itself for the library backend."""

    arguments = [
        BackendField(name='backend', title='Backend'),

        IntegerField(name='iters', title='Iteration Count',
                     description="""The number of steady-state iterations to run.

        This field specifies the number of iterations to run the program.
        Must be available in compile test for library backend.  Must be
        availble in run test for other backends."""),

        TextField(name='own_output', title='Output File',
                  description="""Output file from running program

        This field specifies the file holding the program output to eventually
        be verified.  Must be available in compile test for library backend.
        Must be availble in run test for other backends."""),

        IntegerField(name='timeout', title='Timeout', default_value=TIMEOUT,
                     description="""Timeout in seconds.

        Specifies the number of seconds that program can run before
        being killed.  If not specified, defaults to constant TIMEOUT""")
        ]

    def Run(self, context, result):
      """Actually run the target program."""

      if self.backend == 'raw4':
          return self._RunRaw(context, result)
      elif self.backend == 'uni' or self.backend == 'simpleC' or self.backend == 'cluster':
          return self._RunUni(context, result)
      else:
          result.Fail('Unknown backend: "' + self.backend + '"')

    def _RunRaw(self, context, result):
        test_home_dir = context_to_dir(context)

        #e = TimedExecutable()
        e = qm.executable.RedirectedExecutable(self.timeout)

        ###
        ### All this retry and exception printing business:
        ### qmtest will raise an attribute not found error on __Executable__child
        ### if the system is out of file handles, as can be the case when a
        ### large cluster program is running.  Such an exception should be
        ### recoverable by waiting for the program to release the file handles.
        ### Other exceptions require eventual handling.
        ###
        status = None
        hasRun = 0
        attemptsLeft = 20
        while hasRun==0:
          try:
              status = e.Run(['make', '-f', 'Makefile.streamit', 'run'],
                             dir=test_home_dir)
              hasRun = 1
          except:
              exctype, value = sys.exc_info()[:2]
              traceback.print_exception(exctype,value,None,None,sys.stderr)
              time.sleep(60)
              attemptsLeft = attemptsLeft - 1
              if attemptsLeft == 0:
                  raise exctype, value

        result['RunProgramTest.stderr'] = e.stderr
        #result['RunProgramTest.outputfilename'] = os.path.join(test_home_dir,self.own_output)

        # TODO: see what processing happens on this output, if any.
        f = open(os.path.join(test_home_dir,self.own_output), 'w')
        f.write(e.stdout)
        f.close()

        InterpretExitCode(result, status, 0, 'RunProgramTest')

    def _RunNamedFile(self, context, result, filename):
        # then run_cluster with path and -i
        test_home_dir = context_to_dir(context)

        # see CVS version 1.22 for attempt to run dumping stdout and
        # stderr to files in case internal QMTest error obscured error
        # in running program.  Didn't seem to help...
        path = os.path.join('.', filename)
        arguments = [path, '-i ' + str(self.iters)]
        #e = TimedExecutable()
        e = qm.executable.RedirectedExecutable(self.timeout)

        ###
        ### All this retry and exception printing business:
        ### qmtest will raise an attribute not found error on __Executable__child
        ### if the system is out of file handles, as can be the case when a
        ### large cluster program is running.  Such an exception should be
        ### recoverable by waiting for the program to release the file handles.
        ### Other exceptions require eventual handling.
        ###
        status = None
        hasRun = 0
        attemptsLeft = 20
        while hasRun==0:
          try:
              status = e.Run(arguments, dir=test_home_dir, path=path)
              hasRun = 1
          except:
              exctype, value = sys.exc_info()[:2]
              traceback.print_exception(exctype,value,None,None,sys.stderr)
              time.sleep(60)
              attemptsLeft = attemptsLeft - 1
              if attemptsLeft == 0:
                  raise exctype, value


        # Dump stdout to the file; ignore stderr.
        f = open(os.path.join(test_home_dir,self.own_output), 'w')
        f.write(e.stdout)
        f.close()
        result['RunProgramTest.stderr'] = e.stderr

        # Mostly, note if the program died an unholy death.
        InterpretExitCode(result, status, 0, 'RunProgramTest')
        
        
    def _RunUni(self, context, result):
        self._RunNamedFile(context, result, 'a.out')

class CompareResultsTest(qm.test.test.Test):
    """Compare the results from a program run to the expected output."""

    arguments = [
        BackendField(name='backend', title='Backend'),

        TextField(name='output', title='Output file',
                  default_value='output',
                  description="""Name of the file containing the output.

        This file contains the output from running the program.  For
        the uniprocessor and library backends, this should be a list
        of values, one to a line; for the RAW backend, this is the
        output of the RAW simulator 'btl'."""),

        TextField(name='expected', title='Expected results file',
                  description="""Name of file containing expected results.

        This file contains the expected output from running the
        program.  This should be a list of values, one to a line, for
        all backends.""")
        ]

    # Should these be frobbable fields?
    # Percent difference between expected and actual allowed:
    tolerance = 0.2
    # Difference smaller than this is treated as zero (round-off error)
    zero = 1e-6

    def Run(self, context, result):
      """Actually do the comparison."""

      test_home_dir = context_to_dir(context)

      try:

        failed = 0
        
        #print "self.expected: ", str(self.expected), "\n"
        #print "self.output:   ", str(self.output), "\n"

        #result['CompareResultsTest.outputfilename'] = os.path.join(test_home_dir,self.output)
        # First off, read the expected results file:
        expected = []
        try:
            f = open(os.path.join(test_home_dir,self.expected), 'r')
            expected = f.readlines()
            f.close()
        except:
            result.Fail('Missing correct results file "' +  \
                        os.path.join(test_home_dir,self.expected) + '"')
            
        # Next, read the actual results file:
        actual = []
        try:
            f = open(os.path.join(test_home_dir,self.output), 'r')
            actual = f.readlines()
            f.close()
        except:
            result.Fail('Missing program run results file "' +  \
                        os.path.join(test_home_dir,self.output) + '"')
            
        # For the RAW backend, cook 'actual'.
        # [21: 00001b444]: 0.098017   ==> 0.098017
        # with -spacetime format is: decimal, hex, float: 1.23 or 1 or -1e-08
        # ### PASSED:  1036565814 3dc8bd36    0.0980171412 [x,y] = [1, 2]
        # furthermore if this pattern occurs before the last occurrence of
        # the lline "running..." it should be ignored.
        if self.backend == 'raw4':
            actual = map(lambda s: re.subn(r'^\[(.+:) (.+)\]: (.*)$',
                                           r'\3', s),
                         actual)
            actual = filter(lambda p: p[1] > 0, actual)
            actual = map(lambda p: p[0], actual)

        # Optimizations may change the meaning of "n steady-state iterations"
        # So we accept actual results with length different from expected
        # The one thing we don't allow is no actual output where some output
        # is expected
        if len(actual) == 0 and len(expected) > 0:
            tag = 'CompareResultsTest.line_0'
            result[tag + '.expected'] = 'some output'
            result[tag + '.actual'] = 'no output'
            failed = 1
            
        # Build pairs of values, converted to floats.
        pairs = []
        for i in range(min(len(expected), len(actual))):
            try:
                ev = float(expected[i])
                av = float(actual[i])
                p = (ev, av)
                pairs.append(p)
            except:                     # error in conversion to float.
                tag = 'CompareResultsTest.line_%d' % i
                result[tag + '.expected'] = expected[i]
                result[tag + '.actual'] = actual[i]
                failed = 1
                
        # Now do the actual comparison:
        error_count = 0
        for i in range(len(pairs)):
            (ev, av) = pairs[i]
            difference = abs(av - ev)
            max_difference = abs(self.tolerance * ev)
            if difference > max_difference and difference > self.zero:
                failed = 1
                tag = 'CompareResultsTest.line_%d' % i
                result[tag + '.expected'] = str(ev)
                result[tag + '.actual'] = str(av)
                if ev != 0:
                    percent = (difference / ev) * 100
                    result[tag + '.percent'] = str(percent)
                error_count += 1
                if error_count == 10:  # only print first 10 differences
                    break
        # All done.
        if failed:
            result.Fail('Output mismatch.')

      finally:
          pass
      
