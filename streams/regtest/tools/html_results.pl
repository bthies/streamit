#!/usr/local/bin/perl

# Script to parse the results of running the regression test framework
# and generate a nice summary of the results that can be viewed from RT.
#
# Usage: parse_results prefix log_file.txt error_file.txt success_file.txt
#   results_file.txt summary.html listing.html
# (where prefix is the directory containing the tests,
#  log_file is the output from running make in streams/regtest,
#  error_file is the file containing the error messages from the tests,
#  success_file is the file containing the successful regression tests,
#  results_file contains the results (eg speed) of running the various
#   different configurations of the compiler,
#  summary is the output file receiving a summary blurb, and
#  listing is the output file receiving a complete listing.)
#

# Note: The contents of the error file are not acutally used at the
# present time. error messages are parsed out of the log file instead.
#
# $Id: html_results.pl,v 1.3 2003-09-03 19:08:44 dmaze Exp $

use strict;

# entry point
main();


sub main {
    my $prefix           = shift(@ARGV) || die ("No prefix supplied\n");
    my $log_filename     = shift(@ARGV) || die ("No log filename supplied\n");
    my $error_filename   = shift(@ARGV) || die ("No error filename supplied\n");
    my $success_filename   = shift(@ARGV) || die ("No success filename supplied\n");
    my $summary_filename = shift(@ARGV) || die ("No summary filename supplied\n");
    my $listing_filename = shift(@ARGV) || die ("No listing filename supplied\n");

    # read in the contents of the log, error, and success files
    my $log_contents     = read_file($log_filename);
    my $error_contents   = read_file($error_filename);
    my $success_contents = read_file($success_filename);

    my %success;
    my %failure;

    parse_logs($prefix, $log_contents, $error_contents, $success_contents,
	       \%success, \%failure);

    generate_summary($summary_filename, $log_contents, \%success, \%failure);
    generate_listing($listing_filename, \%success, \%failure);
}

sub generate_summary {
  my $out = shift;
  my $log = shift;
  my $success = shift;
  my $failure = shift;

  # find the time it took to run the test, and the overall result (next line)
  my @parsed   = $log =~ m/Time: (.*)\n*(.*)/gi;
  my $run_time = shift(@parsed);
  my $result   = shift(@parsed);

  # remove any commas in the runtime
  $run_time =~ s/,//gi;

  open OUT, ">$out";
  print OUT "<table border=0 cellspacing=0 cellpadding=1 width=100%>\n";
  print OUT "<tr><th align=left>".
    "<a href=\"<% \$RT::WebPath %>/StreamIt/listing.html\">Regtest</a>".
      "</th><th>&nbsp;</th></tr>\n";
  print OUT "<tr class=oddline><td>Last run</td><td>" . `date` .
    "</td></tr>\n";
  print OUT "<tr class=evenline><td>Successes</td><td>" .
    count_total_options($success) . "</td></tr>\n";
  print OUT "<tr class=oddline><td>Failures</td><td>" .
    count_total_options($failure) . "</td></tr>\n";
  print OUT "</table>";
  close OUT;
}

sub parse_logs {
    my $prefix = shift;
    my $log = shift;
    my $errors = shift;
    my $successes = shift;
    my $succeeded = shift;    # keys=tests, values = options failed with
    my $failed = shift; # keys=tests, values = options succeeded with

    # ---------- Parse Failures ------------
    # now, we should match all of the errors, to generate a report for
    # each file not for each file with each set of options
    my @parsed =
      $log =~ m/junit.framework.AssertionFailedError: (.*?)\((.*?) \)/gi;

    # for each failure line, split it up into the (test filename) and
    # the options that were used to compile it, and save that in the
    # associative hash %$failed.
    while(@parsed) {
	my $test = shift(@parsed);
	$test =~ s/ $prefix/ /;
	my $options = shift(@parsed);
	push @{$failed->{$test}}, $options;
    }


    # ---------- Parse Successes ------------
    @parsed = split("\n", $successes); # each line contains a success
    while(@parsed) {
	my $current_line = shift(@parsed);
	# split line into test and options
	my ($test, $options) = $current_line =~ m/(.*)\((.*)\)/g;
	$test =~ s/ $prefix/ /;
	push @{$succeeded->{$test}}, $options;
    }
}

sub generate_regtest_item {
  my ($collection, $key, $i) = @_;
  my $result = '';

  my ($op, $path) = $key =~ /^(\S+)\s+(.*)/;
  $result .=  "<& RegtestItem, Class => '" .
    ($i%2?"oddline":"evenline") . "', Op => '$op',\n" .
      "   Path => '$path',\n";
  my $first = 1;
  foreach my $flag (@{$collection->{$key}})
    {
      if ($first) {
	$result .= "   Flags => [";
	$first = 0;
      } else {
	$result .= ",\n";
      }
      $result .= "'$flag'";
    }
  $result .= "] &>\n";

  return $result;
}

sub generate_listing {
  my $out = shift;
  my $succeeded = shift;
  my $failed = shift;
  my $i;

  open OUT, ">$out";
  print OUT '<& /Elements/Header, Title => loc("StreamIt Regtest") &>';
  print OUT '<& /Elements/Tabs, Title => loc("StreamIt Regtest") &>';
  print OUT "<table border=0 width=\"100%\">\n";
  print OUT "<tr align=top><td>\n";

  if (keys %$failed)
    {
      print OUT "<& /Elements/TitleBoxStart, title => \"" .
	count_total_options($failed) . " Failures\", bodyclass => '' &>\n";

      $i = 0;
      foreach my $key (sort keys %$failed)
	{
	  $i++;
	  print OUT generate_regtest_item($failed, $key, $i);
	}
      print OUT "<& /Elements/TitleBoxEnd &>\n";
    }

  if (keys %$succeeded)
    {
      print OUT "<& /Elements/TitleBoxStart, title => \"" .
	count_total_options($succeeded) . " Successes\", bodyclass => '' &>\n";

      $i = 0;
      foreach my $key (sort keys %$succeeded)
	{
	  $i++;
	  print OUT generate_regtest_item($succeeded, $key, $i);
	}
      print OUT "<& /Elements/TitleBoxEnd &>\n";
    }

  print OUT "</td></tr>\n";
  print OUT "</table>\n";
  close OUT;
}


# reads in a file and returns its contents as a scalar variable
# usage: read_file($filename)
sub read_file {
    my $filename = shift || die ("no filename passed to read_file\n");

    open(INFILE, "<$filename") || die ("could not open $filename");
    
    # save the old standard delimiter
    my $old_delim = local($/);
    # set the standad delimiter to undef so we can read
    # in the file in one go
    local($/) = undef; 

    # read in file contents
    my $file_contents = <INFILE>;
    close(INFILE);
    
    # restore the standard delimiter
    local($/) = $old_delim;

    return $file_contents;
}


# counts the number of compiler options there are in the success or failure
# hashes. These hashes each have "Compile|Execute|Verify filename" as keys
# and multiple lines of <sp><sp><sp>options
# this sub go through and counts all of those options for all keys, so we can report the
# number of successes and the number of failures that we have
sub count_total_options {
  my $hashref = shift || die ("No hash ref passed to count_total_options");

  my $total_options = 0;
  foreach my $current_key (keys %$hashref) {
    $total_options += @{$hashref->{$current_key}};
  }
  return $total_options;
}


# stupid perl syntax
1;

