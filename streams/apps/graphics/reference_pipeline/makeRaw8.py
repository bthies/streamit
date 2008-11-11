#!/usr/uns/bin/python

import os

exec_string = '../../scripts/strcm.py GPUModel.filterlist'
print( exec_string )
os.system( exec_string )

exec_string = 'strc -N3 -r8 -u256 --nopartition --noanneal --spacedynamic --malloczeros --altcodegen --removeglobals --macros --wbs --destroyfieldarray --layoutfile manual_layout.txt --devassignfile device_assignment.txt GPUModel.str'
print( exec_string )
os.system( exec_string )
