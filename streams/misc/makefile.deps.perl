#! /usr/uns/bin/perl

use strict;
use warnings;
use Cwd;
use File::Find;

my $root_dir = cwd . "/";
my $root_dir_len = length($root_dir);

# explicitly exported from Makefile
my @package_dirs = split(/\s+/, $ENV{"PACKAGE_DIRS"});

open(DEPS, "> " . $ENV{"MAKEFILE_DEPEND"});

print DEPS '%.class : %.java' . "\n";
print DEPS "	" . $ENV{"JAVAC"}. " " . $ENV{"JAVAC_FLAGS"} . " \$<\n\n";

my %class_hash;

find(\&find_sub_a,cwd);

#print DEPS "$root_dir\n"; 	# debug
print DEPS "alldeps:";
foreach (sort keys %class_hash ) {
    print DEPS " $_";
}
print DEPS "\n\n";

find(\&find_sub_b,cwd);

close(DEPS);

sub find_sub_a {
    return unless /\.u$/;
    my $abs_dir = $File::Find::dir;
    my $rel_dir = substr($abs_dir,$root_dir_len);
    my $classname = $_;
    $classname =~ s/\.u$/\.class/;
    return if $classname =~ /-[0-9]+\.class$/;
    my $qualified_classname = "$rel_dir/$classname";
    $class_hash{$qualified_classname} = 1;
}

sub find_sub_b {
    return unless /\.u$/;
    local @ARGV;
    @ARGV = $_;
    foreach (<>) {
	next if /\.class$/;
	next if /\$/;
	next if /-[0-9]+\.java$/;
	print DEPS "$_\n\n";
    }
}
    
