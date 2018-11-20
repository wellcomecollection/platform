ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = infrastructure/shared
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

SBT_APPS    =
ECS_TASKS   =
LAMBDAS     = lambdas/drain_ecs_container_instance \
              lambdas/ecs_ec2_instance_tagger

TF_NAME     = infra_shared
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
