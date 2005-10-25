#!/usr/uns/bin/python
#
# run-reg-tests.py: Yet another test to run regression tests
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: run-reg-tests.py,v 1.25 2005-10-25 17:16:33 dimock Exp $
#
# Taking history from run_reg_tests.pl: this is the third implementation
# of a script to run StreamIt regression tests.  It is written in Python,
# since all of our other regtest stuff is for the QMTest world and
# because this should be saner.

import email.MIMEText
import os
import os.path
import popen2
import smtplib
import time
import re

# Some defaults:
admins = 'streamit-regtest-log@cag.lcs.mit.edu'
users = 'streamit-regtest@cag.lcs.mit.edu'
#admins = 'dimock@csail.mit.edu'
#users = 'dimock@csail.mit.edu'
cvs_root = '/projects/raw/cvsroot'
regtest_root = '/home/bits8/streamit/regtest'
smtp_server = 'k2.csail.mit.edu'

# TODO: determine distinctions between "nightly" and "all" regtests.
# These could just be different regtest.xml control files.

class PrepError(Exception):
    """Error if something goes wrong setting up."""
    def __init__(self, msg, where, log):
        self.msg = msg
        self.where = where
        self.log = log
    def __str__(self):
        return self.msg + "\n" + "See log in " + self.where + \
               "\n\n" + self.log

