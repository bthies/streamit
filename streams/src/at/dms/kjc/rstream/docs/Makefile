rstream.ps: clean rstream.dvi
	dvips rstream.dvi -o rstream.ps

rstream.pdf: clean rstream.dvi
	dvipdf rstream.dvi rstream.pdf

rstream.dvi: $(FILES)
	latex rstream
	bibtex rstream
	latex rstream
	latex rstream

clean:
	$(RM) *.aux *.bbl *.blg *.dvi *.log
