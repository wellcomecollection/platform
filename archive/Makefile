ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= archive

SBT_APPS    = archivist registrar progress_async progress_http notifier registar_http
SBT_DOCKER_LIBRARIES = archive_common
SBT_NO_DOCKER_LIBRARIES =

ECS_TASKS   = bagger archive_api callback_stub_server
LAMBDAS     =

TF_NAME     = archive
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

include $(ROOT)/$(STACK_ROOT)/archive_api/Makefile
