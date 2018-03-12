ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_api

SBT_APPS    =
ECS_TASKS   =
LAMBDAS     =

TF_NAME     = data_api
TF_PATH     = $(STACK_ROOT)
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))