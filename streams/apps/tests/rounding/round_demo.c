int Y = 205;
int Cb = 121;
int Cr = 131;


// This gives the wrong answer (default cluster code).
void computeWithFloat() {
  float Ey = ((Y - (16.0f)) / (219.0f))/*float*/;
  float Epb = ((Cb - (128.0f)) / (224.0f))/*float*/;
  float Epr = ((Cr - (128.0f)) / (224.0f))/*float*/;

  float EB = ((((1.001856f) * Ey) + ((1.855625f) * Epb)) + ((0.001005f) * Epr))/*float*/;
  int B = ((int)(round((EB * (256.0f)))))/*int*/;

  printf("\n");
  printf("C default:\n");
  printf("Ey = %3.30f\n", Ey);
  printf("Epb = %3.30f\n", Epb);
  printf("Epr = %3.30f\n", Epr);
  printf("EB = %3.30f\n", EB);;
  printf("EB*256 = %3.30f\n", EB * 256.0f);
  printf("B = %d\n", B);
}

// This gives the right answer (hack to cluster code).
void computeWithDouble() {
  double Ey = ((Y - (16.0f)) / (219.0f))/*float*/;
  double Epb = ((Cb - (128.0f)) / (224.0f))/*float*/;
  double Epr = ((Cr - (128.0f)) / (224.0f))/*float*/;

  float EB = ((((1.001856f) * Ey) + ((1.855625f) * Epb)) + ((0.001005f) * Epr))/*float*/;

  int B = ((int)(round((EB * (256.0f)))))/*int*/;

  printf("\n");
  printf("C as doubles:\n");
  printf("Ey = %3.30f\n", Ey);
  printf("Epb = %3.30f\n", Epb);
  printf("Epr = %3.30f\n", Epr);
  printf("EB = %3.30f\n", EB);;
  printf("EB*256 = %3.30f\n", EB * 256.0f);
  printf("B = %d\n", B);
}

// This gives the right answer (Java library code adapted to C)
void computeWithFloatJavaStyle() {
  float Ey = (((float) Y) - 16)/219.0;
  float Epb = (((float) Cb)-128)/224.0;
  float Epr = (((float) Cr)-128)/224.0;
  
  float EB = 1.001856*Ey + 1.855625*Epb + 0.001005*Epr;

  int B = (int) round(EB*256);
  
  printf("B = %d\n", B);
}


int main() {
  computeWithFloat();
  computeWithDouble();
  //computeWithFloatJavaStyle();
  return 0;
}
