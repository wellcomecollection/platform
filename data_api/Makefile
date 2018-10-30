ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_api

SBT_APPS    = snapshot_generator
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   =
LAMBDAS     = snapshot_scheduler snapshot_slack_alarms

TF_NAME     =
TF_PATH     =
TF_IS_PUBLIC_FACING =


$(val $(call stack_setup))
