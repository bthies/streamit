#
# streamit.py: Python extensions to QMTest for StreamIt
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: streamit.py,v 1.2 2003-11-20 17:08:41 dmaze Exp $
#

# This file just defines some extra test classes that QMTest can use.
# For now it assumes the standard XML database class, though it doesn't
# explicitly require it.

import os
import os.path
import qm.executable
from   qm.fields import *
import qm.test.test
import re

class TimedExecutable(qm.executable.RedirectedExecutable):
    # TODO: make this configurable.
    timeout = 20 * 60

    # Insert some texec in here.
    def Run(self, arguments=[], environment=None, dir=None, path=None):
        newpath = os.environ["STREAMIT_HOME"] + "/regtest/tools/texec/texec"
        arguments = [newpath, '-s', str(self.timeout), path] + arguments[1:]
        return qm.executable.RedirectedExecutable.Run(self, arguments,
                                                      environment,
                                                      dir, newpath)

class BackendField(EnumerationField):
    """A field containing a StreamIt compiler backend."""

    backend_names = ['Uniprocessor', 'Library', 'RAW 4x4']
    backend_tags = ['uni', 'library', 'raw4']    

    # TODO: think about some way to present the backend_names
    # to the user, but use the backend_tags internally.
    def __init__(self, name, default_value=None, **properties):
        EnumerationField.__init__(self, name, default_value,
                                  self.backend_tags, **properties)

class RunOptionsField(TupleField):
    """A field containins options to run a StreamIt program."""

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
    if os.WIFEXITED(status):
        exit_code = os.WEXITSTATUS(status)
        if expected != None and exit_code != expected:
            result[component + '.expected_exit_code'] \
                             = str(expected)
            result[component + '.exit_code'] = str(exit_code)
            result.Fail("Unexpected exit code.")
    elif os.WIFSIGNALED(status):
        signum = os.WTERMSIG(status)
        result[component + '.signal'] = str(signum)
        result.Fail("Exited with signal.")
    elif os.WIFSTOPPED(status):
        signum = os.WSTOPSIG(status)
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
        affect the program execution.""")
        ]

    def Run(self, context, result):
        """Actually run the StreamIt compiler."""
        path = os.path.join(os.environ['STREAMIT_HOME'], 'strc')
        # Figure out what flags to use for the backend
        if self.backend == 'uni':
            backend = []
        elif self.backend == 'library':
            backend = ['--library']
        elif self.backend == 'raw4':
            backend = ['--raw', '4']
        # List of args to the program, starting with the program name,
        # and always including the iteration count:
        arguments = [path] + backend + \
                    ["--iterations", str(self.runopts[1])] + \
                    self.options + self.filenames
        e = TimedExecutable()
        status = e.Run(arguments, path=path)

        # In all cases, save stderr.
        result['RunStrcTest.stderr'] = e.stderr
        # For the library backend, write stdout to a file;
        # for other backends, save it.
        if self.backend == 'library':
            f = open(self.runopts[0], 'w')
            f.write(e.stdout)
            f.close()
        else:
            result['RunStrcTest.stdout'] = e.stdout

        InterpretExitCode(result, status, self.exit_code, 'RunStrcTest')
        
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
        iteration count used at compile time is used instead.""")
        ]

    def Run(self, context, result):
        """Actually run the target program."""
        if self.backend == 'raw4':
            return self._RunRaw(context, result)
        elif self.backend == 'uni':
            return self._RunUni(context, result)
        else:
            # Should raise an exception
            pass

    def _RunRaw(self, context, result):
        e = TimedExecutable()
        status = e.Run(['make', '-f', 'Makefile.streamit', 'run'])

        # TODO: see what processing happens on this output, if any.
        f = open(self.runopts[0], 'w')
        f.write(e.stdout)
        f.close()

        InterpretExitCode(result, status, None, 'RunProgramTest')

    def _RunUni(self, context, result):
        # Unless magic has happened, the binary should be a.out.
        path = os.path.join('.', 'a.out')
        arguments = [path, '-i' + str(self.runopts[1])]
        e = TimedExecutable()
        status = e.Run(arguments, path=path)

        # Dump stdout to the file; ignore stderr.
        f = open(self.runopts[0], 'w')
        f.write(e.stdout)
        f.close()

        # Mostly, note if the program died an unholy death.
        InterpretExitCode(result, status, None, 'RunProgramTest')

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

        failed = 0
        
        # First off, read the expected results file:
        f = open(self.expected, 'r')
        expected = f.readlines()
        f.close()

        # Next, read the actual results file:
        f = open(self.output, 'r')
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
                result[tag + '.expected'] = ev
                result[tag + '.actual'] = av
                if ev != 0:
                    percent = (difference / ev) * 100
                    result[tag + '.percent'] = percent

        # All done.
        if failed:
            result.Fail('Output mismatch.')
