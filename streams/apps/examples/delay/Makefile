# make test harnesses for DelayOne.str Delay.str DelayMany.str 

all : DelayOneTest.str DelayTest.str DelayManyTest.str

DelayOneTest.str : DelayOne.str DelayOneTest \
 ../../libraries/FloatSeqSource.str ../../libraries/FloatPrinter.str
	cat $+ > $@

DelayTest.str :  Delay.str DelayTest \
 ../../libraries/FloatSeqSource.str ../../libraries/FloatPrinter.str
	cat $+ > $@

DelayManyTest.str :  DelayMany.str DelayManyTest \
 ../../libraries/FloatSeqSource.str ../../libraries/FloatPrinter.str
	cat $+ > $@
