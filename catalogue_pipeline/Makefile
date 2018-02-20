ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_pipeline

SBT_APPS 	= id_minter ingestor transformer
ECS_TASKS 	=
LAMBDAS 	=

TF_NAME 	= catalogue_pipeline
TF_PATH 	= $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
