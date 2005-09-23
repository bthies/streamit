#!/usr/bin/perl -w

$numArgs = $#ARGV + 1;
if ($numArgs == 1) {
    $precompile_file = $ARGV[0];
    open(PRECOMPILE, $precompile_file) || die("Could not open $precompile_file for input!");
    open(POSTCOMPILE, ">&STDOUT") || die("Could not output to STDOUT for output!");;
    main();
} elsif ($numArgs == 3 && ($ARGV[1] eq "-o")) {
    $precompile_file = $ARGV[0];
    $postcompile_file = $ARGV[2];
    open(PRECOMPILE, $precompile_file) || die("Could not open $precompile_file for input!");
    open(POSTCOMPILE, ">$postcompile_file") || die("Could not open $postcompile_file for output!");    
    main();
} else {
    help();
    exit(-1);
}

sub process_rule {
    my $rule = shift; 
    my @$file_contents = shift;
    my @output = ();

    foreach $line (@file_contents) {
        $newline = "";
        while ($newline ne $line) {
            $newline = $rule->($line);
        }
        push(@output, $newline); 
    }

    return @output; 
}

sub bin2dec {
    return unpack("N", pack("B32", substr("0" x 32 . shift, -32)));
}

sub rule_convertbinary {
    $line = $_[0];
    chomp($line);
    while ($line =~ /0b/) {
        my($before, $matching, $after) = $line =~ /(.*)(0b[0|1]*)(.*)/;
        $matching = substr($matching, 2);
        $matching = bin2dec($matching);
        $line = $before . $matching . $after;
    }
    return $line
}

