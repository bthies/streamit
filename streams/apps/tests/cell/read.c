#include <stdio.h>

int main() {
  FILE * in = fopen("Simple.in", "r");
  int i, t;
  for (i=0; i<100; i++) {
    fread(&t, sizeof(int), 1, in);
    printf("%d\n", t);
  }
}
