default: clean Gsm.str

Gsm.str: StGsmDecoder.str
	cpp -P StGsmDecoder.str > $@

clean:
	rm -f Gsm.*
