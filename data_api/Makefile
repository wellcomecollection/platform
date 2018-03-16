ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_api

SBT_APPS    = snapshot_convertor
ECS_TASKS   = elasticdump
LAMBDAS     = snapshot_scheduler snapshot_convertor_job_generator

TF_NAME     = data_api
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))
