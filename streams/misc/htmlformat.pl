#!/usr/uns/bin/perl -w
use strict;
require HTML::TreeBuilder;
require HTML::FormatText;

my $tree = HTML::TreeBuilder->new->parse_file($ARGV[0]);
my $formatter = HTML::FormatText->new(leftmargin=>0);
print $formatter->format($tree);
