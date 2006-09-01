#!/usr/uns/bin/perl
#
# make-dot-in.pl: convert file to file.in magically
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: make-dot-in.pl,v 1.4 2006-09-01 21:37:42 dimock Exp $
#
# This script generates .in files from useful files, so that autoconf
# can replace some values at build time.  It does two things:
#
# (1) If the first line of the file is "#!...perl -w", replaces
#     the perl part with @PERL@; if it's python, use @PYTHON@.
#
# (2) If we encounter a line that is exactly
#       # AUTOCONFISCATE <pattern>
#     then assume future lines match <pattern>.  Within those lines,
#     the pattern can contain 'var' and 'VAR', which are matched for
#     upper- and lower-case versions of a variable name.  The region
#     ends with
#       # END AUTOCONFISCATE
#     This magic should work on Bourne shell, make, Perl, and Python
#     files, which all use the same comment character.
#
########
# AD: (2) was much to complex -- and buggy -- for use with just two files.
# Makefile.vars and strc.
# instead: 
# for lines between # AUTOCONFISCATE .*  and   # END AUTOCONFISCATE
#  take word at start of line, confirm that next token is "="
#  and replace the first alphanumeric sequence after the "=" with @WORD@
#  (caveat: alphanumeric expanded to include " " and "-" for switches and
#   "." and "/" for paths.) 
# also print out the # AUTOCONFISCATE .*  and   # END AUTOCONFISCATE
# so that we can pass to .in and back.

use warnings;
use strict;

foreach my $fname (@ARGV)
  {
    open READER, "< $fname";
    open WRITER, "> $fname.in";

    my $line = 0;
    my $pattern = undef; # figure out, please
    my @fields = ();
    my $replacing = 0;

  LINE:
    while (<READER>)
      {
	$line++;
	if ($line == 1)
	  {
	    s/#!.*perl/#!\@PERL\@/;
	    s/#!.*python/#!\@PYTHON\@/;
	    print WRITER $_;
	  } elsif ($replacing)
	  {
	    if ($_ eq "# END AUTOCONFISCATE\n")
	      {
		$replacing = 0;
		print WRITER $_;
	      }
	    elsif (/\s*^(\W*)(\w+)\s*=[ \t\r\f]*([^A-Za-z0-9_.\n\/]*)[- A-Za-z0-9_.\/]*(.*)$/) {
		# occasional bug on handing $1 to uc directly in perl 5.8.0
		my $d2 = $2;
		my $ucased = uc($d2);
		my $outstring = $1 . $2 . " = " . $3 . "@" . $ucased . "@" . $4 . "\n";
		print WRITER $outstring;
		} else {
		    # do not build a release if can't make .in files
		    die "malformed line in AUTOCONFISCATE region:\n$_";
		}
	} elsif (/^\# AUTOCONFISCATE (.*)/)
	{
	    $replacing = 1;
	    print WRITER $_;
	}
	else
	{
	    print WRITER $_;
	}
    }
    
    close READER;
    close WRITER;
    
    # While we're at it, set permissions on $fname.
    chmod 0777 & ~umask, "$fname.in" if -x $fname;
  }
