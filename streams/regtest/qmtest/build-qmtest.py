#!/usr/uns/bin/python
#
# build-qmtest.py: build QMTest XML files from the StreamIt tree
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: build-qmtest.py,v 1.20 2007-04-03 23:45:08 dimock Exp $
#

import os
import os.path
import re
import shutil
import sys
from   types import *
import xml.dom.minidom

# TODO: some option parsing.
# For now, assume sys.argv[1] has the path to the regtest control file,
# and that we're in STREAMIT_HOME.
# If sys.argv[2] exists, prefix it to the 'root' attribute of <test>
# generally "../" if building in a subdirectory.

def main():
    # Read the regtest control file.
    controlfileName = sys.argv[1]
    prefix = ""
    if len(sys.argv) > 2:
         prefix = sys.argv[2]
    control = ReadControlFile(controlfileName,prefix)
    BuildQMTestTree(control)

def ReadControlFile(fn,prefix):
    """Read a regtest XML control file.

    Reads an XML file, and looks for <test> and <option> tags in it.
    Each of the <test>s is read for its root attribute, naming the
    subdirectories of the top-level directory to test.  The <option>s
    are read for their target attribute, naming which compiler backend
    to use, and the content of these tags are taken as additional
    options to strc.

    'fn' -- Name of the regtest control file.

    returns -- A dictionary, where the 'testroots' element contains
    a list of the root strings, the 'targets' element contains
    a list of pairs of (target, options), and the 'whichtypes' element
    contains a list of benchmark 'types' to execute."""
    
    dom = xml.dom.minidom.parse(fn)
    # From this, get the list of test roots:
    testroots = []
    tests = dom.getElementsByTagName('test')
    for node in tests:
        testroots.append(prefix + node.getAttribute('root'))
    # And the set of targets, as pairs of (target, options).
    targets = []
    options = dom.getElementsByTagName('option')
    for node in options:
        target = node.getAttribute('target')
        onode = node.firstChild
        # ASSERT: onode is a text node, or None
        opts = ''
        if onode:
            opts = onode.data
        targets.append((target, opts))
    # Get classifications of tests: regtest, checkin, ...
    whichtypes = []
    testtypes = dom.getElementsByTagName('whichtype')
    for node in testtypes:
        tnode = node.firstChild
        # ASSERT: tnode is a text node
        if (tnode and tnode.data != ''):
            whichtypes.append(tnode.data)
    whichtypes.sort()
    dom.unlink()
    return { 'testroots': testroots,
             'targets': targets,
             'whichtypes' : whichtypes,
             'prefix' : prefix}

def BuildQMTestTree(control):
    """Build a QMTest XML tree.

    The tree is built under the 'QMTest' directory of the current
    working directory.

    'control' -- Results of parsing the regtest control file, as
    returned from 'ReadControlFile()'..  
    { 'testroots': testroots, 'targets': targets, 'whichtypes' : whichtypes }
    where testroots is a list of directories to search in BuildQMTestTree
    targets, whichtypes are processed by DoQMTestDir

    'includes' a list of specific targets to match the 'includedin'
    attribute of 'impl'"""

    def visit(arg, dirname, names):
#        print >> sys.stderr, "dirname = " + dirname
#        print >> sys.stderr, "names = " + (" ".join(names))
        if 'benchmark.xml' in names:
            DoQMTestDir(dirname, control)

    for r in control['testroots']:
        os.path.walk(r, visit, None)

