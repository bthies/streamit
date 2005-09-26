#!/usr/local/bin/perl -w
#
# streamit-mail-unowned.pl: report on RT tickets that aren't owned by anyone
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: streamit-mail-unowned.pl,v 1.1 2005-09-26 22:01:31 rabbah Exp $
#

use lib "/usr/local/lib";
use RT::Interface::CLI qw(CleanEnv);
use RT::Tickets;

CleanEnv();
RT::LoadConfig();
RT::Init();

my @queues = ('StreamIt Bugs', 'StreamIt Features');
my $Tickets = RT::Tickets->new(RT::Nobody);
my $message = '';
$message .= "From: rabbah\@mit.edu\n";
$message .= "To: commit-stream\@cag.lcs.mit.edu\n";
$message .= "Subject: StreamIt Unassigned Bug List\n";
$message .= "\n";
$message .= "StreamIt Unassigned Bug List\n";
$message .= "============================\n";
$message .= "\n";

for my $qname (@queues)
  {
    $message .= "$qname:\n";

    $Tickets->ClearRestrictions;
    $Tickets->LimitStatus(VALUE => "Open");
    $Tickets->LimitStatus(VALUE => "New");
    $Tickets->LimitOwner(VALUE => RT::Nobody->UserObj->Id);
    $Tickets->LimitQueue(VALUE => $qname, OPERATOR => '=');

    $^A = '';
    while (my $Ticket = $Tickets->Next)
      {
	my ($id, $subject) = ($Ticket->Id, $Ticket->Subject);
	formline <<'END', $id, $subject;
  @>> ^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
END
        formline <<'END', $subject while $subject;
      ^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
END
      }
    $message .= $^A ? "$^A\n" : "No outstanding bugs.\n\n";
  }

open MAIL, "| /usr/lib/sendmail commit-stream\@cag.lcs.mit.edu";
print MAIL $message;
close MAIL;
