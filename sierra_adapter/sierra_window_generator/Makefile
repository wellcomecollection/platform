ROOT = $(shell git rev-parse --show-toplevel)
SIERRA_WINDOW_GENERATOR = $(ROOT)/sierra_adapter/sierra_window_generator

include $(ROOT)/functions.Makefile


$(SIERRA_WINDOW_GENERATOR)/src/requirements.txt: $(SIERRA_WINDOW_GENERATOR)/src/requirements.in
	$(ROOT)/builds/docker_run.py -- \
		--volume $(SIERRA_WINDOW_GENERATOR)/src:/src micktwomey/pip-tools

sierra_window_generator-test:
	$(call test_lambda,sierra_adapter/sierra_window_generator)

sierra_window_generator-publish:
	$(call publish_lambda,sierra_adapter/sierra_window_generator)
