#include <fstream>
#include <iostream>
using namespace std;

main(int argc, char **argv)
{
  if (argc != 3)
    cout << "Syntax: b2i <inputfilename> <outputfilename>\n";
  else {
    ifstream in(argv[1], ios::in | ios::binary);
    ofstream out(argv[2], ios::out | ios::binary);
    while (!in.eof()) {
      unsigned char f;
      in.read((char*) &f, sizeof(unsigned char));
      int g = (int) f;
      out.write((char*) &g, sizeof(int));
    }
  }
}

