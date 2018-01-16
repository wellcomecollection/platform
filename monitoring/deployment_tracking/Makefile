ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring/deployment_tracking

SBT_APPS    =
ECS_TASKS   =
LAMBDAS     = notify_old_deploys service_deployment_status

$(val $(call stack_setup))
