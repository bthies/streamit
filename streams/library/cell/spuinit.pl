#! /usr/bin/perl -W

use strict;

if ($#ARGV + 1 != 6) {
    print STDERR ("spuinit.pl: SPU_LAYOUT must specify programs for all 6 SPUs.\n");
    exit(1);
}

my $cincf;
open($cincf, ">spuinit.inc");

print $cincf ("#ifndef _SPUINIT_INC_\n" .
              "#define _SPUINIT_INC_\n");
print $cincf ("static void\nspuinit()\n{\n");

for (my $i = 0; $i <= $#ARGV; $i++) {
    my $progname = $ARGV[$i];

    printf $cincf ("spu_info[${i}].program = &${progname}_program;\n");
    printf $cincf ("spu_info[${i}].data_start = " .
                   "(LS_ADDRESS)&${progname}_data_start;\n");
}

print $cincf ("}\n");
print $cincf ("#endif\n");

close($cincf);
