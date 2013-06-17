#!/usr/bin/perl -w
#
# Testing script.
#
# It may have various arguments - but these are important because
# they dela with input and output data:
#
# -i <input-file> -o <output-file>
#
# $Id: grid-testing-01.pl,v 1.2 2007/03/30 09:11:59 marsenger Exp $
# ---------------------------------

my ($input, $output);

# --- print arguments on STDOUT
print "Arguments of the grid-testing-01:\n";
while (@ARGV) {
    if ($ARGV[0] eq '-i') {
	shift;
	$input = $ARGV[0];
    } elsif ($ARGV[0] eq '-o') {
	shift;
	$output = $ARGV[0];
    } else {
	print "\targ: $ARGV[0]\n";
    }
    shift;
}
print "\tinput:  $input\n" if defined $input;
print "\toutput: $output\n" if defined $output;
print "--- end of command-line arguments ---\n";

# --- print environments
print "Some environment of the grid-testing-01\n";
foreach (sort keys %ENV) {
    print "$_=" . $ENV{$_} . "\n" if /^ENVVAR/;
}
#print "$_=" . $ENV{$_} . "\n" foreach (sort keys %ENV);
print "--- end of environment ---\n";

# --- copy input file to output file
if ($input) {
    open (INP, '<', $input)
	or die "Cannot open '$input' for reading: $!\n";
    if ($output) {
	open (OUT, '>', $output)
	    or die "Cannot open '$output' for writing: $!\n";
	print OUT while (<INP>);
	close OUT;
    } else {
	print while (<INP>);
    }
}
