#!/usr/uns/bin/python
#
# build-qmtest.py: build QMTest XML files from the StreamIt tree
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: build-qmtest.py,v 1.3 2003-11-20 18:11:46 dmaze Exp $
#

import os
import os.path
import re
import shutil
import sys
from   types import *
import xml.dom.minidom

# TODO: some option parsing.
# For now, assume sys.argv[0] has the path to the regtest control file,
# and that we're in STREAMIT_HOME.

def main():
    # Read the regtest control file.
    control = ReadControlFile(sys.argv[1])
    BuildQMTestTree(control)

def ReadControlFile(fn):
    """Read a regtest XML control file.

    Reads an XML file, and looks for <test> and <option> tags in it.
    Each of the <test>s is read for its root attribute, naming the
    subdirectories of the top-level directory to test.  The <option>s
    are read for their target attribute, naming which compiler backend
    to use, and the content of these tags are taken as additional
    options to strc.

    'fn' -- Name of the regtest control file.

    returns -- A dictionary, where the 'testroots' element contains
    a list of the root strings and the 'targets' element contains
    a list of pairs of (target, options)."""
    
    dom = xml.dom.minidom.parse(fn)
    # From this, get the list of test roots:
    testroots = []
    tests = dom.getElementsByTagName('test')
    for node in tests:
        testroots.append(node.getAttribute('root'))
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
    dom.unlink()
    return { 'testroots': testroots, 'targets': targets }

def BuildQMTestTree(control):
    """Build a QMTest XML tree.

    The tree is built under the 'QMTest' directory of the current
    working directory.

    'control' -- Results of parsing the regtest control file, as
    returned from 'ReadControlFile()'."""

    def visit(arg, dirname, names):
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
    returned from 'ReadControlFile()'."""

    benchmarkname = os.path.join(path, 'benchmark.xml')
    qmname = '.'.join(SplitAll(path))
    qmdir = DirToQMDir(path)
    dom = xml.dom.minidom.parse(benchmarkname)
    # We basically want to ignore the entire benchmark.xml file, except
    # for the <implementations>.
    implementationss = dom.getElementsByTagName('implementations')
    implementations = implementationss[0]
    impls = implementations.getElementsByTagName('impl')
    seq = 0
    for impl in impls:
        # Only StreamIt implementations are interesting.
        if impl.getAttribute('lang') == 'StreamIt':
            # Where are the source files?  If the implementation has
            # a dir attribute, they're in that subdirectory.
            srcdir = path
            subdir = impl.getAttribute('dir')
            if subdir: srcdir = os.path.join(srcdir, subdir)

            # What is the name of this implementation?  Number them
            # sequentially, but override if the implementation has an
            # id attribute.
            seq = seq + 1
            id = impl.getAttribute('id')
            if not id: id = "impl%d" % seq
            benchname = qmname + "." + id

            # Create the benchmark directory.
            benchdir = os.path.join(qmdir, id + ".qms")
            if not os.path.exists(benchdir):
                os.makedirs(benchdir)

            # Walk through the files.  Classify them by their class
            # attribute, and also copy them into benchdir.
            fileset = {}
            files = impl.getElementsByTagName('file')
            for file in files:
                fnode = file.firstChild
                fn = fnode.data
                cls = file.getAttribute('class')
                if not cls: cls = 'source'
                if cls not in fileset: fileset[cls] = []
                fileset[cls].append(fn)

            ActuallyBuildTests(srcdir, benchdir, fileset, benchname, control)

    dom.unlink()

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

def ActuallyBuildTests(srcdir, benchdir, fileset, benchname, control):
    """Actually construct the QMTest XML test files in a directory.

    'srcdir' -- Directory containing the original benchmark.

    'benchdir' -- Directory in which to create the test files.

    'fileset' -- Mapping from type of file ('source' and 'output'
    matter) to lists of filenames.

    'benchname' -- QMTest base name for this set of tests.

    'control' -- Regtest control structure."""

    for (target, opts) in control['targets']:
        testname = MakeOptionName(target, opts)
        testdir = os.path.join(benchdir, testname + ".qms")
        if not os.path.exists(testdir):
            os.makedirs(testdir)
        # Start by copying all of the files over.
        for l in fileset.itervalues():
            for fn in l:
                src = os.path.join(srcdir, fn)
                dst = os.path.join(testdir, fn)
                shutil.copyfile(src, dst)
        # Three stages, write out the files.
        extras = { 'opts': opts,
                   'testname': benchname + '.' + testname }
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
        CreateTextElement(doc, set, 'text', fn)

    # (Does benchmark.xml give us a way to specify number of iterations?)
    CreateRunOpts(doc, extension, iters=100, output='output')
    
    return doc

def GetRunDOM(target, fileset, extras):
    """Build an XML DOM for the 'run' part of the test.

    'target' -- String target for the compiler.

    'fileset' -- Mapping from type of file ('source' matters) to lists
    of filenames.

    'extras' -- Mapping describing additional parameters available.
    'testname' should contain the basename of this set of tests."""

    # Only deal with the uniprocessor and RAW paths.
    if not (target == 'uni' or target == 'raw4'):
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

    # (Does benchmark.xml give us a way to specify number of iterations?)
    CreateRunOpts(doc, extension, iters=100, output='output')
    
    return doc

def GetVerifyDOM(target, fileset, extras):
    """Build an XML DOM for the 'verify' part of the test.

    'target' -- String target for the compiler.

    'fileset' -- Mapping from type of file ('output' matters) to lists
    of filenames.

    'extras' -- Mapping describing additional parameters available.
    'testname' should contain the basename of this set of tests."""

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
    if target == 'uni' or target == 'raw4':
        prereq = prereq + '.run'
    else:
        prereq = prereq + '.compile'
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
    CreateTextElement(doc, argument, 'text', fileset['output'][0])

    return doc

def CreateRunOpts(doc, parent, iters=100, output='output'):
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
    argument.setAttribute('name', 'runopts')
    tuple = doc.createElement('tuple')
    argument.appendChild(tuple)
    CreateTextElement(doc, tuple, 'text', output)
    CreateTextElement(doc, tuple, 'integer', str(iters))

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
    of 'library', 'uni', or 'raw4'.

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
