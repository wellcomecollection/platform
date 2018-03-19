ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring/deployment_tracking

SBT_APPS    =
ECS_TASKS   =
LAMBDAS     = notify_ecr_pushes notify_old_deploys service_deployment_status

TF_NAME     =
TF_PATH     =
TF_IS_PUBLIC_FACING =

$(val $(call stack_setup))