sub rule_nextstartcode {
    $_[0] =~ s/( *)next_start_code\(\);/$1\{\n$1  int nsc_tempval;\n$1  nsc_tempval = 0;\n$1  for (int nsc_i = 0; nsc_i < 24; nsc_i++) \{\n$1    nsc_tempval += peek(nsc_i);\n$1    nsc_tempval <<= 1;\n$1  \}\n$1  nsc_tempval >>=1;\n$1  while (nsc_tempval != 1) \{\n$1    pops(1, nsc_tempval);\n$1    \/\/ print(\"....looking for next_start_code....\");\n$1    nsc_tempval = 0;\n$1    for (int nsc_i = 0; nsc_i < 24; nsc_i++) \{\n$1      nsc_tempval += peek(nsc_i);\n$1      nsc_tempval <<= 1;\n$1    \}\n$1    nsc_tempval >>=1;\n$1  \}\n$1\}/;
    return $_[0];
}

sub rule_markerbit {
    $_[0] =~ s/( *)marker_bit\(\);/$1\{\n$1  int marker_bit;\n$1  pops(1, marker_bit);\n$1  if (marker_bit != 1)\n$1  print(\"Error - Expected Marker Bit To Be Set\");\n$1\}/;
    return $_[0];
}

sub rule_addmarkerbit {
    $_[0] =~ s/( *)add_marker_bit\(\);/$1\{\n$1  int marker_bit = 1;\n$1  pushs(1, marker_bit);\n$1\}/;
    return $_[0];
}

sub rule_vlc {
    $_[0] =~ s/( *)variable_length_code\((\w+(\[\w+\])*), *(\w+(\[\w+\])*)\);/$1\{\n$1  boolean found = false;\n$1  int guesslength = 1;\n$1  int tablepos = 0;\n$1  while (!found) \{\n$1    peeks(guesslength, $2);\n$1    tablepos = 0;\n$1    while (!found && tablepos < $4_len) \{\n$1      if ($2 == $4\[tablepos\].code && guesslength == $4\[tablepos\].len) \{\n$1        found = true;\n$1        pops(guesslength, $2);\n$1      \} else \{\n$1      tablepos++;\n$1    \}\n$1    \}\n$1    guesslength++;\n$1  \}\n$1  $2 = $4\[tablepos\].value;\n$1\}/;
    return $_[0];
}

sub rule_vlec {
    $_[0] =~ s/( *)variable_length_encode\((\w+(\[\w+\])*), *(\w+(\[\w+\])*)\);/$1\{\n$1  boolean found = false;\n$1  int index = -1;\n$1  while (!found) {\n$1    index++;\n$1    if ($4\[index\].value == $2)\n$1      found = true;\n$1  }\n$1  int templen = $4\[index\].len;\n$1  int tempcode = $4\[index\].code;\n$1  pushs(templen, tempcode);\n$1}/;
    return $_[0];
}

sub rule_vlc_dct {
    $_[0] =~ s/( *)variable_length_code_dct\((\w+(\[\w+\])*), *(\w+(\[\w+\])*), *(\w+(\[\w+\])*)\);/$1\{\n$1  boolean found = false;\n$1  int guesslength = 1;\n$1  int tablepos = 0;\n$1  while (!found) \{\n$1    peeks(guesslength, $2);\n$1    tablepos = 0;\n$1    while (!found && tablepos < $6_len) \{\n$1      if ($2 == $6\[tablepos\].code && guesslength == $6\[tablepos\].len) \{\n$1        found = true;\n$1        pops(guesslength, $2);\n$1      \} else \{\n$1        tablepos++;\n$1      \}\n$1    \}\n$1    guesslength++;\n$1  \}\n$1  $2 = $6\[tablepos\].run;\n$1  $4 = $6\[tablepos\].level;\n$1\}/;
    return $_[0];
}

sub rule_pops {
    $_[0] =~ s/( *)pops\((\w+(\[\w+\])*), *(\w+(\[\w+\])*)\);/$1$4 = 0;\n$1for (int pops_i = 0; pops_i < ($2-1); pops_i++) \{\n$1  $4 += pop();\n$1  $4 <<= 1;\n$1\}\n$1$4 += pop();/;
    return $_[0];
}

sub rule_peeks {
    $_[0] =~ s/( *)peeks\((\w+(\[\w+\])*), *(\w+(\[\w+\])*)\);/$1$4 = 0;\n$1for (int peeks_i = 0; peeks_i < ($2-1); peeks_i++) \{\n$1  $4 += peek(peeks_i);\n$1  $4 <<= 1;\n$1\}\n$1$4 += peek($2-1);/;
    return $_[0];
}
                                        
sub rule_shortpushs {
    $_[0] =~ s/( *)shortpushs\((\w+(\[\w+\])*)\);/$1\{\n$1  int intpushs_int = $2;\n$1  int intpushs_b0 = ((intpushs_int >> 8) & 0x000000FF);\n$1  int intpushs_b1 = (intpushs_int & 0x000000FF) << 8;\n$1  intpushs_int = (intpushs_b0 | intpushs_b1);\n$1  pushs(16, intpushs_int);\n$1\}/;
    return $_[0];
}

sub rule_shortpops {
    $_[0] =~ s/( *)shortpops\((\w+(\[\w+\])*)\);/$1\{\n$1  int intpops_int;\n$1  pops(16, intpops_int);\n$1  int intpops_b0 = ((intpops_int >> 8) & 0x000000FF);\n$1  int intpops_b1 = (intpops_int & 0x000000FF) << 8;\n$1  $2 = (intpops_b0 | intpops_b1);\n$1\}/;
    return $_[0];
}

sub rule_intpushs {
    $_[0] =~ s/( *)intpushs\((\w+(\[\w+\])*)\);/$1\{\n$1  int intpushs_int = $2;\n$1  int intpushs_b0 = ((intpushs_int >> 24) & 0x000000FF);\n$1  int intpushs_b1 = (intpushs_int & 0x00FF0000) >> 8;\n$1  int intpushs_b2 = (intpushs_int & 0x0000FF00) << 8;\n$1  int intpushs_b3 = (intpushs_int & 0x000000FF) << 24;\n$1  intpushs_int = (intpushs_b0 | intpushs_b1 | intpushs_b2 | intpushs_b3);\n$1  pushs(32, intpushs_int);\n$1\}/;
    return $_[0];
}

sub rule_intpops {
    $_[0] =~ s/( *)intpops\((\w+(\[\w+\])*)\);/$1\{\n$1  int intpops_int;\n$1  pops(32, intpops_int);\n$1  int intpops_b0 = ((intpops_int >> 24) & 0x000000FF);\n$1  int intpops_b1 = (intpops_int & 0x00FF0000) >> 8;\n$1  int intpops_b2 = (intpops_int & 0x0000FF00) << 8;\n$1  int intpops_b3 = (intpops_int & 0x000000FF) << 24;\n$1  $2 = (intpops_b0 | intpops_b1 | intpops_b2 | intpops_b3);\n$1\n$1}/;
    return $_[0];
}

sub rule_pushs {
    $_[0] =~ s/( *)pushs\((\w+(\[\w+\])*), *(\w+(\[\w+\])*)\);/$1\{\n$1  int pushs_int = $4 << (32-$2);\n$1  for (int pushs_i = 0; pushs_i < $2; pushs_i++) \{\n$1    if (pushs_int >= 0) \{\n$1      push(0);\n$1    \} else \{\n$1      push(1);\n$1    \}\n$1    pushs_int <<= 1;\n$1  \}\n$1\}/;
    return $_[0];
}

sub rule_include {
    if ($_[0] =~ m/\#include \"(\w+).str\"/) {
        open(ADDLIBRARY, "./$1.str") || die("Could not open ./$1.str for input!");
        @library_contents = <ADDLIBRARY>;
        $_[0] = "";
        foreach $line (@library_contents) {
            $_[0] .= $line;
        }
    }
    return $_[0];
}

sub main {

    @file_contents = <PRECOMPILE>;

    @intermediate = ();
    
    @intermediate = process_rule(\&rule_convertbinary, @intermediate);
    
    @intermediate = process_rule(\&rule_intpushs, @intermediate);
    @intermediate = process_rule(\&rule_shortpushs, @intermediate);

    @intermediate = process_rule(\&rule_intpops, @intermediate);
    @intermediate = process_rule(\&rule_shortpops, @intermediate);
    
    @intermediate = process_rule(\&rule_vlc, @intermediate);
    @intermediate = process_rule(\&rule_vlec, @intermediate);
    @intermediate = process_rule(\&rule_vlc_dct, @intermediate);
    
    @intermediate = process_rule(\&rule_peeks, @intermediate);
    
    @intermediate = process_rule(\&rule_addmarkerbit, @intermediate);
    @intermediate = process_rule(\&rule_markerbit, @intermediate);
    @intermediate = process_rule(\&rule_nextstartcode, @intermediate);
    @intermediate = process_rule(\&rule_pops, @intermediate);
    @intermediate = process_rule(\&rule_pushs, @intermediate);

    @intermediate = process_rule(\&rule_include, @intermediate);
    
    foreach $line (@intermediate) {
        print POSTCOMPILE "$line\n";
    }
    close(POSTCOMPILE);

}

sub help {
    print("Usage: preprocess.perl [INPUT_FILE] [OPTION]\n");
    print("Converts psuedo StreamIt source code into StreamIt source code.\n");
    print("Default output is STDOUT.\n\n");
    print("  -o [OUTPUTFILE]     Redirect output to a file\n");
    print("\nReport bugs to Matthew Drake <madrake\@gmail.com>\n");
}






