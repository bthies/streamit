#!/usr/uns/bin/python

import os.path

# List of pairs of (dirname, tagged).
dirs = []

def main():
    def visit(arg, dirname, names):
        global dirs
        # Update dirs:
        lastdir = os.path.join(map(lambda p: p[0], dirs))
        prefix = os.path.commonprefix([lastdir, dirname])
        dirparts = os.path.split(dirname)
        for (d,t) in dirs[len(prefix):]:
            if t:
                print "</dir>"
        dirs = dirs[:len(prefix)] + \
               map(lambda d: (d, 0), dirparts[len(prefix):])

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
    print '</benchset>'
    
main()
