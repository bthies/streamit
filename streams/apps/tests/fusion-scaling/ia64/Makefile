all:
	# generate compile script
	perl -pi -e 's/MODE\=2/MODE=1/g' genscaling.sh
	./genscaling.sh > docompile.sh
	# generate run script
	perl -pi -e 's/MODE\=1/MODE=2/g' genscaling.sh
	./genscaling.sh > dorun.sh
	# make runnable
	chmod u+x docompile.sh dorun.sh
