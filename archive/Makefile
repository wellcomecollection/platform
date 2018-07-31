ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= archive

SBT_APPS    = archiver
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   =
LAMBDAS     =

TF_NAME     = archive
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))
