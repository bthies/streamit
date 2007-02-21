all:
	if test -d src; then $(MAKE) -C src; fi
	if test -d library/c; then $(MAKE) -C library/c; fi
	if test -d misc/compression; then $(MAKE) -C misc/compression; fi
	$(MAKE) -C library/cluster

clean:
	if test -d src; then $(MAKE) -C src clean; fi
	if test -d library/c; then $(MAKE) -C library/c clean; fi
	if test -d misc/compression; then $(MAKE) -C misc/compression clean; fi
	$(MAKE) -C library/cluster clean
