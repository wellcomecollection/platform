ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= archive

SBT_APPS    = archivist notifier registrar progress
SBT_DOCKER_LIBRARIES = archive_common
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   = bagger archive_api
LAMBDAS     = archive_bags archive_report_ingest_status archive_request_ingest

TF_NAME     = archive
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

include $(ROOT)/$(STACK_ROOT)/archive_api/Makefile
