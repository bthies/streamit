default: run_transcoder

run_transcoder: Transcoder.str input.jpg.int
	strc Transcoder.str -library -nosched

input.jpg.int: ../input/input.jpg ../utils/b2i
	../utils/b2i ../input/input.jpg ../streamit/input.jpg.int

../utils/b2i: ../utils/b2i.c
	g++ ../utils/b2i.c -o ../utils/b2i

output.jpg: output.jpg.int ../utils/i2b
	../utils/i2b ../streamit/output.jpg.int ../streamit/output.jpg

../utils/i2b: ../utils/i2b.c
	g++ ../utils/i2b.c -o ../utils/i2b

jpegtobmp: JPEGtoBMP.str input.jpg.int
	strc JPEGtoBMP.str -library -nosched

clean:
	rm -f *.class
	rm -f *.dot
	rm -f *.java
	rm -f *.int
	rm -f output.jpg