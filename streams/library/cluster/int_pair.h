
#ifndef __INT_PAIR_H
#define __INT_PAIR_H


class int_pair {

 public:
  int from, to;

  int_pair(int from, int to);
  int_pair &operator=(const int_pair &rhs);
  int operator==(const int_pair &rhs) const;
  int operator<(const int_pair &rhs) const;
};


#endif
