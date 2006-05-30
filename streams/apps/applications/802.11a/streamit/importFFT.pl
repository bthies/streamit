#!/usr/local/bin/perl

# Writes out all of FFT6 except for the void->void stream.  We need to
# do this since strc expects only one void->void stream in the input
# files.

open(MYFILE, "../../../benchmarks/fft/streamit/FFT6.str") or die("Cannot find FFT6.str.\n");
{
    local $/;
    $text = <MYFILE>;
}

$text =~ s!void->void.*complex->complex filter CombineDFT!complex->complex filter CombineDFT!is;

print $text;
close(MYFILE);