def DoQMTestDir(path, control):
    """Create a QMTest directory for a given path.

    The provided directory must have a 'benchmark.xml' file.
    This creates the corresponding QMTest directories, and copies
    files from the original directory to the test directories.

    'path' -- Path to the benchmark in the source tree.

    'control' -- Results of parsing the regtest control file, as
    returned from 'ReadControlFile()'.  
    { 'testroots': testroots, 'targets': targets, 'whichtypes' : whichtypes }
    where testroots is a list of directories to search in BuildQMTestTree
    targets is list of backend, option pairs for use with StreamIt compiler,
    and whichtypes is a list of benchmark 'types' to execute."""

    benchmarkname = os.path.join(path, 'benchmark.xml')

    #print >>sys.stderr, "Path " + path

    prefix = control['prefix']
    outpath = path[len(prefix):len(path)]
    qmname = '.'.join(map(lambda p: QMSanitize(p), SplitAll(outpath)))
    qmdir = DirToQMDir(outpath)
    
    #print >> sys.stderr, "Benchmark name " + benchmarkname
    dom = xml.dom.minidom.parse(benchmarkname)
    # We basically want to ignore the entire benchmark.xml file, except
    # for the <implementations>.
    implementationss = dom.getElementsByTagName('implementations')
    implementations = implementationss[0]
    impls = implementations.getElementsByTagName('impl')
    # Only StreamIt implementations are interesting.
    impls = filter(lambda i: i.getAttribute('lang') == 'StreamIt', impls)
    seq = 0
    whichtypes = control['whichtypes']
    for impl in impls:
        
        # If benchmark has regtest="skip" field, skip it
        if (impl.getAttribute('regtest') == 'skip'): continue

        # get compiletime and runtime if present
        compile_time = impl.getAttribute('compile_time')
        run_time = impl.getAttribute('run_time')
        iters = impl.getAttribute('iters')
        
        # if regtest is looking for markers for a certain subset of benchmarks
        # and if this benchmark has 'whichtype' markers, and there is no
        # intersection between the sought markers and the markers on this
        # benchmark, then skip this benchmark.
        if (len(whichtypes) > 0):
            twhichtypes = []
            testtypes = impl.getElementsByTagName('whichtype')
            for node in testtypes:
                tnode = node.firstChild
                # ASSERT: tnode is a text node
                if (tnode and tnode.data != ''):
                    twhichtypes.append(tnode.data)
            twhichtypes.sort()

            if ((len(twhichtypes) > 0)
                and EmptyIntersectionOfSorted(whichtypes, twhichtypes)):
                continue

        # Where are the source files?  If the implementation has
        # a dir attribute, they're in that subdirectory.
        srcdir = path
        subdir = impl.getAttribute('dir')
        if subdir: srcdir = os.path.join(srcdir, subdir)

        # What is the name of this implementation?  Number them
        # sequentially, but override if the implementation has an
        # id attribute.  Don't use a separate name if there's only
        # one implementation.
        if len(impls) == 1:
            benchname = qmname
            benchdir = qmdir
        else:
            seq = seq + 1
            id = impl.getAttribute('id')
            if not id: id = "impl%d" % seq
            id = QMSanitize(id)
            benchname = qmname + "." + id
            benchdir = os.path.join(qmdir, id + ".qms")

        
        # Create the benchmark directory.
        if not os.path.exists(benchdir):
            os.makedirs(benchdir)

        # Run appropriate Makefile if any
        # BUG? this make not run under qmtest so any errors not reported.
        # on other hand some makefiles may use relative directories so
        # not possible to run except in srcdir.
        makes=impl.getElementsByTagName('make')
        for make in makes:
            mnode=make.firstChild
            mn=mnode.data
            print >> sys.stderr, "cd "+srcdir+";make -f "+mn
            run=os.popen("cd "+srcdir+";make -f "+mn,'r')
            run.close()
            
        # Walk through the files.  Classify them by their class
        # attribute, and also copy them into benchdir.
        # You can now put more than one file in a single <file> tag.
        # separate files by spaces (may change of ever support Windoze)
        # fileset is a map from class to sets of filenames.
        fileset = {}
        files = impl.getElementsByTagName('file')
        for file in files:
            fnode = file.firstChild
            fn = fnode.data
            cls = file.getAttribute('class')
            if not cls: cls = 'source'
            is_relative = file.getAttribute('relative')
            if is_relative: cls = cls + '_relative'
            if cls not in fileset: fileset[cls] = []
            fileset[cls].extend(fn.split())

        ActuallyBuildTests(srcdir, benchdir, fileset, benchname, control, compile_time, run_time, iters)

    dom.unlink()

def EmptyIntersectionOfSorted(sorted1, sorted2):
    # return true if sorted lists have empty intersection, false otherwise
    i1 = 0; i2 = 0
    n1 = len(sorted1); n2 = len(sorted2)
    while (i1 < n1 and i2 < n2):
        c = cmp(sorted1[i1],sorted2[i2])
        if (c == 0):
            return 0
        elif (c < 0):
            i1 = i1 + 1
        else:
            i2 = i2 + 1
    return 1

def DirToQMDir(path):
    """Convert a source-tree path to a QMTest path.

    We replicate the normal directory tree for use by QMTest.  Each
    component of the directory name gets a '.qms' extension added so
    that QMTest will recognize it.

    'path' -- Name of the directory to convert.

    returns -- Name of the directory for QMTest's benefit."""

    parts = SplitAll(path)
    parts = map(lambda p: QMSanitize(p) + '.qms', parts)
    path = apply(os.path.join, parts)
    return path

def SplitAll(path):
    """Break a path into its component parts.

    The normal Python 'os.path.split()' only removes the tail part of
    a path; this function splits it fully.

    'path' -- A directory name.

    returns -- A list containing the components of the directory name."""
    
    parts = []
    while path != '':
        (head, tail) = os.path.split(path)
        parts = [tail] + parts
        path = head
    return parts

