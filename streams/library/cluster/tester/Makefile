submit:	check
	@mkdir -p ${NAME}
	@if test -L .tmp; then rm .tmp; fi
	@ln -s ${NAME} .tmp
	@if test -f .tmp/.ccphost; then rm .tmp/.ccphost; fi
	@env NODES=${NODES} NAME=${NAME} PROGDIR=${PROGDIR} ITERS=${ITERS} run.sh
check:
ifndef NAME
	@echo "Missing NAME = Directory to store data in"
	@exit 1
endif
	@if ! test -e ${NAME}; then mkdir ${NAME}; fi
	@if ! test -d ${NAME}; then echo "Error with directory: ${NAME}"; exit 1; fi
ifndef NODES
	@echo "Missing NODES = Number of nodes"
	@exit 1
endif
ifndef PROGDIR
	@echo "Missing PROGDIR = Program directory"
	@exit 1
endif
	@if echo "${PROGDIR}" | grep \~; then echo "Error, PROGDIR must be absolute"; exit 1; fi
ifndef ITERS
	@echo "Missing ITERS = Iterations"
	@exit 1
endif
