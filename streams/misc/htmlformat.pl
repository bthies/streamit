#!/usr/uns/bin/perl -w
#
# We may not have HTML::TreeBuilder in the correct place.
# you may need to:
# perl -MCPAN -e shell
# cpan> install HTML::TreeBuilder
# cpan> install HTML::Element
# cpan> install HTML::FormatText
#
use strict;
require HTML::TreeBuilder;
require HTML::FormatText;

my $tree = HTML::TreeBuilder->new->parse_file($ARGV[0]);
my $formatter = HTML::FormatText->new(leftmargin=>0);
print $formatter->format($tree);
