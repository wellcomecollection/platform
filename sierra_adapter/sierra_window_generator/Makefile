ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra
SIERRA_WINDOW_GENERATOR = $(ROOT)/sierra_adapter/sierra_window_generator

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile


$(SIERRA_WINDOW_GENERATOR)/src/requirements.txt: $(SIERRA_WINDOW_GENERATOR)/src/requirements.in
	docker run --volume $(SIERRA_WINDOW_GENERATOR):/src micktwomey/pip-tools

sierra_window_generator-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_WINDOW_GENERATOR)/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

sierra_window_generator-publish: $(ROOT)/.docker/publish_lambda_zip
	$(call publish_lambda,sierra_adapter/sierra_window_generator)
