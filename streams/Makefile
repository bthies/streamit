all:
	if test -d src; then $(MAKE) -C src; fi
	$(MAKE) -C library/c
	$(MAKE) -C library/cluster

clean:
	if test -d src; then $(MAKE) -C src clean; fi
	$(MAKE) -C library/cluster clean
	$(MAKE) -C library/c clean
