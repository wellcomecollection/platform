ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


## Lint the onotologies folder
lint-ontologies: $(ROOT)/.docker/jslint_ci
	docker run -v $(ROOT)/onotologies:/data jslint_ci:latest


.PHONY: lint-ontologies
