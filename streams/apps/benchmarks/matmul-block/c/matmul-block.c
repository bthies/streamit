/*
 * matmul-block.c: block matrix multiply
 * $Id: matmul-block.c,v 1.1 2002-11-07 22:19:09 dmaze Exp $
 */

#ifdef raw
#include <raw.h>
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#endif

void begin(void);

float *block_multiply(float *a, int x0, int y0,
                      float *b, int x1, int y1,
                      int blockDiv);
float *get_sub(float *a, int x0, int y0, int blockDiv, int i, int j);
float *transpose(float *a, int x0, int y0);
float get_float(void);
float *make_matrix(int x, int y);
void mul_subs(float *a, int x0, int y0, float *b, int x1, int y1,
              float *result);
float *combine_subs(float **subs, int x0, int y0, int blockDiv);

static int numiters = -1;

#ifndef raw
int main(int argc, char **argv)
{
  int option;

  while ((option = getopt(argc, argv, "i:")) != -1)
  {
    switch(option)
    {
    case 'i':
      numiters = atoi(optarg);
    }
  }

  begin();
  return 0;
}
#endif

void begin(void)
{
  int i;
  
  const int x0 = 12;
  const int y0 = 12;
  const int x1 = 9;
  const int y1 = 12;
  const int blockDiv = 3;

  /* No initialization since filters are stateless and don't peek.  Yay! */
  while (numiters == -1 || numiters-- > 0)
  {
    float *a = make_matrix(x0, y0);
    float *b = make_matrix(x1, y1);
    float *c = block_multiply(a, x0, y0, b, x1, y1, blockDiv);
    free(a);
    free(b);
    for (i = 0; i < x1 * y0; i++)
    {
#ifdef raw
      print_float(c[i]);
#else
      printf("%f\n", c[i]);
#endif
    }
    free(c);
  }
}

float *block_multiply(float *a, int x0, int y0,
                      float *b, int x1, int y1,
                      int blockDiv)
{
  float **a_subs;
  float *b_transpose;
  float **b_subs;
  float **product_subs;
  float *result;
  int i, j, k;
  int x0s = x0 / blockDiv;
  int y0s = y0 / blockDiv;
  int x1s = x1 / blockDiv;
  int y1s = y1 / blockDiv;
  
  /* Create sub-matrices of A: */
  a_subs = malloc(blockDiv * blockDiv * sizeof(float *));
  for (j = 0; j < blockDiv; j++)
    for (i = 0; i < blockDiv; i++)
      a_subs[i * blockDiv + j] = get_sub(a, x0, y0, blockDiv, i, j);

  /* Also create sub-matrices of B': */
  /* (This could become faster if we ditched the transpose.) */
  b_transpose = transpose(b, x1, y1);
  b_subs = malloc(blockDiv * blockDiv * sizeof(float *));
  for (j = 0; j < blockDiv; j++)
    for (i = 0; i < blockDiv; i++)
      b_subs[i * blockDiv + j] = get_sub(b_transpose, y1, x1, blockDiv, i, j);

  /* That was fun.  Now multiply these blocks.  Note the ordering:
   * A00 A01 A02 ... A00 A01 A02 ... A10 ...
   * B00 B01 B02 ... B90 B91 B92 ... B00 ...
   */
  product_subs = malloc(blockDiv * blockDiv * sizeof(float *));
  for (i = 0; i < blockDiv * blockDiv; i++)
  {
    product_subs[i] = malloc(x1s * y0s * sizeof(float));
    for (j = 0; j < x1s * y0s; j++)
      product_subs[i][j] = 0;
  }
  
  for (k = 0; k < blockDiv; k++)
    for (j = 0; j < blockDiv; j++)
      for (i = 0; i < blockDiv; i++)
      {
        int idxa = k * blockDiv + i;
        int idxb = j * blockDiv + i;
        mul_subs(a_subs[idxa], x0s, y0s,
                 b_subs[idxb], x1s, y1s,
                 product_subs[idxb]);
      }

  /* ...and recombine the products. */
  result = combine_subs(product_subs, x1, y0, blockDiv);

  /* Clean up: */
  for (i = 0; i < blockDiv * blockDiv; i++)
  {
    free(a_subs[i]);
    free(b_subs[i]);
    free(product_subs[i]);
  }
  free(a_subs);
  free(b_subs);
  free(product_subs);
  return result;
}

float *get_sub(float *a, int x0, int y0, int blockDiv, int i, int j)
{
  int xs = x0 / blockDiv;
  int ys = y0 / blockDiv;
  int xbase = i * xs;
  int ybase = j * ys;
  int x, y;
  float *result = malloc(xs * ys * sizeof(float));
  for (y = 0; y < ys; y++)
    for (x = 0; x < xs; x++)
      result[y * xs + x] = a[(y + ybase) * x0 + (x + xbase)];
  return result;
}

float *transpose(float *a, int x0, int y0)
{
  float *at = malloc(x0 * y0 * sizeof(float));
  int x, y;
  for (y = 0; y < y0; y++)
    for (x = 0; x < x0; x++)
      at[x * y0 + y] = a[y * x0 + x];
  return at;
}

/* RETURNS: a sequential floating-point number. */
float get_float(void)
{
  static float num;
  float val = num;
  num++;
  if (num >= 4.0) num = 0.0;
  return val;
}

/* RETURNS: a malloc()ed array of x*y floats in row-major order. */
float *make_matrix(int x, int y)
{
  int i;
  float *matrix = malloc(x * y * sizeof(float));
  for (i = 0; i < x * y; i++)
    matrix[i] = get_float();
  return matrix;
}

void mul_subs(float *a, int x0, int y0, float *b, int x1, int y1,
              float *result)
{
  /* The result is a x1 by y0 subblock. */
  int x, y, z;
  
  for (y = 0; y < y0; y++)
    for (x = 0; x < x1; x++)
    {
      float sum = 0;
      for (z = 0; z < x0; z++)
        sum += a[z + y * x0] * b[z + x * y1];
      result[y * x1 + x] += sum;
    }
}

float *combine_subs(float **subs, int x0, int y0, int blockDiv)
{
  int x0s = x0 / blockDiv;
  int y0s = y0 / blockDiv;
  float *result = malloc(x0 * y0 * sizeof(float));
  int i, j, x, y;
  
  /* Guessing at the ordering: */
  for (j = 0; j < blockDiv; j++)
    for (i = 0; i < blockDiv; i++)
      for (y = 0; y < y0s; y++)
        for (x = 0; x < x0s; x++)
          result[(j * y0s + y) * x0 + (i * x0s + x)] =
            subs[j * blockDiv + i][y * x0s + x];

  return result;
}

