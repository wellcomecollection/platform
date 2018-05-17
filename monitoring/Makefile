ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring

SBT_APPS    =
ECS_TASKS   = slack_budget_bot
LAMBDAS     = post_to_slack terraform_tracker

SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

TF_NAME     = monitoring
TF_PATH     = $(STACK_ROOT)
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

include $(ROOT)/monitoring/deployment_tracking/Makefile
include $(ROOT)/monitoring/ecs_dashboard/Makefile
include $(ROOT)/monitoring/load_test/Makefile
