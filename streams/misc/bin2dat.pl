#!/usr/uns/bin/perl
#
# bin2dat.pl: convert binary to formatted data
# $Id: bin2dat.pl,v 1.3 2007-01-04 18:51:41 dimock Exp $
#
# Use this script to convert data from a native binary (output by a
# StreamIt FileWriter object) to a text or .ppm file.
#
#   bin2dat.pl --ppm <file> <x-width> <y-width> <output>
#     Reads binary <file> as output from StreamIt and outputs .ppm file
#     with pixel dimensions <x-width> x <y-width>.  Assumes each pixel
#     has max color value of 255.

use Getopt::Long;
use IO::File;

# global variables:

use vars qw($format $in $out);

# options specify input format: --bit, --int, --float, or --ppm

$format = "int";
my $result = GetOptions("bit" => sub { $format = "bit"; },
			"int" => sub { $format = "int"; },
			"float" => sub { $format = "float"; },
                        "ppm" => sub { $format = "ppm"; });

# open input and output

$in = \*STDIN;
$in = new IO::File("<$ARGV[0]") if @ARGV >= 1;
$out = \*STDOUT;

if ($format eq "ppm") {
    if (@ARGV < 3) { die "usage:  bin2dat.pl --ppm filename.bin x-dim y-dim\n"; }
    $xmax = "$ARGV[1]";
    $ymax = "$ARGV[2]";
    $out = new IO::File(">$ARGV[3]") if @ARGV >= 4;
} else {
    $out = new IO::File(">$ARGV[1]") if @ARGV >= 2;
}

# format integers:
# undo what dat2bin does to bit -- which is treat them like integers.

if ($format eq "int" || $format eq "bit") {
    $out = new IO::File(">$ARGV[1]") if @ARGV >= 2;
    my $val;
    while (0 < read $in,$val,4) {
	$val = unpack('i*', $val);
	print $out "$val\n";
    }
}

# format floats

elsif ($format eq "float") {
    $out = new IO::File(">$ARGV[1]") if @ARGV >= 2;
    my $val;
    while (0 < read $in,$val,4) {
	$val = unpack('f*', $val);
	print $out "$val\n";
    }
}

# format ppm file

elsif ($format eq "ppm") {
  # output header of .ppm file
  print $out "P3\n";
  print $out "# Created by script\n";
  print $out "$xmax $ymax\n";
  # assume max color value is 255
  print $out "255\n";

  # assume whole file is there; print it to output
  for ($j=0; $j<$ymax; $j++) {
    for ($i=0; $i<$xmax; $i++) {
      # R,G,B for each pixel
      for ($k=0; $k<3; $k++) {
        read $in,$val,4;
        $val = unpack("i*", $val);
        print $out "$val ";
        # newline every 15 values, since lines should not be longer than 70 chars in a .ppm file
        if (($j*$xmax*3+$i*3+$k) % 15 == 14) {
          print $out "\n";
        }
      }
    }
  }
  
} 

else {
  # other formats not supported yet
  warn "Format \"$format\" not supported yet.";
}

close $in;
close $out;
