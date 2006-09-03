all:
	if test -d src; then $(MAKE) -C src; fi
	if test -d library/c; then $(MAKE) -C library/c; fi
	$(MAKE) -C library/cluster

clean:
	if test -d src; then $(MAKE) -C src clean; fi
	if test -d library/c; then $(MAKE) -C library/c clean; fi
	$(MAKE) -C library/cluster clean
