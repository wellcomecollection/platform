ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_api

SBT_APPS 	= api
ECS_TASKS 	= api_docs
LAMBDAS 	=

TF_NAME 	= catalogue_api
TF_PATH 	= $(ROOT)/$(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))
