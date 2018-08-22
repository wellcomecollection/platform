ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_pipeline/transformer

SBT_APPS 	= transformer_miro transformer_sierra
SBT_DOCKER_LIBRARIES = transformer_common
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS 	=
LAMBDAS 	=

TF_NAME 	=
TF_PATH 	=

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
