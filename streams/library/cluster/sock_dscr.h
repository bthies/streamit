
#ifndef __SOCK_DSCR_H
#define __SOCK_DSCR_H

class sock_dscr {

 public:
  int from, to, type;

  sock_dscr(int from, int to, int type);
  sock_dscr &operator=(const sock_dscr &rhs);
  int operator==(const sock_dscr &rhs) const;
  int operator<(const sock_dscr &rhs) const;
};


#endif
