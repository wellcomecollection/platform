ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= storage

SBT_SSM_APPS = notifier archivist ingests ingests_api bags bags_api bag_replicator
SBT_APPS =
SBT_DOCKER_LIBRARIES = storage_common ingests_common
SBT_NO_DOCKER_LIBRARIES = bags_common storage_display

ECS_TASKS   = bagger callback_stub_server
LAMBDAS     = lambdas/trigger_bag_ingest

TF_NAME     = storage
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))