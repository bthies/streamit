#! /usr/uns/bin/perl

# parameters .str file, numiters

use strict;
use warnings;

my $tolerance = .01;
my $strFile = shift;
my $numIters = shift;

#find the file writer add statement
# add FileWriter<type>("filename");
local @ARGV = ($strFile);

my $addFileStmt;

foreach (<>) {
  if (/add FileWriter/) {
    !(defined($addFileStmt)) or die("Found Two add FileWriter statements");
    $addFileStmt = $_;
  } 
}

die("Found No add FileWriter statement.") unless defined($addFileStmt);

#get the type and the file name
my ($type) = ($addFileStmt =~ /.+\<(.+)\>.+/);
my ($outFile) = ($addFileStmt =~ /.+\"(.+)\".+/);

#make sure that the file exists and the results.out file exists
die ("output file does not exist") unless ( -s $outFile);
die ("results.out file does not exist") unless ( -s "results.out");

#make sure the type is either int or float
die("Unrecognized Type") unless ($type =~ /^float$/ || $type =~/^int$/);
#mv the outfile to $outFile.raw
system("mv", "$outFile", "$outFile.raw");
#run the library
system("strc", "-library", "-i", "$numIters", "$strFile");

#compare $outfile to $outfile.raw based on type
#exit with sucess for successful compare or die
if (compareFiles($outFile, "$outFile.raw", $type) == 1) {
  print "Files Sucessfully Compare!";
  open(RES, ">>results.out");
  #record in the results.out that we have passed
  print RES "PASSED\n";
  close RES;
}
else {
  print "Files Fail Compare!";
}



#take the two file names and the type, and compare them
sub compareFiles {
  my ($file1, $file2, $type) = ($_[0], $_[1], $_[2]);
  my $temp;
  my $bin1;
  my $bin2;
  my $num1;
  my $num2;
  #make $file1 always smaller
  if ( -s $file1 > -s $file2) {
    $temp = $file1;
    $file1 = $file2;
    $file2 = $temp;
  }
  #open the files
  open(FILE1, $file1) or die("Cannot open $file1");
  open(FILE2, $file2) or die("Cannot open $file2");
  binmode(FILE1);
  binmode(FILE2);
  while (read(FILE1, $bin1, 4) != 0) {
    #read 4 bytes from file2
    read(FILE2, $bin2, 4);
    if ($type =~ /^int$/) {
      $num1 = unpack("i", $bin1);
      $num2 = unpack("i", $bin2);
      if (intCheck($num1, $num2) == 0) {
	close(FILE1);
	close(FILE2);
	return 0;
      }
    }
    else {
      $num1 = unpack("f", $bin1);
      $num2 = unpack("f", $bin2);
      print $num1, " ", $num2, "\n";
      if (floatCheck($num1, $num2) == 0) {
	close(FILE1);
	close(FILE2);
	return 0;
      }
    }
  }
  #everything passed!
  close FILE1;
  close FILE2;
  return 1;
}

  
#return 1 if the two int arguments are within
#$tolerance of one another, 0 if not
sub intCheck {
  my($n1, $n2) = ($_[0], $_[1]);
  my ($ratio, $t1);
  #make num1 the larger number
  if ($n1 < $n2) {
    $t1 = $n1;
    $n1 = $n2;
    $n2 = $t1;
  }
  if ($n2 == 0) {
    if ($n1 == 0) {
      return 1;
    }
    else {
      return 0;
    }
  }
  #num2 != 0
  $ratio = ($n1 / $n2) - 1;
  if ($ratio <= $tolerance) {
    return 1;
  }
  else {
    return 0;
  }
}

#return 1 if the two int arguments are within
#$tolerance of one another, 0 if not
sub floatCheck {
  my($n1, $n2) = ($_[0], $_[1]);
  my $ratio;
  my $t;
  #make num1 the larger number
  if ($n1 < $n2) {
    $t = $n1;
    $n1 = $n2;
    $n2 = $t;
  }
  if ($n2 == 0.0) {
    if ($n1 == 0.0) {
      return 1;
    }
    else {
      return 0;
    }
  }
  #num2 != 0
  $ratio = ($n1 / $n2) - 1;
  if ($ratio <= $tolerance) {
    return 1;
  }
  else {
    return 0;
  }
}
