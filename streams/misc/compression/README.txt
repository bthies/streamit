Here is the current matrix of supported file converters:

       TO
FROM   avi  aa   raw  pam
avi    ---   X         X
aa          ---   X   (X)
raw              ---   X
pam     X   (X)   X   ---

Entries marked with an (X) go through an intermediate format instead
of being converted directly.

Note that the source AVI's can use any compression technique, though
the destination AVI is hard-coded as Apple Animation (could be
adjusted in script).
