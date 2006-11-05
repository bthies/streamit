#!/usr/bin/perl

$infilename  = "table2.txt";
$outfilename = "out2.txt";
open(INF, $infilename) || die("\nCan't open table2.txt for reading: $!\n");
open(OUTF, ">$outfilename") || die("\nCan't open code2.txt for writing: $!\n");

@input = <INF>;


@inputa = @input;
while(@inputa>0)
{
    $temp =pop(@inputa);
    $temp1=$temp;
    $temp2=$temp;

    $temp1 =~ s/([0-3])[^0-9]+[0-9]+[^0-9]+[01]+[^01]*/\1/;
    $temp2 =~ s/[0-3][^0-9]+([0-9]+)[^0-9]+[01]+[^01]*/\1/;
    $temp  =~ s/[0-3][^0-9]+[0-9]+[^0-9]+([01]+)[^01]*/\1/;
    
    $leng = length($temp);

    $output = "else if(buffertemp[";
    $index = 7;	
    
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
    $output .= "return tuple3(" . $temp1 . "," . $temp2 . "," . $leng . ");\n";

    push(@cleana, $output);
}
while(@cleana>0)
{
    $temp=pop(@cleana);
    print OUTF "$temp";
}
print OUTF "\n\n\n\n\n\n\n";



close(OUTF);
close(INF);


