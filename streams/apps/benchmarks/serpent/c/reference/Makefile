# $Id: Makefile,v 1.1 2006-06-19 23:33:20 rabbah Exp $
# Makefile for GNU make for the C reference implementation of Serpent

# This file is part of the C reference implementation of Serpent.
#
# Written by Frank Stajano,
# Olivetti Oracle Research Laboratory <http://www.orl.co.uk/~fms/> and
# Cambridge University Computer Laboratory <http://www.cl.cam.ac.uk/~fms27/>.
# 
# (c) 1998 Olivetti Oracle Research Laboratory (ORL)
#
# Original (Python) Serpent reference development started on 1998 02 12.
# C implementation development started on 1998 03 04.
#
# Serpent cipher invented by Ross Anderson, Eli Biham, Lars Knudsen.
# Serpent is a candidate for the Advanced Encryption Standard.



# Gnu make cheat sheet ;-)
# --------------------
# $@ = name of target
# $< = name of first dependency
# $^ = names of all dependencies, separated by spaces

CC = gcc34
CFLAGS = -g  #ansi -pedantic -Wall -O3
# We want the reference code to be as clean as it can be


%: %.o serpent-reference.o serpent-aux.o
	$(CC) -o $@ $(CFLAGS) $^


# This first target is a minimal test run with my "old" main program just
# to see that things still work
zerotest: serpent-test
	./$< -e \
        -k 0000000000000000000000000000000000000000000000000000000000000000 \
        -p 00000000000000000000000000000000
	./$< -d \
        -k 0000000000000000000000000000000000000000000000000000000000000000 \
        -c 00000000000000000000000000000000
# The correct answers to this are, respectively,
# cipherText = A954AEBC22BBE85F60591526CC0287D7
# plainText = 1F356DBD0829FFB383CBF6629551DBD7



%.txt: %
	./$< > $@
# Note that, with the unoptimised reference implementation, the Monte Carlo
# Test outputs take several hours each. Consequently, the default target
# won't make them. If you want them, do "make alltest" when you're ready to
# wait.

kat: ecb_vk.txt ecb_vt.txt ecb_iv.txt ecb_tbl.txt

mct: ecb_e_m.txt ecb_d_m.txt cbc_e_m.txt cbc_d_m.txt

alltest: kat mct


allexe: ecb_vk ecb_vt ecb_iv ecb_tbl \
	ecb_e_m ecb_d_m cbc_e_m cbc_d_m \
	serpent-test


# Dependencies
#
# These are easy ones that can be made by the implicit %.c -> %.o rule, so
# we only list the dependencies and not how to build the target.
serpent-reference.o: serpent-reference.c serpent-reference.h \
	serpent-api.h serpent-tables.h serpent-aux.h
serpent-aux.o: serpent-aux.c serpent-aux.h serpent-api.h
ecb_vk.o: ecb_vk.c serpent-aux.h serpent-api.h
ecb_vt.o: ecb_vt.c serpent-aux.h serpent-api.h
ecb_tbl.o: ecb_tbl.c serpent-aux.h serpent-api.h
ecb_iv.o: ecb_iv.c serpent-aux.h serpent-api.h
ecb_e_m.o: ecb_e_m.c serpent-aux.h serpent-api.h
ecb_d_m.o: ecb_d_m.c serpent-aux.h serpent-api.h
cbc_e_m.o: cbc_e_m.c serpent-aux.h serpent-api.h
cbc_d_m.o: cbc_d_m.c serpent-aux.h serpent-api.h
serpent-test.o: serpent-test.c serpent-api.h



# This recompiles the core of serpent in a way that includes extra
# printouts to show the internal state at various stages
serpent-reference-show-internals.o: serpent-reference.c serpent-reference.h \
	serpent-api.h serpent-tables.h serpent-aux.h
	$(CC) $(CFLAGS) -DSHOW_INTERNALS -c $< -o $@

# ...And we need to say that the "internal values" test needs to be linked
# against that special core.
ecb_iv: ecb_iv.o serpent-reference-show-internals.o serpent-aux.o
	$(CC) -o $@ $(CFLAGS) $^


ecb_tbl_plaintext_only.txt: ecb_tbl_precomputed.txt 
	fgrep PT ecb_tbl_precomputed.txt > ecb_tbl_plaintext_only.txt
ecb_tbl.txt: ecb_tbl ecb_tbl_plaintext_only.txt 
	./ecb_tbl < ecb_tbl_plaintext_only.txt >ecb_tbl.txt
ecb_tbl.run: ecb_tbl ecb_tbl_pt.txt 
	./ecb_tbl < ecb_tbl_plaintext_only.txt
ecb_tbl.diff: ecb_tbl.txt
	$(DIFF) $(DIFF_FLAGS) $< ecb_tbl_precomputed.txt


# Housekeeping
clean:
	rm -f *.o *~ ecb_tbl_plaintext_only.txt
veryclean: clean
	rm -f *.exe

# --------------------------------------------------
# (For internal development use, NIST may ignore this) 

# Make this target to run the program without sending the output to a file
# (just to see if it works after you've built it)
%.run: %
	./$<



# Make this target to check the outcome of
# the test against the previous submission.

DIFF = diff
DIFF_FLAGS = -i -s
DIFF_DATA_PATH = c:/temp/eli/katmct/

%.diff: %.txt
	$(DIFF) $(DIFF_FLAGS) ./$< $(DIFF_DATA_PATH)

%.fastdiff: %.txt %_fast.txt
	$(DIFF) $(DIFF_FLAGS) $^

%.otherdiff: %_fast.txt
	$(DIFF) $(DIFF_FLAGS) ./$< $(DIFF_DATA_PATH)



# Link the fast bitslice version with the ref's main programs
serpent-bitslice.o: serpent.c serpent.h serpentsboxes.h
	$(CC) -c -O3 $< -o $@
ecb_e_m_fast: ecb_e_m.o serpent-bitslice.o serpent-aux.o
	$(CC) -o $@ $(CFLAGS) $^
ecb_d_m_fast: ecb_d_m.o serpent-bitslice.o serpent-aux.o
	$(CC) -o $@ $(CFLAGS) $^
cbc_e_m_fast: cbc_e_m.o serpent-bitslice.o serpent-aux.o
	$(CC) -o $@ $(CFLAGS) $^
cbc_d_m_fast: cbc_d_m.o serpent-bitslice.o serpent-aux.o
	$(CC) -o $@ $(CFLAGS) $^


