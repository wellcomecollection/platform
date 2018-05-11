ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_science

SBT_APPS    =
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   =
LAMBDAS     =

TF_NAME     = data_science
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false



$(val $(call stack_setup))
