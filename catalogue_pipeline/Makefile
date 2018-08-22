ROOT = $(shell git rev-parse --show-toplevel)
STACK_ROOT 	= catalogue_pipeline

include $(ROOT)/functions.Makefile

SBT_APPS 	= id_minter ingestor recorder matcher merger
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS 	=
LAMBDAS 	=

TF_NAME 	= catalogue_pipeline
TF_PATH 	= $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

include $(ROOT)/$(STACK_ROOT)/transformer/Makefile
