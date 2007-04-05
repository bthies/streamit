#!/usr/uns/bin/python
#
# run-reg-tests.py: Yet another test to run regression tests
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: run-reg-tests.py,v 1.45 2007-04-05 13:13:36 dimock Exp $
#
# Taking history from run_reg_tests.pl: this is the third implementation
# of a script to run StreamIt regression tests.  It is written in Python,
# since all of our other regtest stuff is for the QMTest world and
# because this should be saner.

# Javadoc crash looks like:
# 	at com.sun.tools.javadoc.Main.main(Main.java:31)
# should check for this.

import email.MIMEText
import os
import os.path
import popen2
import smtplib
import time
import re

import sys

# Some defaults:
admins = 'streamit-regtest-log@cag.lcs.mit.edu'
users = 'streamit-regtest@cag.lcs.mit.edu'
#admins = 'dimock@csail.mit.edu'
#users = 'dimock@csail.mit.edu'
#
# The following were munged to run on a system requiring a different version
# of python than the one in /usr/uns/bin/pyton and a different version
# of gcc than the one in /usr/uns/bin/gcc
#
# command to start up qmtest
invoke_qmtest = 'qmtest'
# invoke_qmtest = '/usr/bin/python /home/bits7/dimock/qm-2.3/bin/qmtest'
# prefix to line to run build-qmtest.py.
invoke_build_prefix = ''
# invoke_build_prefix = '/usr/bin/python '
# flags for make of libraries, raw 
make_flags = ''
# make_flags = ' CC=/usr/bin/gcc CXX=/usr/bin/g++ '
#
fixed_path ='/usr/uns/jdk1.5.0_01/bin:/usr/uns/bin:/usr/bin/X11:/bin:/usr/bin'
# fixed_path = '/usr/uns/jdk1.5.0_01/bin:/usr/bin/X11:/bin:/usr/bin:/usr/uns/bin'
#
#
cvs_root = '/projects/raw/cvsroot'
regtest_parent = '/home/bits8/streamit'
regtest_root = os.path.join(regtest_parent, 'regtest')
javadoc_dir = '/home/public/streamit/latest_javadoc'
javadoc_script = 'misc/build-javadoc'
javadoc_error_dir = '/home/public/streamit/latest_doccheck'
javadoc_error_script = 'misc/check-javadoc-errors'
summary_script_first = 'regtest/qmtest/status_summary.xsl'
summary_script_second = 'regtest/qmtest/status_summary.pl'
javadocLog = 'javadocLog'
xmlresults = 'results.xml'
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

# uncomment the following and comment the preceeding to just report on
# an already-run regression test.

#     def main(self):
#         self.starttime = time.localtime()
#         self.endtime = time.localtime()
#         self.pre_report()
#         self.report()
        
#     def get_clean_timedate_stamp(self):
#         return "20060326.220014.Sun"

    def make_paths(self):
        self.working_dir = regtest_root + "/" + self.get_clean_timedate_stamp()
        self.streamit_home = os.path.join(self.working_dir, 'streams')
        # Are changes to os.environm passed on through spawn and open2?
        os.environ['STREAMIT_HOME'] = self.streamit_home
        #os.environ['TOPDIR'] = os.path.join(self.streamit_home, 'misc', 'raw')
        os.environ['TOPDIR'] = '/home/bits6/mgordon/starsearch'
        os.environ['PATH'] = self.streamit_home + ':' + fixed_path

        # hack to make simpleC compiles work on some machines.
        # may need to remove when changing machines.
        # os.environ['CC'] = '/usr/bin/gcc'
        
        # Vaguely overcomplicated assembly of the CLASSPATH.
        # WIBNI we could read this from dot-bashrc?
        class_path = ['src', '3rdparty', '3rdparty/cplex/cplex.jar',
                      '3rdparty/jgraph/jgraph.jar','3rdparty/JFlex/jflex.jar',
                      '3rdparty/jcc/jcc.jar']
        class_path = map(lambda p: os.path.join(self.streamit_home, p),
                         class_path)
        class_path = ['.',
                      '/usr/uns/jdk1.5.0_01/jre/lib/rt.jar',
                      '/usr/uns/java/antlr.jar'] + class_path

        ld_library_path = '/projects/streamit/3rdparty/fftw/fftw-2.1.5/rfftw/.libs:/projects/streamit/3rdparty/fftw/fftw-2.1.5/fftw/.libs'
        c_include_path = '/projects/streamit/3rdparty/fftw/fftw-2.1.5/rfftw:/projects/streamit/3rdparty/fftw/fftw-2.1.5/fftw'
        #different gcc / OS releases have different standard names
        os.environ['LD_LIBRARY_PATH'] = ld_library_path
        os.environ['LPATH'] = ld_library_path
        os.environ['C_INCLUDE_PATH'] = c_include_path
        os.environ['CPATH'] = c_include_path
        # perl libraries needed by strc 
