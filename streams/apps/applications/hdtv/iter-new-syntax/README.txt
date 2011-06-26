This is a partial translation from the old Java syntax.  The current
translation provides an accurate representation of the stream graph,
the I/O rates, schedule, and state.  It does not provide an accurate
representation of the work estimate or the computation itself.

It should be possible to finish the translation, though will have to
have some good array support (e.g., returning arrays from helper
functions) for it to work under strc.

Unfinished files:
ReedSolomonEncoder.str
ReedSolomonDecoder.str
UngerboeckDecoder.str
