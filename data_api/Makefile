ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_api

SBT_APPS    = snapshot_generator
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   =
LAMBDAS     = snapshot_scheduler

TF_NAME     = data_api
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = true



$(val $(call stack_setup))
