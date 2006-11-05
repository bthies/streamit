#!/usr/bin/perl

$| = 1;

$infilename  = "table3.txt";
$outfilename = "out3.txt";
open(INF, $infilename) || die("\nCan't open table3.txt for reading: $!\n");
open(OUTF, ">$outfilename") || die("\nCan't open code3.txt for writing: $!\n");


@input = <INF>;
@inputa = @input;

$loopnum = 16;

while($loopnum > 1)
{
    $temploop=0;

    print "-";
    while($temploop<$loopnum)
    {
	$temp =$inputa[$temploop];
	chomp $temp;
	$temp1=$temp;
	$temp2=$temp;
	
	$temp1 =~ s/([0-9]+)[^0-9]+[01]+[^01]+.*/\1/;
	$temp  =~ s/[0-9]+[^0-9]+([01]+)[^01]+.*/\1/;
	$temp2 =~ s/([0-9]+[^0-9]+)[01]+[^01]+(.*)/\1\2/;
	$inputa[$temploop] = $temp2;

	if($loopnum==16)
	{
	    $index = 9;
	    $output = "else if(buffertemp2[";
	}
	else
	{
	    $index = 5;
	    $output = "else if(buffertemp[";
	}
	
	$leng = length($temp);
    
	$output .= $index . ":" . ($index+1-$leng). "]";
	if($index+1-$leng < 10) 
	{
	    $output .= " ";
	}
	$output .= " == ";
	if($leng < 10)
	{ 
	    $output .= " ";
	}
	$output .= $leng . "\'b" . $temp . ") ";
	$output .= "return tuple2(" . $temp1 . "," . $leng . ");\n";

	push(@cleana, $output);
       	
	$temploop++;

	print "+";
    }

    while(@cleana>0)
    {
	push(@cleanb,(pop(@cleana)));
    }
    while(@cleanb>0)
    {
	$temp=pop(@cleanb);
	print OUTF "$temp";
    }
    print OUTF "\n\n\n\n\n\n\n";
    $loopnum--;

    print ".";
}

close(OUTF);
close(INF);


