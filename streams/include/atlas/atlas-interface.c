#include "cblas.h"

/**
 * Computes y = Ax + b, where:
 *  - A is an MxN matrix in row-major form
 *  - x is a N-element column vector
 *  - b is a M-element column vector
 *
 * For example, consider:
 *
 *    A        x     b   =  y
 *  [1 2 3] * [1] + [7]  = [17]    M=2, N=3
 *  [4 5 6]   [2]   [8]    [40]
 *            [3]   
 *
 * You should pass this in as:
 *
 * float* A = {1.0, 2.0, 3.0,
 *             4.0, 5.0, 6.0};
 *
 * float* x = {1.0, 2.0, 3.0};
 *
 * float* b = {7.0, 8.0, 9.0};
 *
 * and you will get back in the contents of y:
 *
 * float* y = {17.0,
 *             40.0};
 *
 * So you should have allocated N*sizeof(float) for y before entry.
 */
void atlasMatrixVectorProduct(float* A, float* x, float* b, int M, int N, float* y) {
  // the leading dimension of A is N since it's row-major
  int LDA = N;
  // don't do any transformations on matrices
  int OP_A = CblasNoTrans;
  int OP_B = CblasNoTrans;
  // don't scale the parameters
  float ALPHA = 1.0;
  // multiply the addition by zero, since we don't want to clobber values of <b>
  float BETA = 0.0;
  // amount by which elements of x and y are separated
  int incB = 1;
  int incX = 1;
  int incY = 1;
  // first do y = 1.0*Ax + 0.0*y
  cblas_sgemv( CblasRowMajor,  OP_A, M, N,
	       ALPHA, A, LDA, x, incX, BETA, y, incY);

  // now do y = 1.0*b + y
  cblas_saxpy( M, 1.0, b, incB, y, incY );
}
