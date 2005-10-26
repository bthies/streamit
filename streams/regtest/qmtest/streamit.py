#
# streamit.py: Python extensions to QMTest for StreamIt
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: streamit.py,v 1.10 2005-10-26 22:44:53 dimock Exp $
#

# This file just defines some extra test classes that QMTest can use.
# For now it assumes the standard XML database class, though it doesn't
# explicitly require it.

import os
import os.path
import qm.executable
from   qm.fields import *
import qm.test.test
import signal
import threading
import re
import socket
import shutil

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

    backend_names = ['Uniprocessor', 'Library', 'RAW 4x4', 'Cluster 1']
    backend_tags = ['uni', 'library', 'raw4', 'cluster']    

    # TODO: think about some way to present the backend_names
    # to the user, but use the backend_tags internally.
    def __init__(self, name, default_value=None, **properties):
        EnumerationField.__init__(self, name, default_value,
                                  self.backend_tags, **properties)

class RunOptionsField(TupleField):
    """A field containing options to run a StreamIt program."""

    def __init__(self, name, **properties):

        fields = [qm.fields.TextField(name="output",
                                      default_value="output"),
                  qm.fields.IntegerField(name="iters",
                                         default_value=100)]
        TupleField.__init__(self, name, fields, **properties)

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
        
        RunOptionsField(name='runopts', title='Runtime options',
                        description="""Options used running the program.
                        
        Specifies the number of iterations and the output file for
        the program.  Whether and how these are used depends on
        the backend.  The iteration count here is always passed to
        'strc', but only affects actual compilation for the RAW
        backend.  For the library backend, 'strc' also runs the
        program, so the iteration count and output file here
        affect the program execution."""),

        IntegerField(name='timeout', title='Timeout', default_value=TIMEOUT,
                     description="""Timeout in seconds.

        Specifies the number of seconds that program can run before
        being killed.  If not specified, defaults to constant TIMEOUT""")
        ]

    def Run(self, context, result):
      """Actually run the StreamIt compiler."""

      test_home_dir = context_to_dir(context)

#      print "In directory ", os.getcwd(), "\n"
#      print "RunStrTest.Run ", os.getcwd(), "\n"
#      print "runopts[0]: ", str(self.runopts[0]), "\n"
      print "runopts[1]: ", str(self.runopts[1]), "\n"
      print "timeout: ", str(self.timeout), "\n"
#      print "options: ", str(self.options), "\n"
#      print "filenames: ", str(self.filenames), "\n"
#      print "context keys: ", str(context.keys()), "\n"
#      print "context values: ", str(context.values()), "\n"

      path = os.path.join(os.environ['STREAMIT_HOME'], 'strc')
      # Figure out what flags to use for the backend
      if self.backend == 'uni':
          backend = []
      elif self.backend == 'library':
          backend = ['--library']
      elif self.backend == 'raw4':
          backend = ['--raw', '4']
      elif self.backend == 'cluster':
          backend = ['-cluster', '1']
      # List of args to the program, starting with the program name,
      # and always including the iteration count:
      arguments = [path] + backend + \
                  ["--iterations", str(self.runopts[1])] + \
                  self.options + self.filenames
      #e = TimedExecutable()
      e = qm.executable.RedirectedExecutable(self.timeout)
      status = e.Run(arguments, dir=test_home_dir, path=path)

      # In all cases, save stderr.
      result['RunStrcTest.stderr'] = e.stderr
      # For the library backend, write stdout to a file;
      # for other backends, save it.
      if self.backend == 'library':
          f = open(os.path.join(test_home_dir,self.runopts[0]), 'w')
          f.write(e.stdout)
          f.close()
      else:
          result['RunStrcTest.stdout'] = e.stdout

      InterpretExitCode(result, status, self.exit_code, 'RunStrcTest')

      if (self.backend == 'cluster'
          and result.GetOutcome() == result.PASS):
          # cluster requires extra make step
          #e = TimedExecutable()
          e = qm.executable.RedirectedExecutable(self.timeout)
          e.Run(['make', '-f', 'Makefile.cluster', 'run_cluster'], dir=test_home_dir)
          result['RunStrcTest.stdout_makefile'] = e.stdout
          result['RunStrcTest.stderr_makefile'] = e.stderr
          makestatus = 1
          if os.access('run_cluster', os.F_OK):
              makestatus=0

          InterpretExitCode(result, makestatus, self.exit_code, 'RunStrcTest')

          if (result.GetOutcome() == result.PASS):
              # first need to replace "machine-1" in cluster-config.txt
              # with name of the machine that we are running on (uname -n)
              finame = os.path.join(test_home_dir, "cluster-config.txt")
              ftname = os.path.join(test_home_dir, "cluster-config.txt.tmp")
              # need socket to get host name??!
              hostname = socket.gethostname()
              fi = open(finame)
              ft = open(ftname, 'w')
              for s in fi.readlines():
                  ft.write(s.replace('machine-1',hostname))
                  
              fi.close()
              ft.close()
              # Python 2.2.2: no shutil.move yet
              shutil.copy(ftname,finame)
              os.unlink(ftname)        

              
