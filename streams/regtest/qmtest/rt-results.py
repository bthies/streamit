#!/usr/uns/bin/python
#
# rt-results.py: present QMTest results in CAG RT
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: rt-results.py,v 1.1 2003-12-16 20:54:07 dmaze Exp $

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
    rt_root = '.'
    if len(sys.argv) > 1: result_file = sys.argv[1]
    if len(sys.argv) > 2: rt_root = sys.argv[2]

    # We actually care about the detailed list of results here.
    # And all of the parts.  So inline classify_results stuff.
    f = open(result_file, 'r')
    results = qm.test.base.load_results(f)
    f.close()

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

    print_rt_summary(disposition, rt_root)
    print_rt_listing(disposition, rt_root)
    print_rt_details(resname, rt_root)

def nextline(line):
    """Alternates between RT 'oddline' and 'evenline'."""
    if line == 'oddline':
        return 'evenline'
    else:
        return 'oddline'

def print_rt_summary(disposition, rt_root):
    """Print a summary box listing how many tests succeeded.
    This box can be incorporated on the main RT page.

    'disposition' -- mapping from test name to string describing
    test disposition

    'rt_root' -- base directory for RT streamit crud"""

    ofn = os.path.join(rt_root, 'Summary')
    f = open(ofn, 'w')

    f.write('<table border=0 cellspacing=0 cellpadding=1 width=100%\n')
    f.write('<tr><th align=left>\n')
    f.write('<a href="<% $RT::WebPath %>/StreamIt/listing.html">Regtest</a>\n')
    f.write('</th><th>&nbsp;</th></tr>\n')

    line = 'oddline'
    for (tag, name) in [('compile-failed', 'Compile failed'),
                        ('run-failed', 'Execution failed'),
                        ('verify-failed', 'Verify failed'),
                        ('not-verified', 'Not verified'),
                        ('passed', 'Passed')]:
        f.write('<tr class=%s><td>%s</td><td>%d</td></tr>' %
                (line, name, len(filter(lambda v: v == tag,
                                        disposition.values()))))
        line = nextline(line)

    f.write('</table>\n')
    f.close()

def print_rt_regtest_item(rt_root):
    """Dump the RegtestItem file.

    'rt_root' -- base directory for RT streamit crud"""

    ofn = os.path.join(rt_root, 'RegtestItem')
    f = open(ofn, 'w')

    # Mmm, embedded Perl in HTML printed from Python.
    f.write('''<tr class=<%$Class%>>
<td><b><a href="<%"$Test.html"%>"><%$Test%></a></b></td>
<td><%$Status%></td><td>
<%PERL>
$Tickets->ClearRestrictions;
$Tickets->LimitStatus(VALUE => "new");
$Tickets->LimitStatus(VALUE => "open");
$Tickets->LimitQueue(VALUE => "StreamIt Bugs", OPERATOR => "=");
$Tickets->LimitCustomField(CUSTOMFIELD => 3, OPERATOR => "=",
                           VALUE => "$Test");
while (my $t = $Tickets->Next) {
</%PERL>
<a href="<%$RT::WebPath%>/Ticket/Display.html?id=<%$t->Id%>"><%$t->id%>
(<%$t->OwnerObj->Name%>) <%$t->Subject%></a><br>
% }
</td></tr>
<%INIT>
my $Tickets = RT::Tickets->new($session{"CurrentUser"});
</%INIT>
<%ARGS>
$Class => "oddline"
$Test => undef
$Status => undef
</%ARGS>
''')

    f.close()

def print_rt_listing(disposition, rt_root):
    """Print a listing of all of the tests and whether they
    succeeded or not.

    'disposition' -- mapping from test name to string describing
    test disposition

    'rt_root' -- base directory for RT streamit crud"""

    # To think about: the RegtestItem file does a search of all RT
    # tickets for each regtest thing.  This is slow.  It might be better
    # to write Perl code that scans all of the tickets once and builds
    # up a hash mapping test name to lists of ticket objects.  Except
    # that then it's perl and gets all referency and stuff.
    print_rt_regtest_item(rt_root)

    # To think about: do we want two listings, one by name,
    # one by disposition?  This might be more generally useful.
    ofn = os.path.join(rt_root, 'listing.html')
    f = open(ofn, 'w')

    f.write('<& /Elements/Header, Title => loc("StreamIt RegTest") &>\n')
    f.write('<& /Elements/Tabs, Title => loc("StreamIt Regtest") &>\n')
    f.write('<table border=0 width="100%">\n')
    f.write('<tr align=top><td>\n')
    f.write('<& /Elements/TitleBoxStart, title => "Tests by name" &>\n')

    line = 'oddline'
    names = disposition.keys()
    names.sort()
    for k in names:
        f.write(('<& RegtestItem, Class => "%s", Test => "%s", ' +
                 'Status => "%s" &>\n') % (line, k, disposition[k]))
        line = nextline(line)

    f.write('<& /Elements/TitleBoxEnd &>\n')
    f.write('</td></tr></table>\n')
    f.write('<& /Elements/Footer &>\n')
    f.close()

def print_rt_details(resname, rt_root):
    """Print a detailed page for each test giving its output and errors.

    'resname' -- mapping from test name to mapping from segment (compile,
    run, verify) to QMTest result object

    'rt_root' -- base directory for RT streamit crud"""

    # Go through all of the tests in no particular order.
    for testname in resname.keys():
        ofn = os.path.join(rt_root, "%s.html" % testname)
        f = open(ofn, 'w')

        tag = "StreamIt Regtest: " + testname
        f.write('<& /Elements/Header, Title => loc("%s") &>\n' % tag)
        f.write('<& /Elements/Tabs, Title => loc("%s") &>\n' % tag)

        for (key, tag) in [('compile', 'Compilation'),
                           ('run', 'Execution'),
                           ('verify', 'Verification')]:
            if key not in resname[testname]: continue
            result = resname[testname][key]
            f.write('<& /Elements/TitleBoxStart, title => "%s" &>\n'
                    % tag)

            line = 'oddline'
            for k in result.keys():
                f.write('<tr class="%s"><td colspan=2><b>%s</b><br>\n' %
                        (line, k))
                f.write('<p>%s</p></td></tr>\n' % (result[k]))
                line = nextline(line)
            f.write('<& /Elements/TitleBoxEnd &>')

        f.write('<& /Elements/Footer &>\n')
        f.close()

if __name__ == "__main__":
    main()
