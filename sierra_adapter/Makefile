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
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))


$(ROOT)/.docker/sierra_adapter_progress:
	echo "FROM python:3-alpine\nRUN pip3 install attrs boto3" | docker build --tag sierra_adapter_progress -
	mkdir -p $(ROOT)/.docker
	touch $(ROOT)/.docker/sierra_adapter_progress

sierra_adapter_progress: $(ROOT)/.docker/sierra_adapter_progress
	$(ROOT)/.scripts/docker_run.py --aws -- \
		--volume $(ROOT)/$(STACK_ROOT):/src \
		sierra_adapter_progress \
		python /src/report_adapter_progress.py


sierra_common-test:
	$(call sbt_test,sierra_adapter_common)

sierra_common-publish:
	echo "Nothing to do!"

