#include <fstream>
#include <iostream>
using namespace std;

main(int argc, char **argv) {
  if (argc != 3)
    cout << "Syntax: i2b <inputfilename> <outputfilename>\n";
  else {
    ifstream in(argv[1], ios::in | ios::binary);
    ofstream out(argv[2], ios::out | ios::binary);
    while (!in.eof()) {
      int g;
      in.read((char*) &g, sizeof(int));
      unsigned char f = (unsigned char) g;
      out.write((char*) &f, sizeof(unsigned char));
    }
  }
}
