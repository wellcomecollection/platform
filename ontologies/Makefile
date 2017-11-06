ROOT = $(shell git rev-parse --show-toplevel)
ONTOLOGIES = $(ROOT)/ontologies

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(ONTOLOGIES)/lint_turtle/requirements.txt: $(ONTOLOGIES)/lint_turtle/requirements.in
	docker run --rm --volume $(ONTOLOGIES)/lint_turtle:/src micktwomey/pip-tools


lint-turtle: $(ROOT)/.docker/python3.6_ci
	docker run --volume $(ROOT)/ontologies:/data --env OP=lint-turtle --tty python3.6_ci

lint-json: $(ROOT)/.docker/jslint_ci
	docker run --volume $(ROOT)/ontologies:/data jslint_ci:latest

lint-ontologies: lint-turtle lint-json


.PHONY: lint-turtle lint-json lint-ontologies
