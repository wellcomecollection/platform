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


SWITCH_API_PINS = $(ROOT)/catalogue_api/switch_api_pins

$(ROOT)/.docker/switch_api_pins: $(SWITCH_API_PINS)/Dockerfile $(SWITCH_API_PINS)/switch_api_pins.py
	docker build --tag switch_api_pins $(SWITCH_API_PINS)
	mkdir -p $(ROOT)/.docker
	touch $(ROOT)/.docker/switch_api_pins

catalogue_api-terraform-switch: $(ROOT)/.docker/switch_api_pins
	$(ROOT)/docker_run.py --aws --dind -- --volume ~/.ssh:/root/.ssh switch_api_pins