#        os.environ['PERL5LIB'] = '/usr/uns/encap/perl-5.8.0/lib/5.8.0/:/usr/uns/lib/site_perl/5.8.0:/home/streamit/lib/perl5/site_perl/5.8.0:/home/streamit/lib/perl5/site_perl/5.8.0/i386-linux-thread-multi'
        os.environ['PERL5LIB'] = '/usr/uns/encap/perl-5.8.0/lib/5.8.0:/u/dimock/lib/site_perl/5.8.0'
        # Eclipse crud:
#        eclipse_base = '/home/bits7/NO_BACKUP/streamit/eclipse/plugins'
#        ecl = eclipse_base + '/org.eclipse.'
#        ecl_ver = '2.1.1'
#        class_path = class_path + \
#                     map(lambda (p, q):
#                         "%s%s_%s/%s" % (ecl, p, ecl_ver, q),
#                         [('ui.workbench.texteditor', 'texteditor.jar'),
#                          ('jface', 'jface.jar'),
#                          ('ui.editors', 'editors.jar'),
#                          ('jface.text', 'jfacetext.jar'),
#                          ('swt.motif', 'ws/motif/swt.jar'),
#                          ('ui.views', 'views.jar'),
#                          ('core.runtime', 'runtime.jar'),
#                          ('ui.workbench', 'workbench.jar'),
#                          ('text', 'text.jar'),
#                          ('jdt.core', 'jdtcore.jar'),
#                          ('jdt.ui', 'jdt.jar'),
#                          ('core.resources', 'resources.jar')])
        
        os.environ['CLASSPATH'] = ':'.join(class_path)

        # Set up "cvs_date" which is independent of any actions in current run
        #
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
        except:
            pass


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
        f = open(logfile, mode='a')
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

        self.run_and_log('printenv', 'envLog', 'Log working environment')
        self.run_and_log('cvs -d %s co streams' % cvs_root, 'cvsLog',
                         'CVS checkout')

        # I there is a "latest" run,
        # get a file of cvs history since the latest run.  There is a slight
        # overlap since we are getting cvs history from the start of the
        # last run until now, rather than from the start of the cvs checkout
        # for the last run until the start of the cvs checkout for this run.
        if self.cvs_date != '':
            try:
                cvs_command = 'cvs history -x AMR -a -D "' + self.cvs_date + '"'
                os.chdir(self.working_dir + '/streams')
                self.run_and_log(cvs_command, 'cvsHistory',
                                 'Getting CVS history')
                os.chdir(self.working_dir)
            except:
                self.cvs_date = ''
        
        self.run_and_log('make -C %s/src' % self.streamit_home +
                         make_flags, 'makeLog',
                         'Building the compiler')
        self.run_and_log('make -C %s/library/c' % self.streamit_home +
                         make_flags,
                         'makeCLog', 'Building the C library')
        self.run_and_log('make -C %s/library/cluster' % self.streamit_home +
                         make_flags,
                         'makeClusterLog', 'Building the cluster library')
        self.run_and_log('make -C %s/misc/raw' % self.streamit_home +
                         make_flags, 'rawLog',
                         'Building the RAW tree', permissible=1)
        # No error results on this yet.
        os.chdir(self.streamit_home)
        self.run_and_log('mkdir QMTest', 'setupLog', 'making QM database',
                         permissible=1)
        self.run_and_log('cp regtest/qmtest/classes.qmc QMTest/', 'setupLog',
                         'making QM database')
        self.run_and_log('cp regtest/qmtest/configuration QMTest/', 'setupLog',
                         'making QM database')
        self.run_and_log('cp regtest/qmtest/streamit.py QMTest/', 'setupLog',
                         'making QM database')
        self.run_and_log(invoke_build_prefix + self.streamit_home +
                         '/regtest/qmtest/build-qmtest.py' + ' ' +
                         self.streamit_home + '/regtest/qmtest/regtest.xml',
                          'setupLog', 'making QM database')

    def run_tests(self):
        # set to run several tests at once if sufficient cpus.
        # we should probably also check for sufficient virtual memory
        cpu_count = 1
