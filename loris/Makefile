ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= loris

SBT_APPS 	=
ECS_TASKS 	=
LAMBDAS 	=

TF_NAME 	= loris
TF_PATH 	= $(ROOT)/loris/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))