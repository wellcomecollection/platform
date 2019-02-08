ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= storage

SBT_APPS 	 =
SBT_SSM_APPS = notifier \
               archivist \
               ingests \
               ingests_api \
               bags \
               bags_api \
               bag_replicator


ACCOUNT_ID = 975596993436
PROJECT_ID = storage

SBT_DOCKER_LIBRARIES    = storage_common ingests_common
SBT_NO_DOCKER_LIBRARIES = bags_common storage_display

PYTHON_SSM_APPS = bagger
PYTHON_APPS     =
LAMBDAS 	    = lambdas/trigger_bag_ingest

TF_NAME = storage
TF_PATH = $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))