ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_pipeline

SBT_APPS 	= id_minter ingestor reindex_worker transformer
ECS_TASKS 	=
LAMBDAS 	= reindexer_v2/complete_reindex \
              reindexer_v2/reindex_job_creator \
              reindexer_v2/reindex_shard_generator \
			  reindexer_v2/resharder

TF_NAME 	= catalogue_pipeline
TF_PATH 	= $(ROOT)/catalogue_pipeline/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
