#!/usr/bin/python

import getopt
import os
import string
import sys
from testlib import *

def get_options(args):
    optlist, args = getopt.getopt(args, '',
                                  ['checkout', 'nocheckout',
                                   'buildsys', 'nobuildsys',
                                   'test', 'notest',
                                   'run', 'norun',
                                   'root=', 'libdir=', 'control=',
                                   'debug', 'profile', 'cflags=',
                                   'sflags=', 'case=', 'all'])
    for (opt, val) in optlist:
        if opt == '--nocheckout': opts.checkout = 0
        if opt == '--checkout':   opts.checkout = 1
        if opt == '--nobuildsys': opts.buildsys = 0
        if opt == '--buildsys':   opts.buildsys = 1
        if opt == '--notest':     opts.test = 0
        if opt == '--test':       opts.test = 1
        if opt == '--norun':      opts.run = 0
        if opt == '--run':        opts.run = 1
        if opt == '--root':       opts.set_root(val)
        if opt == '--libdir':     opts.libdir = val
        if opt == '--control':    opts.control = val
        if opt == '--debug':      opts.cflags = '-g'
        if opt == '--profile':    opts.profile = 1
        if opt == '--cflags':     opts.cflags = val
        if opt == '--sflags':     opts.sflags = val
        if opt == '--case':       opts.cases.append(val)
        if opt == '--all':        opts.all = 1
    return args


args = get_options(sys.argv[1:])
set = ControlReader(StreamItTestFactory()).read_control(opts.control)
if opts.checkout:
    raise NotImplementedError("Checkout not supported yet")
if opts.cases != []:
    set = set.limit(opts.cases)
elif not opts.all:
    set = set.limitToEnabled()
if opts.buildsys:
    raise NotImplementedError("System build not supported yet")
if opts.test:
    set.run_tests()

