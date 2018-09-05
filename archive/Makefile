ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= archive

SBT_APPS    = archivist registrar
SBT_DOCKER_LIBRARIES = archive_common
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   =
LAMBDAS     = archive_bags archive_ingest

TF_NAME     = archive
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