class RunRegTests:
    def __init__(self):
        self.make_paths()

    def main(self):
        try:
            self.prep()
            self.run_tests()
            self.pre_report() 
            self.report()
            self.rt_report()
            self.set_latest()
        except PrepError, e:
            self.mail_admins(str(e))

    def get_clean_timedate_stamp(self):
        return time.strftime("%Y%m%d.%H%M%S.%a")

    def make_paths(self):
        self.working_dir = regtest_root + "/" + self.get_clean_timedate_stamp()
        self.streamit_home = os.path.join(self.working_dir, 'streams')
        # Are changes to os.environm passed on through spawn and open2?
        os.environ['STREAMIT_HOME'] = self.streamit_home
        os.environ['TOPDIR'] = os.path.join(self.streamit_home, 'misc', 'raw')
        os.environ['PATH'] = self.streamit_home + \
                             ":/usr/uns/bin:/usr/bin/X11:/bin:/usr/bin"

        # Vaguely overcomplicated assembly of the CLASSPATH.
        # WIBNI we could read this from dot-bashrc?
        class_path = ['src', '3rdparty', '3rdparty/cplex/cplex.jar',
                      '3rdparty/jgraph/jgraph.jar']
        class_path = map(lambda p: os.path.join(self.streamit_home, p),
                         class_path)
        class_path = ['.',
                      '/usr/uns/jre/lib/rt.jar',
                      '/usr/uns/java/antlr.jar'] + class_path

        # Eclipse crud:
        eclipse_base = '/home/bits7/NO_BACKUP/streamit/eclipse/plugins'
        ecl = eclipse_base + '/org.eclipse.'
        ecl_ver = '2.1.1'
        class_path = class_path + \
                     map(lambda (p, q):
                         "%s%s_%s/%s" % (ecl, p, ecl_ver, q),
                         [('ui.workbench.texteditor', 'texteditor.jar'),
                          ('jface', 'jface.jar'),
                          ('ui.editors', 'editors.jar'),
                          ('jface.text', 'jfacetext.jar'),
                          ('swt.motif', 'ws/motif/swt.jar'),
                          ('ui.views', 'views.jar'),
                          ('core.runtime', 'runtime.jar'),
                          ('ui.workbench', 'workbench.jar'),
                          ('text', 'text.jar'),
                          ('jdt.core', 'jdtcore.jar'),
                          ('jdt.ui', 'jdt.jar'),
                          ('core.resources', 'resources.jar')])
        
        os.environ['CLASSPATH'] = ':'.join(class_path)

    def run_and_log(self, cmd, filename, action, permissible=0):
        """Run a command, logging its output under self.working_dir.

        Parameters:

        'cmd' -- String command to run.

        'filename' -- File in self.working_dir to log output to.

        'action' -- What we're doing, for error messages.

        'permissible' -- If true, allow any exit code, not just zero.
        Still trap exceptional exits, though.

        Exceptions:

        'PrepError' -- If the command fails with an exception, or if
        permissible is false and the command exits with a non-zero
        exit code."""

        # Use the older popen2 package here, since we want the
        # object to get the return status.
        pop = popen2.Popen4(cmd)
        msgs = ''
        while 1:
            data = pop.fromchild.read()
            if data == '':
                break
            msgs += data
        status = pop.wait()

        logfile = os.path.join(self.working_dir, filename)
        f = open(logfile, 'w')
        f.write(msgs)
        f.close()

        if (permissible and not os.WIFEXITED(status)) or \
           (not permissible and status != 0):
            raise PrepError(action + " failed!", logfile, msgs)

    def prep(self):
        # Perl regtest would mail the results of 'cvs checkout' and
        # compilation to the admins list.  Consciously avoid doing
        # that, since we never read it.  Do log these to files, though.
        os.makedirs(self.working_dir)
        os.chdir(self.working_dir)

        self.run_and_log('cvs -d %s co streams' % cvs_root, 'cvslog',
                         'CVS checkout')

        # There may or may not be a 'latest' file.
        # If there is then it is a link to a directory
        # The directory name should be the start time of the previous
        # regression test run.
        # Get a file of cvs history since the last run.  There is a slight
        # overlap since we are getting cvs history from the start of the
        # last run until now, rather than from the start of the cvs checkout
        # for the last run until the start of the cvs checkout for this run.
        self.cvs_date = ''
        try:
            last_dirname = os.path.basename(os.path.realpath(regtest_root
                                                             + '/latest'))
            re_pattern=re.compile('^(\d\d\d\d)(\d\d)(\d\d)\.(\d\d)(\d\d)')
            m = re_pattern.match(last_dirname)
            self.cvs_date = m.group(1)+'-'+m.group(2)+'-'+m.group(3)+' '+m.group(4)+':'+m.group(5)
            cvs_command = 'cvs history -x AMR -a -D "' + self.cvs_date + '"'
            os.chdir(self.working_dir + '/streams')
            self.run_and_log(cvs_command, 'cvshistory', 'Getting CVS history')
            os.chdir(self.working_dir)
        except:
            pass
        
        self.run_and_log('make -C %s/src' % self.streamit_home, 'makelog',
                         'Building the compiler')
        self.run_and_log('make -C %s/library/c' % self.streamit_home,
                         'makeclog', 'Building the C library')
        self.run_and_log('make -C %s/library/cluster' % self.streamit_home,
                         'makeclusterlog', 'Building the cluster library')
        self.run_and_log('make -C %s/misc/raw' % self.streamit_home, 'rawlog',
                         'Building the RAW tree', permissible=1)
        # No error results on this yet.
        os.chdir(self.streamit_home)
        os.spawnl(os.P_WAIT, self.streamit_home + '/regtest/qmtest/streamitqm',
                  'streamitqm', 'setup')

    def run_tests(self):
        # set to run several tests at once if sufficient cpus.
        # we should probably also check for sufficient virtual memory
        cpu_count = 0
        re_pattern=re.compile('^processor\s+:')
        try:
            f = open('/proc/cpuinfo', 'r')
            while 1:
                line = f.readline()
                if (not line):
                    break
                if re_pattern.match (line):
                    cpu_count += 1
            f.close()
        except:
            pass
        run_command = 'qmtest run'
        if cpu_count:
            run_command = run_command + ' -j' + str(cpu_count)
        # ok, now run with thread count as set above
        os.chdir(self.streamit_home)
        self.starttime = time.localtime()
        self.run_and_log(run_command, 'qmtestlog', 'Running QMTest',
			 permissible=1)
        self.endtime = time.localtime()

    def pre_report(self):
        self.run_and_log('qmtest report -o ' +
                         self.streamit_home + '/results.xml' + ' ' +
                         self.streamit_home + '/results.qmr',
                         'qmprereportlog', 'Generating xml report')

    def report(self):
        header = """StreamIt Regression Test Summary
--------------------------------

Start time: %s
End time: %s
Elapsed time: %s
Directory: %s

https://www2.cag.lcs.mit.edu/rt/StreamIt/listing.html has full results.
%s/results.qmr
is the QMTest results file.

""" \
        % (time.asctime(self.starttime), time.asctime(self.endtime),
           "%s seconds" % (time.mktime(self.endtime) -
                           time.mktime(self.starttime)),
           self.working_dir,
           self.streamit_home)
        
        last_results = ''
        try:
            fn = regtest_root + '/latest/streams/results.xml'
            os.stat(fn) # throws OSError if fn doesn't exist
            last_results = ' ' + fn
        except:
            pass

        if self.cvs_date:
            header = header + "CVS history since " + self.cvs_date + "\n"
            hf = open(self.working_dir + '/cvshistory', 'r')
            lines = hf.readlines()
            hf.close()
            re_pattern = re.compile('^(\S\s+\S+\s+\S+\s+\S+\s+\S+)\s+(\S+)\s+(\S+)\s+(streams/\S*)')
            re_docs_pattern = re.compile('^(\S\s+\S+\s+\S+\s+\S+\s+\S+)\s+(\S+)\s+(\S+)\s+(streams/docs/\S*)')
            for line in lines:
                m = re_pattern.match(line)
                if m:
                    n = re_docs_pattern.match(line)
                    if not n:
                        header = header + m.group(1) + ' ' + m.group(2) + ' ' + m.group(4) + '/' + m.group(3) + '\n'

            header = header + '\n';
            
        pop = popen2.Popen4(self.streamit_home +
                            '/regtest/qmtest/summarize_xml_results ' +
                            self.streamit_home + '/results.xml' +
                            last_results)
        summary = ''
        while 1:
            data = pop.fromchild.read()
            if data == '':
                break
            summary += data
        pop.wait()
        self.mail_all(header + summary)

    def rt_report(self):
        rt_root = os.path.join(self.working_dir, 'rt')
        os.mkdir(rt_root)
        # Ignore errors (but hope it works).
        os.spawnl(os.P_WAIT,
                  os.path.join(self.streamit_home,'regtest/qmtest/rt-results'),
                  'rt-results',
                  os.path.join(self.streamit_home, 'results.xml'),
                  rt_root)
        

    def mail_admins(self, contents):
        self.mail_to(admins, 'StreamIt Regression Test Log', contents)

    def mail_all(self, contents):
        self.mail_to(users, 'StreamIt Regression Test Summary', contents)

    def mail_to(self, to_whom, subject, contents):
        msg = email.MIMEText.MIMEText(contents)
        from_whom = admins
        msg['Subject'] = subject
        msg['From'] = from_whom
        msg['To'] = to_whom
        msg['Date'] = time.strftime("%a, %d %b %Y %H:%M:%S %z")
        s = smtplib.SMTP()
        s.connect(smtp_server)
        s.sendmail(from_whom, to_whom, msg.as_string())
        s.close()

    def set_latest(self):
        """Repoint the 'latest' symlink at this directory."""
        try:
            os.remove(os.path.join(regtest_root, 'latest'))
        except:
            pass
        try:
            os.symlink(self.working_dir,
		       os.path.join(regtest_root, 'latest'))
        except:
            pass

if __name__ == "__main__":
    RunRegTests().main()
