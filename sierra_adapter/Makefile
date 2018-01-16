ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = sierra_adapter

SBT_APPS    = sierra_bib_merger \
              sierra_item_merger \
			  sierra_items_to_dynamo \
			  sierra_reader
ECS_TASKS   =
LAMBDAS     = s3_demultiplexer sierra_window_generator

TF_NAME     = sierra_adapter
TF_PATH     = $(ROOT)/$(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))


SIERRA_WINDOW_GENERATOR = $(ROOT)/sierra_adapter/sierra_window_generator

$(SIERRA_WINDOW_GENERATOR)/src/requirements.txt: $(SIERRA_WINDOW_GENERATOR)/src/requirements.in
	$(ROOT)/builds/docker_run.py -- \
		--volume $(SIERRA_WINDOW_GENERATOR)/src:/src micktwomey/pip-tools


sierra_common-test:
	$(call sbt_test,sierra_adapter_common)

sierra_common-publish:
	echo "Nothing to do!"

