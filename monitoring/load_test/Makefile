ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring/load_test

SBT_APPS    =
ECS_TASKS   = gatling
LAMBDAS     = gatling_to_cloudwatch

TF_NAME     =
TF_PATH     =
TF_IS_PUBLIC_FACING =

$(val $(call stack_setup))


ECS_DASHBOARD = $(ROOT)/monitoring/ecs_dashboard
