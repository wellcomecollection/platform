ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT 	= loris

PYTHON_APPS     =
PYTHON_SSM_APPS = loris
LAMBDAS 	    =

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
