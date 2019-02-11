ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT = infrastructure/shared

SBT_APPS 	 =
SBT_SSM_APPS =

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    = drain_ecs_container_instance \
                  ecs_ec2_instance_tagger

$(val $(call stack_setup))
