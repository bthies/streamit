#!/usr/uns/bin/perl
#

###############################################################################
# min, mean, max, stdev of an array of numbers.
#
# attempts to give reasonable results on boundary cases:
# min, max, mean, sum  come back as "" if length == 0
# (which is whay you would want if printing to a spreadsheet).
# stdev comes back as 0 if length <= 1
#
# When passed an array of numbers, it returns an associative array with
# values for "length", "min", "mean", "max", "stdev"
#
# Use as:
#
# require $ENV{STREAMIT_HOME} . "/misc/scripts/NumericArraySummary.perl";
# ...
# my $foo_summary = numericArraySummary(@foo);
###############################################################################

use warnings;
use strict;

# stdev calculation is not obvious:  It is calculated in a way that avoids
# as much as possible adding or subtracting large numbers while running in
# a single pass (Hanson'e method??).  See
# http://www.megspace.com/science/sfe/i_ot_std.html 
#
# Note that the two-pass algorithm is numerically stable, 
# sqrt( sum( (mean - x_i)^2 ) / (n-1) )
# But its naive translation into a one-pass algorithm is not
# sqrt( ( n * sum(x_i^2) - (sum(x_i))^2 ) / (n * (n - 1)))
# since it involves the difference of large numbers to try to find a 
# potentially small number.


sub numericArraySummary {
my $min = "";
my $max = "";
my $mean = "";
my $stdev = 0;
my $sum = 0;
my $len = @_+0;

if ($len) {

    $min = $_[0];
    $max = $_[0];
    $sum = $_[0];

    my $d;		        # amount mean will change with next data point
    my $n = 1;			# index into array
    my $m = $_[0];		# mean so far
    my $ssd = 0;                # sum of squared deviations so far

    foreach my $x (@_[1..$len-1]) { # update starting with second element of array
        $max = $x if $x > $max;
        $min = $x if $x < $min;
        $sum += $x;

	$d = ($x - $m) / ($n + 1);
        $ssd += $n * $d * $d;
        $m += $d;
        $ssd += ($x - $m) ** 2;
        $n++;
    }

    if ($len > 1) {
	$stdev = sqrt($ssd / ($len - 1));
    }
    if ($len > 0) {
        $mean = $sum / $len; 
    }
}
my %retval = ("length" => $len,
	      "min" => $min,
	      "mean" => $mean,
	      "max" => $max,
	      "stdev" => $stdev,
	      "sum" => $sum);
return %retval;
}

1; # a file included with 'require' must return a true value
