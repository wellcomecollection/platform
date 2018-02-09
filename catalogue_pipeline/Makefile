ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_pipeline

SBT_APPS 	= id_minter ingestor reindexer_v2/reindex_worker transformer
ECS_TASKS 	=
LAMBDAS 	= schedule_reindexer reindexer_v2/complete_reindex reindexer_v2/reindex_job_creator

TF_NAME 	= catalogue_pipeline
TF_PATH 	= $(ROOT)/catalogue_pipeline/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
