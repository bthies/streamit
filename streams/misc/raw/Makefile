#
# top level makefile for a source tree that uses the raw tool chain
# 
# To create a raw tool chain compliant source tree:
# 1. copy install/starbuild/seedfiles/Makefile to <sourcetree>/Makefile
# 2. copy install/starbuild/seedfiles/Makefile.dist to <sourcetree>/Makefile.dist
#
# update Makefile.dist with the details of the raw tool chain you are using
#
# 8-18-03 MBT
#
#
# MBT: stupid, but user customizations tend to use the OS variable
#

OS := $(shell uname -s)

default-rule: setup

##### user defines.  these may be changed.

# include user customizations
-include custom/Makefile.user.$(USER)

# get definition of MASONDIR from Makefile.rawdist
include Makefile.rawdist
STARBUILD = $(MASONDIR)/starbuild

##### rules for setting up the repository

ifeq ($(wildcard $(STARBUILD)/common/Makefile.setup),$(STARBUILD)/common/Makefile.setup)

include $(MASONDIR)/starbuild/common/Makefile.setup

else  # this is for backwards compatibility

ifneq ($(MAKECMDGOALS),clean-setup)
include starbuild/Makefile
endif

$(warning Using starbuild backwards compatible mode x$(wildcard $(MASONDIR)/starbuild/Makefile.setup)x)
setup:
	$(STARBUILD)/clean-setup
	$(STARBUILD)/setup $(STARBUILD)
	@echo "**** Reconfigured for new toolchain selection. ****" ;
	@echo "**** Ignore an errors numbered 253.            ****" ; exit -3

clean-setup:
	$(STARBUILD)/clean-setup

starbuild/Makefile:
	$(STARBUILD)/setup $(STARBUILD)

upgrade:
	cvs up Makefile Makefile.rawdist
	$(MAKE) setup

endif
