ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_api

SBT_APPS    = api
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   = update_api_docs
LAMBDAS     =

TF_NAME     = catalogue_api
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))


$(ROOT)/.docker/switch_api_pins: $(ROOT)/catalogue_api/switch_api_pins/Dockerfile
	docker build --tag switch_api_pins $(ROOT)/catalogue_api/switch_api_pins
	mkdir -p $(ROOT)/.docker
	touch $(ROOT)/.docker/switch_api_pins

catalogue_api-terraform-switch: $(ROOT)/.docker/switch_api_pins
	$(ROOT)/docker_run.py --aws --root -- switch_api_pins
