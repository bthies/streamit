default-rule: setup

##### user defines.  these may be changed.

# include user customizations
-include custom/Makefile.user.$(USER)

# get definition of MASONDIR from Makefile.rawdist
include Makefile.rawdist

STARBUILD = $(MASONDIR)/starbuild

#### derived defines.  these may not be changed.

ifneq ($(MAKECMDGOALS),clean-setup)
include starbuild/Makefile
endif

##### rules

setup:
	$(STARBUILD)/clean-setup
	$(STARBUILD)/setup $(STARBUILD)

clean-setup:
	$(STARBUILD)/clean-setup

starbuild/Makefile:
	$(STARBUILD)/setup $(STARBUILD)

upgrade:
	cvs up Makefile Makefile.rawdist
	$(MAKE) setup
