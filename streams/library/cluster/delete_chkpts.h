
#ifndef __DELET_CHKPTS_H
#define __DELET_CHKPTS_H

#include <service.h>

class delete_chkpts : public service {

  static int max_iter;
  static int iter_done;

  virtual void run();

 public:

  static void set_max_iter(int iter); 
};


#endif