def ActuallyBuildTests(srcdir, benchdir, fileset, benchname, control, compile_time, run_time, iters):
    """Actually construct the QMTest XML test files in a directory.

    'srcdir' -- Directory containing the original benchmark.

    'benchdir' -- Directory in which to create the test files.

    'fileset' -- Mapping from type of file to lists of filenames.
      we expect keys in source, output, data, data_relative

    'benchname' -- QMTest base name for this set of tests.

    'control' -- Regtest control structure.

    'compile_time' -- override of default compile time or None

    'run_time' -- override of default run time or None

    'iters' -- override of iteration count or None"""

    for (target, opts) in control['targets']:
        testname = MakeOptionName(target, opts)
        testdir = os.path.join(benchdir, testname + ".qms")
        if not os.path.exists(testdir):
            #print >> sys.stderr, "making directory " + testdir
            os.makedirs(testdir)
        # Start by copying all of the files over.
        for c,l in fileset.iteritems():
            for fn in l:
                src = os.path.normpath(os.path.join(srcdir, \
                                                    os.path.expandvars(fn)))
                dst = "";
                may_need_dir = c.endswith('_relative')
                if may_need_dir:
                    dst = os.path.normpath(os.path.join(testdir, \
                                                    os.path.expandvars(fn)))
                    
                else:
                    dst = os.path.join(testdir, os.path.basename(fn))
                try:
                    #print >>sys.stderr, "Trying to copy from " + src + " to " + dst
                    if may_need_dir:
                        if not os.path.exists(os.path.dirname(dst)):
                            os.makedirs(os.path.dirname(dst))
                    if not os.path.isfile(dst):
                        shutil.copyfile(src, dst)
                except:
                    print >> sys.stderr, "Unexpected error: ", sys.exc_info()[0]
                    print >> sys.stderr, "Failure to copy file " + \
                          src + " to " + dst + \
                          " may cause errors later in test process"
                    
        # Three stages, write out the files.
        extras = { 'opts': opts,
                   'testname': benchname + '.' + testname,
                   'compile_time': compile_time,
                   'run_time': run_time,
                   'iters': iters}
        for (GetDOM, fn) in [(GetCompileDOM, 'compile.qmt'),
                             (GetRunDOM, 'run.qmt'),
                             (GetVerifyDOM, 'verify.qmt')]:
            dom = GetDOM(target, fileset, extras)
            if dom:
                f = open(os.path.join(testdir, fn), 'w')
                dom.writexml(f)
                f.close()
                dom.unlink()

def GetCompileDOM(target, fileset, extras):
    """Build an XML DOM for the 'compile' part of the test.

    'target' -- String target for the compiler.

    'fileset' -- Mapping from type of file ('source' matters) to lists
    of filenames.

    'extras' -- Mapping describing additional parameters available.
    'opts' should map to a string or list containing extra parameters
    to the StreamIt compiler, 'strc'."""
    

    impl = xml.dom.minidom.getDOMImplementation()
    doc = impl.createDocument(None, 'extension', None)
    extension = doc.documentElement
    extension.setAttribute('class', 'streamit.RunStrcTest')
    extension.setAttribute('kind', 'test')

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'backend')
    CreateTextElement(doc, argument, 'enumeral', target)

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'options')
    set = doc.createElement('set')
    argument.appendChild(set)
    for opt in extras['opts'].split():
        CreateTextElement(doc, set, 'text', opt)

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'filenames')
    set = doc.createElement('set')
    argument.appendChild(set)
    for fn in fileset['source']:
        CreateTextElement(doc, set, 'text', os.path.basename(fn))

    if extras['compile_time']:
        argument = doc.createElement('argument')
        extension.appendChild(argument)
        argument.setAttribute('name', 'timeout')
        CreateTextElement(doc, argument, 'integer',str(extras['compile_time']))

    iters = str(100)
    if extras['iters']:
        iters = extras['iters']

    # fall back to old form: iters = runopts[1]
    CreateRunOpts(doc, extension, iters=iters, output='output')
    
    return doc

