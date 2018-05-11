ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= loris
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

SBT_APPS 	=
ECS_TASKS 	= loris cache_cleaner
LAMBDAS 	=

TF_NAME 	= loris
TF_PATH 	= loris/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))


# TODO: Flip this to using micktwomey/pip-tools when that's updated
# with a newer version of pip-tools.
$(ROOT)/loris/loris/requirements.txt: $(ROOT)/loris/loris/requirements.in
	docker run --rm \
		--volume $(ROOT)/loris/loris:/data \
		wellcome/build_tooling:latest \
		pip-compile

loris-run: loris-build
	$(ROOT)/docker_run.py -- --publish 8888:8888 loris
