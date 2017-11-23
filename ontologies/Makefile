ROOT = $(shell git rev-parse --show-toplevel)
ONTOLOGIES = $(ROOT)/ontologies

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(ONTOLOGIES)/lint_turtle/requirements.txt: $(ONTOLOGIES)/lint_turtle/requirements.in
	docker run --rm --volume $(ONTOLOGIES)/lint_turtle:/src micktwomey/pip-tools

$(ROOT)/.docker/lint_turtle:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=lint_turtle \
		--dir=$(ONTOLOGIES)/lint_turtle

lint-turtle: $(ROOT)/.docker/lint_turtle
	docker run --rm --volume $(ONTOLOGIES):/data --tty lint_turtle

lint-json:
	docker run --volume $(ROOT)/ontologies:/data wellcome/jslint:latest

lint-ontologies: lint-turtle lint-json


.PHONY: lint-turtle lint-json lint-ontologies