def GetRunDOM(target, fileset, extras):
    """Build an XML DOM for the 'run' part of the test.

    'target' -- String target for the compiler.

    'fileset' -- Mapping from type of file ('source' matters) to lists
    of filenames.

    'extras' -- Mapping describing additional parameters available.
    'testname' should contain the basename of this set of tests"""

    # Not relevant for 'library' which combines its run stage into its compile
    # stage.
    # 
    if target == 'library':
        return None
    impl = xml.dom.minidom.getDOMImplementation()
    doc = impl.createDocument(None, 'extension', None)
    extension = doc.documentElement
    extension.setAttribute('class', 'streamit.RunProgramTest')
    extension.setAttribute('kind', 'test')

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'prerequisites')
    set = doc.createElement('set')
    argument.appendChild(set)
    tuple = doc.createElement('tuple')
    set.appendChild(tuple)
    CreateTextElement(doc, tuple, 'text', extras['testname'] + '.compile')
    CreateTextElement(doc, tuple, 'enumeral', 'PASS')

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'backend')
    CreateTextElement(doc, argument, 'enumeral', target)

    if extras['run_time']:
        argument = doc.createElement('argument')
        extension.appendChild(argument)
        argument.setAttribute('name', 'timeout')
        CreateTextElement(doc, argument, 'integer',str(extras['run_time']))

    iters = str(100)
    if extras['iters']:
        iters = extras['iters']

    # fall back to old form: iters = runopts[1]
    CreateRunOpts(doc, extension, iters=iters, output='output')
    
    return doc

def GetVerifyDOM(target, fileset, extras):
    """Build an XML DOM for the 'verify' part of the test.

    'target' -- String target for the compiler.

    'fileset' -- Mapping from type of file ('output' matters) to lists
    of filenames.

    'extras' -- Mapping describing additional parameters available.
    'testname' should contain the basename of this set of tests.

    'timeout -- ignored for now"""

    # Punt now if there's no output file.
    if 'output' not in fileset:
        return None

    impl = xml.dom.minidom.getDOMImplementation()
    doc = impl.createDocument(None, 'extension', None)
    extension = doc.documentElement
    extension.setAttribute('class', 'streamit.CompareResultsTest')
    extension.setAttribute('kind', 'test')

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'prerequisites')
    set = doc.createElement('set')
    argument.appendChild(set)
    tuple = doc.createElement('tuple')
    set.appendChild(tuple)
    # What's the actual name of the prereq?  Library doesn't have a
    # "run" stage.
    prereq = extras['testname']
    if target == 'library':
        prereq = prereq + '.compile'
    else:
        prereq = prereq + '.run'
    CreateTextElement(doc, tuple, 'text', prereq)
    CreateTextElement(doc, tuple, 'enumeral', 'PASS')

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'backend')
    CreateTextElement(doc, argument, 'enumeral', target)

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'output')
    CreateTextElement(doc, argument, 'text', 'output')

    argument = doc.createElement('argument')
    extension.appendChild(argument)
    argument.setAttribute('name', 'expected')
    CreateTextElement(doc, argument, 'text', \
                      os.path.basename(fileset['output'][0]))
    return doc

def CreateRunOpts(doc, parent, iters=100, output=''):
    """Create an XML node describing StreamIt runtime options.

    When invoking a StreamIt program, we need to know how long the
    program runs for and where its output goes.  This is a single
    QMTest XML field (described by the 'streamit.RunOptionsField'
    object).  This function creates that XML 'argument' field and adds
    it to the specified 'parent'.

    'doc' -- XML document node.

    'parent' -- Parent of the node to create.

    'iters' -- Number of iterations to run the program for.

    'output' -- Name of the output file."""

    argument = doc.createElement('argument')
    parent.appendChild(argument)
    argument.setAttribute('name', 'iters')
    CreateTextElement(doc, argument, 'integer', str(iters))

    argument = doc.createElement('argument')
    parent.appendChild(argument)
    argument.setAttribute('name', 'own_output')
    CreateTextElement(doc, argument, 'text', output)



def CreateTextElement(doc, parent, name, content):
    """Create an XML node with a text element for a child.

    Shorthand to create an XML node containing only text and insert
    it in the parent node.  This is for nodes like
    <node>Contents</node>, which show up somewhat regularly.

    'doc' -- XML document node.

    'parent' -- Parent of the node to create.

    'name' -- Name of the XML element to create.

    'content' -- Text content of the element."""
    node = doc.createElement(name)
    node.appendChild(doc.createTextNode(content))
    parent.appendChild(node)

def MakeOptionName(target, opts):
    """Return a test name suitable for QMTest.

    This test name should be the directory part of a test name.
    It encapsulates the target and option list, but is still a valid
    QMTest test name.

    'target' -- Name of the compiler backend target, should be one
    of 'library', 'uni', 'cluster', 'rstream', or 'raw4'.

    'opts' -- Additional options to pass to strc."""

    name = target
    if opts: name = "%s %s" % (target, opts)
    return QMSanitize(name)

def QMSanitize(name):
    """Turn an arbitrary string into a QMTest test name."""
    # A test name can only contain lowercase letters, numbers,
    # and _.  So, let's sanitize a little:
    name = name.lower()
    name = re.sub(" +", "_", name)
    name = re.sub("[^a-z0-9_]+", "", name)
    return name

main()
