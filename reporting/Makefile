ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= reporting
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

SBT_APPS 	=
ECS_TASKS 	=
LAMBDAS 	= miro_transformer

TF_NAME 	= reporting
TF_PATH 	= $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