class RunProgramTest(qm.test.test.Test):
    """Run a compiled program as a QMTest test.

    This runs the output of the StreamIt compiler.  This is only
    meaningful for the uniprocessor and RAW backends, since 'strc'
    runs the program itself for the library backend."""

    arguments = [
        BackendField(name='backend', title='Backend'),

        RunOptionsField(name='runopts', title='Runtime options',
                        description="""Options used running the program.
                        
        Specifies the number of iterations and the output file for
        the program.  Whether and how these are used depends on
        the backend.  In both cases, the output is written to
        the specified output file.  The iteration count is used
        for the uniprocessor backend; for the RAW backend, the
        iteration count used at compile time is used instead."""),

        IntegerField(name='timeout', title='Timeout', default_value=TIMEOUT,
                     description="""Timeout in seconds.

        Specifies the number of seconds that program can run before
        being killed.  If not specified, defaults to constant TIMEOUT""")
        ]

    def Run(self, context, result):
      """Actually run the target program."""
        
      print "Run: runopts[1]: ", str(self.runopts[1]), "\n"
      print "timeout: ", str(self.timeout), "\n"
      if self.backend == 'raw4':
          return self._RunRaw(context, result)
      elif self.backend == 'uni':
          return self._RunUni(context, result)
      elif self.backend == 'cluster':
          return self._RunClu(context, result)
      else:
          # Should raise an exception
          pass

    def _RunRaw(self, context, result):
        test_home_dir = context_to_dir(context)

        #e = TimedExecutable()
        e = qm.executable.RedirectedExecutable(self.timeout)
        status = e.Run(['make', '-f', 'Makefile.streamit', 'run'])

        # TODO: see what processing happens on this output, if any.
        f = open(os.path.join(test_home_dir,self.runopts[0]), 'w')
        f.write(e.stdout)
        f.close()

        InterpretExitCode(result, status, None, 'RunProgramTest')

    def _RunNamedFile(self, context, result, filename):
        # then run_cluster with path and -i
        test_home_dir = context_to_dir(context)

        path = os.path.join('.', filename)
        arguments = [path, '-i' + str(self.runopts[1])]
        #e = TimedExecutable()
        e = qm.executable.RedirectedExecutable(self.timeout)
        status = e.Run(arguments, dir=test_home_dir, path=path)

        # Dump stdout to the file; ignore stderr.
        f = open(os.path.join(test_home_dir,self.runopts[0]), 'w')
        f.write(e.stdout)
        f.close()

        # Mostly, note if the program died an unholy death.
        InterpretExitCode(result, status, None, 'RunProgramTest')
        
    def _RunClu(self, context, result):
        self._RunNamedFile(context, result, 'run_cluster')

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

        # First off, read the expected results file:
        f = open(os.path.join(test_home_dir,self.expected), 'r')
        expected = f.readlines()
        f.close()

        # Next, read the actual results file:
        f = open(os.path.join(test_home_dir,self.output), 'r')
        actual = f.readlines()
        f.close()

        # For the RAW backend, cook 'actual'.
        if self.backend == 'raw4':
            actual = map(lambda s: re.subn(r'^\[(.+:) (.+)\]: (.*)$',
                                           r'\3', s),
                         actual)
            actual = filter(lambda p: p[1] > 0, actual)
            actual = map(lambda p: p[0], actual)

        # Build pairs of values, converted to floats.
        pairs = []
        for i in range(min(len(expected), len(actual))):
            ev = float(expected[i])
            av = float(actual[i])
            p = (ev, av)
            pairs.append(p)

        # Now do the actual comparison:
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

        # All done.
        if failed:
            result.Fail('Output mismatch.')

      finally:
          pass
      
