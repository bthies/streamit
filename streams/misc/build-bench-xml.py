#!/usr/uns/bin/python

import os.path

# List of pairs of (dirname, tagged).
dirs = []

def main():
    def visit(arg, dirname, names):
        global dirs
        # Update dirs:
        if len(dirs) > 0:
            lastdir = apply(os.path.join, map(lambda p: p[0], dirs))
        else:
            lastdir = ''
        prefix = os.path.commonprefix([lastdir, dirname])
        preparts = os.path.split(prefix)
        preparts = filter(lambda p: p != '', preparts)
        dirparts = os.path.split(dirname)
        dirparts = filter(lambda p: p != '', dirparts)
        for (d,t) in dirs[len(preparts):]:
            if t:
                print "</dir>"
        dirs = dirs[:len(preparts)] + \
               map(lambda d: (d, 0), dirparts[len(preparts):])

        if 'benchmark.xml' in names:
            for (d,t) in dirs:
                if not t:
                    print '<dir name="%s">' % (d,)
            dirs = map(lambda p: (p[0], 1), dirs)
            f = open(os.path.join(dirname, 'benchmark.xml'))
            print f.read()
            f.close()

    print '<benchset>'
    os.path.walk('.', visit, None)
    for (d,t) in dirs:
        if t:
            print "</dir>"
    print '</benchset>'
    
main()