#         re_pattern=re.compile('^processor\s+:')
#         try:
#             f = open('/proc/cpuinfo', 'r')
#             while 1:
#                 line = f.readline()
#                 if (not line):
#                     break
#                 if re_pattern.match (line):
#                     cpu_count += 1
#             f.close()
#             if cpu_count > 1:
#                 # set number of cpus to number found
#                 cpu_count = cpu_count - 1
#         except:
#             pass
        run_command = invoke_qmtest + ' run'
        #if cpu_count:
        #    run_command = run_command + ' -j' + str(cpu_count)
        # ok, now run with thread count as set above
        os.chdir(self.streamit_home)
        self.run_and_log(invoke_qmtest + ' create-target -a processes='
                         + str(cpu_count) + ' p' + str(cpu_count) + ' '
                         + 'process_target.ProcessTarget p',
                         'qmtestTargetLog', 'Setting Process target')
        self.starttime = time.localtime()
        self.run_and_log(run_command, 'qmtestLog', 'Running QMTest',
			 permissible=1)
        self.endtime = time.localtime()

    def pre_report(self):
        self.run_and_log(invoke_qmtest + ' report -o ' +
                         os.path.join(self.streamit_home, xmlresults)
                         + ' ' +
                         os.path.join(self.streamit_home, 'results.qmr'),
                         'qmPrereportLog', 'Generating xml report')
        self.run_and_log(os.path.join(self.streamit_home, javadoc_script)
                         + " " + javadoc_dir,
                         javadocLog, "Generating latest javadoc")

        self.javadocwarnings = ''
        pop = popen2.Popen4('tail -n 1 '
                            + os.path.join(self.working_dir,javadocLog)
                            + ' | grep warnings')
        while 1:
            data = pop.fromchild.read()
            if data == '':
                break
            self.javadocwarnings += data
        status = pop.wait()

        self.javadocdetail = ''
        pop = popen2.Popen4('cat '
                            + os.path.join(self.working_dir,javadocLog)
                            + ' | grep /warnings | sed '
                            + "'" + 's/^.*\\/streams//' + "'" )
        while 1:
            data = pop.fromchild.read()
            if data == '':
                break
            self.javadocdetail += data
        status = pop.wait()


        self.run_and_log(os.path.join(self.streamit_home, javadoc_error_script)
                         + " -o " + javadoc_error_dir,
                         "javadocErrorLog", "Generating javadoc error report")

    def report(self):
        hdrfile = open(os.path.join(self.streamit_home,'summary_report'), 'w')
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
            fn = os.path.join(regtest_root, 'latest', 'streams', xmlresults)
            os.stat(fn) # throws OSError if fn doesn't exist
            last_results = ' ' + fn
        except:
            pass

        if self.javadocwarnings != '':
            header = header + 'Javadoc: ' + self.javadocwarnings + "\n"
            
        if self.cvs_date:
            header = header + "CVS history since " + self.cvs_date + "\n"
            hf = open(os.path.join(self.working_dir, 'cvsHistory'), 'r')
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
            
        hdrfile.write(header)          # save header as file in case mail error
        hdrfile.close()
        
        tmpfile = os.path.join(self.working_dir, 'summary_data')
        
        self.run_and_log('xsltproc -o ' + tmpfile
                         + ' --novalid ' + os.path.join(self.streamit_home,
                                                        summary_script_first)
                          + ' ' + os.path.join(self.streamit_home, xmlresults),
                         "summaryXslLog", "Generating summary: xsl process")

        oldxml = os.path.join(regtest_root, 'latest', 'streams', xmlresults)

        oldtmp = ''
        if os.access(oldxml, os.R_OK):
            oldtmp = os.path.join(self.working_dir, 'old_summary_data')
            self.run_and_log('xsltproc -o ' + oldtmp
                         + ' --novalid ' + os.path.join(self.streamit_home,
                                                        summary_script_first)
                          + ' ' + oldxml,
                         "summaryXslLog2", "Generating summary: xsl process 2")

        summary_body = os.path.join(self.streamit_home,'summary_body')
        self.run_and_log(os.path.join(self.streamit_home,summary_script_second)
                         + ' ' + tmpfile + ' ' + oldtmp + ' > ' + summary_body,
                         "summaryLog", "Generating summary: format")


        summary = ''
        sb = open(summary_body, 'r')
        for line in sb:
            summary += line
        sb.close()

        summary = summary + "\n\nJavaDoc warnings:\n" + self.javadocdetail
        
        self.mail_all(header + summary)

    def rt_report(self):
        rt_root = os.path.join(self.working_dir, 'rt')
        os.mkdir(rt_root)
        # Ignore errors (but hope it works).
        os.spawnl(os.P_WAIT,
                  os.path.join(self.streamit_home,'regtest/qmtest/rt-results'),
                  'rt-results',
                  os.path.join(self.streamit_home, xmlresults),
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
        if os.access(os.path.join(self.working_dir,
                                  'rt/Summary'),
                     os.F_OK):
            try:
                os.remove(os.path.join(regtest_root, 'latest'))
            except:
                pass
            try:
                os.symlink(self.working_dir,
                           os.path.join(regtest_root, 'latest'))
            except:
                pass
        else:
            # repointing latest will cause error in rt application
            # if rt/Summary does not exist.  Proper fix should be a check
            # in /home/rt/local/html/index.html as part of check for a
            # StreamIt user or in /home/rt/local/html/Elements/StreamIt
            # which refers to <& /StreamIt/Summary &> , which following
            # several symbolic links turn out to be
            # /home/rt/local/html/StreamIt -> /projects/streamit/www/rt  ,
            # /projects/streamit/www/rt -> /home/bits8/streamit/regtest/latest/rt
            pass

if __name__ == "__main__":
    RunRegTests().main()
