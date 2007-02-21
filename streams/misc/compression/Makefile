all: aa2raw pam2raw raw2pam setup-ffmpeg

FFMPEG_HOME=/projects/streamit/tools/ffmpeg

# sorry, my config skills are less than stellar
setup-ffmpeg: Makefile
	echo "export FFMPEG_HOME=${FFMPEG_HOME}" > setup-ffmpeg
	chmod u+x setup-ffmpeg

aa2raw: aa2raw.c
	gcc aa2raw.c -o aa2raw

pam2raw: pam2raw.c
	gcc pam2raw.c -o pam2raw

raw2pam: raw2pam.c
	gcc raw2pam.c -o raw2pam

clean:
	rm -f aa2raw pam2raw raw2pam setup-ffmpeg *~ #*
