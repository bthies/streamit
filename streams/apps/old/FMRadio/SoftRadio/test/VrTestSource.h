#ifndef _VRTESTSOURCE_H_
#define _VRTESTSOURCE_H_

#include <VrSource.h>

class VrTestSource : public VrSource<float> {
protected:
  int c;
public:
  virtual void work(int i) {
    for (int n = 0; n < i; n ++) {
      outputWrite((float)c);
      c++;
      if (c >= 1000)
	c = 0;
    }
  }
  virtual void initialize() {
    setOutputSize (getFirstConnector()->getHistory());
  }
};
#endif
