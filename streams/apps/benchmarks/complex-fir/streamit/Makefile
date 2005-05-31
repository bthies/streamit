all: input.dat

ConvertFile.class: ConvertFile.java
	jikes -nowarn ConvertFile.java

input.dat: input.txt ConvertFile.class
	java ConvertFile input.txt input.dat

clean:
	rm input.dat
