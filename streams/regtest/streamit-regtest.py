#!/usr/bin/python

import getopt
import os
import string
import sys

# Useful globals:

class RegTest:
    def __init__(self):
        self.directory = ""
        self.output = ""
        self.opts = "-i1000"
        self.sources = []

    def setDir(self, dir):
        self.directory = dir

    def setOutput(self, out):
        self.output = out

    def setOpts(self, opts):
        self.opts = opts

    def addSource(self, src):
        self.sources.append(src)

    def runCommand(self, cmd):
        print cmd
        return os.system(cmd)

    def report(self, msg):
        print
        print "*** " + msg
        print
    
    def whyDied(self, result):
        if (os.WIFSIGNALED(result)):
            why = "with signal %d" % os.WTERMSIG(result)
            # It seems odd that there isn't a macro for this...
            if (result & 0x80 != 0):
                why = why + (" (core dumped)")
        else:
            why = "with status %d" % os.WEXITSTATUS(result)
        return why

    def test(self):
        oldwd = os.getcwd()
        os.chdir(os.path.join(opts.root, self.directory))
        result = self.dotest()
        os.chdir(oldwd)
        return result

    def dotest(self):
        result = self.runCommand("java at.dms.kjc.Main -s " +
                                 string.join(self.sources) + " > reg-out.c")
        if (result != 0):
            self.report("StreamIt compilation failed " + self.whyDied(result))
            return result

        result = self.runCommand("gcc -o reg-out %s reg-out.c -lstreamit" %
                                 opts.get_cflags())
        if (result != 0):
            self.report("gcc compilation failed " + self.whyDied(result))
            return result

        # Stop here if there was an explicit request to not run the test.
        if (not opts.run):
            return result

        # Otherwise, at least run the test.
        result = self.runCommand("./reg-out %s > reg-out.dat" % self.opts)
        if (result != 0):
            self.report("compiled binary failed " + self.whyDied(result))
            return result

        # Stop here if there isn't a reference file.
        if (not self.output):
            return result

        # diff the actual results against the expected.
        result = self.runCommand("cmp %s reg-out.dat" % self.output)
        if (result != 0):
            self.report("Comparing actual against expected output failed "
                        + self.whyDied(result))
            return result
        
        return result
    
class RegTestSet:
    def __init__(self):
        self.tests = {}

    def add(self, name, test):
        self.tests[name] = test
    
    def limit(self, names):
        set = RegTestSet()
        for name in names:
            try:
                set.add(name, self.tests[name])
            except (KeyError):
                self.report("No such test case " + name)
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
    
    def __init__(self):
        self.state = self.wantTest
        self.set = RegTestSet()
    
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
                self.test = RegTest()
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
            elif word == "output":
                self.state = self.haveOutput
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

class Options:
    def __init__(self):
        self.checkout = 0
        self.buildsys = 0        
        self.test = 1
        self.run = 1
        self.cases = []
        self.cflags = '-g -O2'
        self.set_root(os.environ['STREAMIT_HOME'])

    def get_options(self, args):
        optlist, args = getopt.getopt(args, '',
                                      ['checkout', 'nocheckout',
                                       'buildsys', 'nobuildsys',
                                       'test', 'notest',
                                       'run', 'norun',
                                       'root=', 'libdir=', 'control=',
                                       'debug', 'profile', 'cflags=',
                                       'case='])
        for (opt, val) in optlist:
            if opt == '--nocheckout': self.checkout = 0
            if opt == '--checkout':   self.checkout = 1
            if opt == '--nobuildsys': self.buildsys = 0
            if opt == '--buildsys':   self.buildsys = 1
            if opt == '--notest':     self.test = 0
            if opt == '--test':       self.test = 1
            if opt == '--norun':      self.run = 0
            if opt == '--run':        self.run = 1
            if opt == '--root':       self.set_root(val)
            if opt == '--libdir':     self.libdir = val
            if opt == '--control':    self.control = val
            if opt == '--debug':      self.cflags = '-g'
            if opt == '--profile':    self.cflags = '-g -pg -a'
            if opt == '--cflags':     self.cflags = val
            if opt == '--case':       self.cases.append(val)
        return args

    def set_root(self, root):
        self.root = root
        self.libdir = os.path.join(self.root, 'library/c')
        self.control = os.path.join(self.root, 'regtest/control')

    def get_cflags(self):
        return "%s -I%s -L%s" % (self.cflags, self.libdir, self.libdir)

opts = Options()
args = opts.get_options(sys.argv[1:])
set = ControlReader().read_control(opts.control)
if opts.checkout:
    raise NotImplementedError("Checkout not supported yet")
if opts.cases != []:
    set = set.limit(opts.cases)
if opts.buildsys:
    raise NotImplementedError("System build not supported yet")
if opts.test:
    set.run_tests()

