#!/usr/uns/bin/python
#
# examine-results.py: get interesting results from a QMTest results file
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: examine-results.py,v 1.1 2003-11-19 22:18:41 dmaze Exp $

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

result_file = 'results.qmr'
if len(sys.argv) > 1: result_file = sys.argv[1]
f = open(result_file, 'r')
results = qm.test.base.load_results(f)
f.close()

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

    if thedisp not in disposition: disposition[thedisp] = []
    disposition[thedisp].append(k)

# Get some counts:
fails = []
sum = 0
for k in ['compile-failed', 'run-failed', 'verify-failed']:
    if k in disposition:
        fails.append(len(disposition[k]))
    else:
        fails.append(0)
    sum = sum + fails[-1]
fails.insert(0, sum)

succeeds = []
sum = 0
for k in ['passed', 'not-verified']:
    if k in disposition:
        succeeds.append(len(disposition[k]))
    else:
        succeeds.append(0)
    sum = sum + succeeds[-1]
succeeds.insert(0, sum)

print "%4d failures  (%d compile, %d execute, %d verify)" % tuple(fails)
print "%4d successes (%d passed, %d not verified)" % tuple(succeeds)


def summarize(l):
    l.sort()
    for b in l:
        print "  " + b

if 'compile-failed' in disposition:
    print
    print "For the following benchmarks, COMPILATION failed:"
    summarize(disposition['compile-failed'])

if 'run-failed' in disposition:
    print
    print "For the following benchmarks, EXECUTION failed:"
    summarize(disposition['run-failed'])

if 'verify-failed' in disposition:
    print
    print "For the following benchmarks, VERIFICATION failed:"
    summarize(disposition['verify-failed'])

if 'not-verified' in disposition:
    print
    print "The following benchmarks executed, but were NOT VERIFIED:"
    summarize(disposition['not-verified'])

if 'passed' in disposition:
    print
    print "The following benchmarks PASSED:"
    summarize(disposition['passed'])
    print

