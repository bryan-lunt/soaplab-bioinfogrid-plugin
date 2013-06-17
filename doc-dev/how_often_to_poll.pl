#!/usr/bin/perl -w
use strict;

my $poll_count = 0;
my $poll_change_count = 0;
my $poll_interval     = ($ARGV[0] || 10);    # 5 seconds
my $max_time          = ($ARGV[1] || 3600);  # 1 hour
my $max_poll_interval = ($ARGV[2] || 300);   # 5 minutes

print "Running time:                  $max_time (secs)\n";
print "Maximal polling interval:      $max_poll_interval (secs)\n";
print "Polling interval at the start: $poll_interval (secs)\n";

my $spent_time = 0;
LOOP: while (1) {
    foreach (1..10) {
	$poll_count++;
	$spent_time += $poll_interval;
	last LOOP if $spent_time > $max_time;
    }
    unless ($poll_interval > $max_poll_interval) {
	$poll_interval *= 2;
	$poll_change_count++;
    }
}

print "Polling was executed:          $poll_count (times)\n";
print "Polling interval changed:      $poll_change_count (times)\n";
print "Polling interval at the end:   $poll_interval (secs)\n";
