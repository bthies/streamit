#
# testlib.py: Run StreamIt tests or benchmarks
# $Id: testlib.py,v 1.1 2001-11-16 18:54:36 dmaze Exp $
#

import os
import string

class StreamItOptions:
    def __init__(self):
        self.root = ""
        self.libdir = ""
        self.control = ""
        self.profile = 0
        self.checkout = 0
        self.buildsys = 0        
        self.test = 1
        self.run = 1
        self.all = 0
        self.cases = []
        self.cflags = '-g -O2'
        self.set_root(os.environ['STREAMIT_HOME'])

    def set_root(self, root):
        self.root = root
        self.libdir = os.path.join(self.root, 'library/c')
        self.control = os.path.join(self.root, 'regtest/control')
    def get_cppflags(self):
        return "%s -I%s" % (self.cflags, self.libdir)
    def get_ldflags(self):
        return "%s -L%s" % (self.cflags, self.libdir)
    def get_cflags(self):
        return "%s -I%s -L%s" % (self.cflags, self.libdir, self.libdir)
    def regout_name(self):
        if self.profile: return "reg-out_p"
        return "reg-out"

class StreamItTest:
    def __init__(self):
        self.directory = ""
        self.output = ""
        self.make = 0
        self.disabled = 0
        self.opts = "-i1000"
        self.sources = []

    def setDir(self, dir):      self.directory = dir
    def setDisabled(self, dis): self.disabled = dis
    def setMake(self, make):    self.make = make
    def setOutput(self, out):   self.output = out
    def setOpts(self, opts):    self.opts = opts
    def addSource(self, src):   self.sources.append(src)

    def isDisabled(self):       return self.disabled

    class CommandFailedException(Exception):
        def __init__(self, what, cmd, result):
            self.what = what
            self.cmd = cmd
            self.result = result

        def whyDied(self):
            # This is a little odd.  Ideally, we'd use things like
            # os.WIFSIGNALED to see what the exit status actually is.
            # But it looks like what Python thinks wait() and system()
            # return is different from what wait(2) documents.  Uh.
            if (self.result & 0xFF00):
                status = self.result >> 8
                why = "with signal %d" % (status & 0x7F)
                if ((status & 0x80) != 0):
                    why = why + (" (core dumped)")
            else:
                why = "with status %d" % (result & 0xFF)
            return why

        def __str__(self):
            return "%s failed %s" % (self.what, self.whyDied())

    def runCommand(self, what, cmd):
        print cmd
        result = os.system(cmd)
        if (result != 0):
            raise self.CommandFailedException(what, cmd, result)

    def report(self, msg):
        print
        print "*** " + msg
        print
    
    def test(self):
        oldwd = os.getcwd()
        os.chdir(os.path.join(opts.root, self.directory))
        result = self.dotest()
        os.chdir(oldwd)
        return result

    # Things that might get run as part of a test.
    def compile_to_c(self):
        if (self.make):
            self.runCommand("Precompilation", "make")
        self.runCommand("StreamIt compilation",
                        "java at.dms.kjc.Main -s %s > reg-out.c" %
                        string.join(self.sources))
        if (opts.profile):
            self.runCommand("gcc compilation (with profiling)",
                            "gcc -o reg-out_p.o -c %s -pg -a reg-out.c" %
                            opts.get_cppflags())
            self.runCommand("gcc linking (with profiling)",
                            ("gcc -o %s -pg -nodefaultlibs " +
                             "%s reg-out_p.o " +
                             "-lstreamit_p -lc_p -lm_p -lgcc") %
                            (opts.regout_name(), opts.get_ldflags()))
        else:
            self.runCommand("gcc compilation",
                            "gcc -o %s %s reg-out.c -lstreamit -lm" %
                            (opts.regout_name(), opts.get_cflags()))

    def run_compiled_binary(self):
        self.runCommand("Running compiled binary",
                        "./%s %s > reg-out.dat" %
                        (opts.regout_name(), self.opts))

    def check_results(self):
        self.runCommand("Comparing results",
                        "cmp %s reg-out.dat" % self.output)

    def dotest(self):
        try:
            self.compile_to_c()
            # Stop here if there was an explicit request to not run the test.
            if (not opts.run): return 0
            # Otherwise, at least run the test.
            self.run_compiled_binary()
            # Stop here if there isn't a reference file.
            if (not self.output): return 0
            # diff the actual results against the expected.
            self.check_results()
            return 0
        except self.CommandFailedException, e:
            self.report(str(e))
            return e.result
    
class StreamItTestFactory:
    def getTest(self):
        return StreamItTest()

class StreamItTestSet:
    def __init__(self):
        self.tests = {}

    def add(self, name, test):
        self.tests[name] = test
    
    def limit(self, names):
        set = StreamItTestSet()
        for name in names:
            try:
                set.add(name, self.tests[name])
            except (KeyError):
                self.report("No such test case " + name)
        return set

    def limitToEnabled(self):
        set = StreamItTestSet()
        for (name, test) in self.tests.items():
            if not test.isDisabled():
                set.add(name, test)
        return set

    def report(self, msg):
        print
        print ">>> " + msg
        print

    def run_tests(self):
        for name in self.tests.keys():
            self.report("Testing " + name)
            self.tests[name].test()

class ControlReader:
    class ParseError:
        pass

    wantTest, haveTest, wantOpen, wantDecl, haveDir, haveOutput, haveSource, haveOpts = range(8)
    
    def __init__(self, factory):
        self.state = self.wantTest
        self.set = StreamItTestSet()
        self.factory = factory
    
    def read_control(self, file):
        f = open(file, 'r')
        while 1:
            line = f.readline()
            if line == "":
                break
            # Strip out comments.
            line = string.split(line, '#')[0]
            for word in string.split(line):
                self.read_word(word)
        f.close()
        return self.set

    def read_word(self, word):
        if self.state == self.wantTest:
            if word == "test":
                self.state = self.haveTest
                self.test = self.factory.getTest()
            else:
                raise self.ParseError()
        elif self.state == self.haveTest:
            self.testname = word
            self.state = self.wantOpen
        elif self.state == self.wantOpen:
            if word == "{":
                self.state = self.wantDecl
            else:
                raise self.ParseError()
        elif self.state == self.wantDecl:
            if word == "directory":
                self.state = self.haveDir
            elif word == "disabled":
                self.test.setDisabled(1)
            elif word == "output":
                self.state = self.haveOutput
            elif word == "make":
                self.test.setMake(1)
            elif word == "source":
                self.state = self.haveSource
            elif word == "opts":
                self.state = self.haveOpts
            elif word == "}":
                self.set.add(self.testname, self.test)
                self.state = self.wantTest
            else:
                raise self.ParseError()
        elif self.state == self.haveDir:
            self.test.setDir(word)
            self.state = self.wantDecl
        elif self.state == self.haveOutput:
            self.test.setOutput(word)
            self.state = self.wantDecl
        elif self.state == self.haveSource:
            self.test.addSource(word)
            self.state = self.wantDecl
        elif self.state == self.haveOpts:
            self.test.setOpts(word)
            self.state = self.wantDecl
        else:
            raise self.ParseError()

opts = StreamItOptions()
