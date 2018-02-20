ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= reindexer

SBT_APPS 	= reindex_worker
ECS_TASKS 	=
LAMBDAS 	= complete_reindex reindex_job_creator reindex_shard_generator resharder

TF_NAME 	= reindexer
TF_PATH 	= $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
