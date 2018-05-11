ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	=

SBT_APPS 	=
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS 	=
LAMBDAS 	=

TF_NAME 	= assets
TF_PATH 	= assets/terraform

# Not technically public facing, but we want double-approval on
# changes here!
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))
