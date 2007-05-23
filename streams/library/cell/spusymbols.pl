#! /usr/bin/perl -W

use strict;

my $csymf;
my $ldsymf;

open($csymf, ">spusymbols.h");
open($ldsymf, ">spusymbols.ld");

print $csymf ("#ifndef _SPU_SYMBOLS_H_\n" .
              "#define _SPU_SYMBOLS_H_\n");

for my $progname (@ARGV) {
    print $csymf ("extern spe_program_handle_t ${progname}_program;\n");

    my @symbols = `nm ${progname}_program`;

    for my $line (@symbols) {
        if ($line =~ m/^(.+) A _end$/) {
            my $data_start = hex($1);

            $data_start = ($data_start + 127) & ~127;

            printf $ldsymf ("${progname}_data_start = 0x%x;\n", $data_start);
        } elsif ($line =~ m/^(.+) T filter_(.+)_wf$/) {
            my $symbol = "wf_" . $2;
            my $lsa = hex($1);

            print $csymf ("extern void *${symbol};\n");
            printf $ldsymf ("${symbol} = 0x%x;\n", $lsa);
        }
    }
}

print $csymf ("#endif\n");

close($csymf);
close($ldsymf);
