ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= archive

SBT_APPS    = archivist progress_async progress_http notifier registrar_async registrar_http
SBT_DOCKER_LIBRARIES = archive_common progress_common
SBT_NO_DOCKER_LIBRARIES = registrar_common archive_display

ECS_TASKS   = bagger callback_stub_server
LAMBDAS     = migrator

TF_NAME     = archive
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))