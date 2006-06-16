#!/usr/uns/bin/perl
#
# dat2bin.pl: convert formatted data to binary
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: dat2bin.pl,v 1.4 2006-06-16 17:10:03 thies Exp $
#
# Use this script to convert data from a text file to native binary
# data for use with a StreamIt FileReader object.
#
#   dat2bin.pl --bit <file> <output>
#     Reads <file>, and converts '0' to 0x00000000 and '1' to 0x00000001.
#     For use with FileReader<bit> filters.
#   dat2bin.pl --int <file> <output>
#     Reads <file> and writes default-length, default-endian binary.
#   dat2bin.pl --float <file> <output>
#     Reads <file> and writes default-format single-precision floats.
#   dat2bin.pl --ppm <file> <output>
#     Reads image .ppm <file> and writes RGB data as binary integers
#     (.ppm header is ignored)
#
# For bit, all whitespace is ignored, and bits are written in big-endian
# order.  For all other formats, words are whitespace-separated.

use Getopt::Long;
use IO::File;
use vars qw($format $in $out);

$format = "int";
my $result = GetOptions("bit" => sub { $format = "bit"; },
			"int" => sub { $format = "int"; },
			"float" => sub { $format = "float"; },
                        "ppm" => sub { $format = "ppm"; });

$in = \*STDIN;
$in = new IO::File("<$ARGV[0]") if @ARGV >= 1;
$out = \*STDOUT;
$out = new IO::File(">$ARGV[1]") if @ARGV >= 2;

# kill first 4 lines of ppm file, as they do not represent data
if ($format eq "ppm") {
  # I don't know perl, this seems to advance the line
  my $line = $_; if (<$in>) {}
  my $line = $_; if (<$in>) {}
  my $line = $_; if (<$in>) {}
  my $line = $_; if (<$in>) {}
}

while (<$in>)
  {
    my $line = $_;
    chomp;
    if ($format eq "bit")
      {
	my @bits = split /\s*/, $line;
	print $out pack("i*", @bits);
      }
    if ($format eq "int")
      {
	my @ints = split /\s+/, $line;
	print $out pack("i*", @ints);
      }
    if ($format eq "ppm")
      {
        # remove leading whitespace, since it is interpreted as a 0 by split
        $line =~ s/^\s+//;
        # ppms could really be byte's instead of int's, but StreamIt
        # doesn't have a byte primitive type yet...
	my @ints = split /\s+/, $line;
	print $out pack("i*", @ints);
      }
    if ($format eq "float")
      {
	my @floats = split /\s+/, $line;
	print $out pack("f*", @floats);
      }
  }

close $in;
close $out;

