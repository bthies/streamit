#!/usr/uns/bin/perl
#
# Usage: make_daily_log.pl logfile log_dir
#
# Copies the logfile into a file in log_dir which is named after the current time;
#
# $Id: make_daily_log.pl,v 1.2 2007-06-19 06:27:19 thies Exp $
use strict;

my $logfile_name      = shift(@ARGV) || die ("no logfile name passed");
my $logfile_directory = shift(@ARGV) || die ("no log directory passed");

# get the current date/time
my $current_date = `/bin/date`;
chomp($current_date);

# substitute all wacky filename characters for normal ones
$current_date =~ s/ +/-/g; # remove spaces
$current_date =~ s/:/_/g; # remove colons

my $new_filename = "$logfile_directory$current_date.log";

# copy the logfile to the log directory in a file named $current_date
`cp $logfile_name $new_filename`;

# print new filename to stdout
print $new_filename

