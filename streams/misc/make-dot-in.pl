#!/usr/uns/bin/perl -w
#
# make-dot-in.pl: convert file to file.in magically
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: make-dot-in.pl,v 1.3 2003-09-25 19:59:43 dmaze Exp $
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

use strict;

foreach my $fname (@ARGV)
  {
    open READER, "<$fname";
    open WRITER, ">$fname.in";

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
	  }
	elsif ($replacing)
	  {
	    if ($_ eq "# END AUTOCONFISCATE\n")
	      {
		$replacing = 0;
		$pattern = undef;
	      }
	    else
	      {
		# Look to see if the string matches.  (It better.)
		chomp;
		my @parts = /^$pattern$/;
		if (!@parts)
		  {
		    warn "$fname:$line: string doesn't match pattern\n";
		    print WRITER "$_\n";
		    next LINE;
		  }
		# Now match up fields and parts.
		my $var = undef;
		my $caps = 0; # -1 if lowercase, 1 if uppercase, 0 if exact
	      FIELD:
		for (my $i = 0; $i <= $#fields; $i++)
		  {
		    my $field = $fields[$i];
		    my $part = $parts[$i];
		    # This isn't an interesting part (it can't be the variable
		    # name) if it doesn't start with a letter.
		    next FIELD if $part !~ /^[[:alpha:]]/;
		    if ($field =~ /@?(var)@?/i)
		      {
			# What case are we looking for?
			my $case = 0;
			$case = -1 if ($field =~ /var/);
			$case = 1 if ($field =~ /VAR/);
			# Figure out the cases where we don't have a match:
			next FIELD if $case == -1 && $part ne lc($part);
			next FIELD if $case == 1 && $part ne uc($part);
			# Or, $field is mixed-case.  Now, check to see
			# if we have a name conflict:
			if ($var)
			  {
			    my $conflict = 0;
			    $conflict = 1 if $caps == -1 && $var ne lc($part);
			    $conflict = 1 if $caps == 1 && $var ne uc($part);
			    $conflict = 1 if $case == -1 && $part ne lc($var);
			    $conflict = 1 if $case == 1 && $part ne uc($var);
			    $conflict = 1 if $case == 0 && $caps == 0 &&
			      $part ne $var;
			    if ($conflict)
			      {
				warn "$fname:$line: var could be $var or $part\n";
				print WRITER "$_\n";
				next LINE;
			      }
			    else
			      {
				# Save the variable only if we have
				# an exact case.
				if ($case == 0)
				  {
				    $var = $part;
				    $caps = 0;
				  }
			      }
			  }
			else
			  {
			    # No old name.
			    $var = $part;
			    $caps = $case;
			  }
		      }
		  }
		# Okay, have a best-guess $var, right?
		if (!$var)
		  {
		    warn "$fname:$line: couldn't determine var\n";
		    print WRITER "$_\n";
		    next LINE;
		  }
		my $lcvar = lc($var);
		my $ucvar = uc($var);
		# Replace parts with the right variable.
		for (my $i = 0; $i <= $#fields; $i++)
		  {
		    my $field = $fields[$i];
		    if ($field =~ /@?(var)@?/i)
		      {
			$field =~ s/var/$lcvar/ or
			$field =~ s/VAR/$ucvar/ or
			$field =~ s/var/$var/i;
			$parts[$i] = $field;
		      }
		  }
		$_ = join('', @parts);
		print WRITER "$_\n";
	      }
	  }
	elsif (/^\# AUTOCONFISCATE (.*)/)
	  {
	    $replacing = 1;
	    # Parse $pattern:
	    @fields = split(/(@?var@?)/i, $1);
	    $pattern = "";
	    foreach my $f (@fields)
	      {
		my $g;
		if ($f =~ /^@?var@?$/i)
		  {
		    $g = ".+";
		  }
		else
		  {
		    $g = $f;
		    $g =~ s/(\W)/\\$1/g;
		  }
		$pattern .= "($g)";
	      }
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
