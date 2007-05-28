#include <stdio.h>

int main() {
  FILE * o = fopen("Simple.in", "w");
  int i;
  for (i=0; i<10000; i++) {
    fwrite(&i, sizeof(i), 1, o);
  }
  fclose(o);
}
