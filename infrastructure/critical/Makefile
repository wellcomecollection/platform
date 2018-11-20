ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= infrastructure/infra_critical

SBT_APPS 	=
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS 	=
LAMBDAS 	= lambdas/dynamo_write_heartbeat


TF_NAME 	= infra_critical
TF_PATH 	= $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
