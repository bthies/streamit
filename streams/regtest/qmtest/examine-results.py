#!/usr/uns/bin/python
#
# examine-results.py: get interesting results from a QMTest results file
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: examine-results.py,v 1.2 2003-11-24 16:22:51 dmaze Exp $

import os
import os.path

# First thing we need to do is set up some magic to find the QMTest
# classes.  Erk.
qm_home = '/usr/uns'
os.environ['QM_HOME'] = qm_home
os.environ['QM_PATH'] = '%s/bin/qmtest' % qm_home
os.environ['QM_BUILD'] = '0'
execfile(os.path.join(qm_home, 'lib/qm/qm', 'setup_path.py'))

# Now, be a normal script from here on in.
import qm.test.base
from   qm.test.result import Result

def main():
    result_file = 'results.qmr'
    if len(sys.argv) > 1: result_file = sys.argv[1]
    f = open(result_file, 'r')
    results = qm.test.base.load_results(f)
    f.close()
    current = classify_results(results)
    print_counts(current)
    detailed_results(current)

def classify_results(results):
    """Classify a listing of QMTest results for StreamIt.

    Running a particular StreamIt benchmark can have several different
    outcomes, depending on the target backend and what information is
    available.  On a particular test, compilation can fail
    ('compile-failed'); if it succeeds, and the backend is not the
    Java libary backend, execution can fail as well ('run-failed').
    If both of these pass, a reference output may be missing
    ('not-verified'), or the output may disagree ('verify-failed') or
    agree ('passed') with it.

    'results' -- A list of 'qm.test.result.Result' objects.

    returns -- A mapping from string test name prefix to one of the
    parenthesized strings in the description above."""

    # How can things divide up?  A couple of ways:
    # -- compile failed
    # -- compile succeeded, run failed
    # -- compile succeeded, run succeeded or is absent, verify failed
    # -- compile/run succeeded, verify is absent
    # -- compile/run succeeded, verify succeeded
    #
    # Start by breaking up the list of results by test name.
    resname = {}
    for r in results:
        label = r.GetId()
        parts = label.split('.')
        first = '.'.join(parts[:-1])
        last = parts[-1]
        if first not in resname: resname[first] = {}
        resname[first][last] = r

    # Now go through that list.
    disposition = {}
    for k in resname.keys():
        if resname[k]['compile'].GetOutcome() != Result.PASS:
            thedisp = 'compile-failed'
        elif 'run' in resname[k] and \
             resname[k]['run'].GetOutcome() != Result.PASS:
            thedisp = 'run-failed'
        elif 'verify' not in resname[k]:
            thedisp = 'not-verified'
        elif resname[k]['verify'].GetOutcome() != Result.PASS:
            thedisp = 'verify-failed'
        else:
            thedisp = 'passed'
        disposition[k] = thedisp

    return disposition

def print_counts(disposition):
    """Print a message about the total number of successes and failures."""
    # Get some counts:
    fails = []
    sum = 0
    for k in ['compile-failed', 'run-failed', 'verify-failed']:
        fails.append(len(filter(lambda v: v == k, disposition.values())))
        sum = sum + fails[-1]
    fails.insert(0, sum)

    succeeds = []
    sum = 0
    for k in ['passed', 'not-verified']:
        succeeds.append(len(filter(lambda v: v == k, disposition.values())))
        sum = sum + succeeds[-1]
    succeeds.insert(0, sum)

    print "%4d failures  (%d compile, %d execute, %d verify)" % tuple(fails)
    print "%4d successes (%d passed, %d not verified)" % tuple(succeeds)

def detailed_results(disposition):
    """Print detailed results."""

    for (k, t) in \
        [('compile-failed',
          "For the following benchmarks, COMPILATION failed:"),
         ('run-failed', "For the following benchmarks, EXECUTION failed:"),
         ('verify-failed',
          "For the following benchmarks, VERIFICATION failed:"),
         ('not-verified',
          "The following benchmarks executed, but were NOT VERIFIED:"),
         ('passed', "The following benchmarks PASSED:")]:
        if k in disposition.values():
            print
            print t
            l = [bench for bench, disp in disposition.items() if disp == k]
            l.sort()
            for b in l:
                print "  " + b

if __name__ == "__main__":
    main()
