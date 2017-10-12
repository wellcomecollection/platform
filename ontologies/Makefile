ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


lint-turtle: $(ROOT)/.docker/python3.6_ci
	docker run --volume $(ROOT)/ontologies:/data --env OP=lint-turtle --tty python3.6_ci

lint-json: $(ROOT)/.docker/jslint_ci
	docker run --volume $(ROOT)/ontologies:/data jslint_ci:latest

lint-ontologies: lint-turtle lint-json


.PHONY: lint-turtle lint-json lint-ontologies
