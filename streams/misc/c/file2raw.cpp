#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <iostream>

void main (int argc, char *argv[])
{
	assert (argc==2);
	char *name = argv [1];
	char newName [100];
	strcpy (newName, name);
	strcat (newName, ".raw");
	cout << "creating " << newName << " from " << name << endl;

	FILE *fin = fopen (name, "rb");
	assert (fin);

	// seek end of file
	fseek (fin, 0, SEEK_END);

	// figure out how long the file really is
	long pos = ftell (fin);
	cout << name << " is " << pos << " bytes long." << endl;
	int wordLength = pos / 4;
	cout << "That translates to " << wordLength << " words." << endl;
	fseek (fin, 0, SEEK_SET);

	// open the output file
	FILE *fout = fopen (newName, "wb");
	assert (fout);

	// write the length of the file to the output file:
	fwrite (&wordLength, sizeof (int), 1, fout);

	// and now copy the file over
	while (wordLength)
	{
		wordLength--;
		int data;
		fread (&data, sizeof (int), 1, fin);
		fwrite (&data, sizeof (int), 1, fout);
	}

	fclose (fin);
	fclose (fout);
}
