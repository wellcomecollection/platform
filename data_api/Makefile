ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_api

SBT_APPS    = snapshot_generator
ECS_TASKS   = elasticdump
LAMBDAS     = snapshot_scheduler

TF_NAME     = data_api
TF_PATH     = $(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = true


elasticdump-test: elasticdump-build
	$(ROOT)/docker_run.py --aws --dind -- \
		--volume $(ROOT):/repo \
		wellcome/build_test_lambda data_api/elasticdump
	$(ROOT)/docker_run.py --aws --dind -- \
		--net=host \
		--volume $(ROOT)/data_api/elasticdump:/data \
		--volume $(ROOT)/shared_conftest.py:/conftest.py \
		--env INSTALL_DEPENDENCIES=false \
		--env FIND_MATCH_PATHS="/data" --tty \
		wellcome/test_lambda_elasticdump

$(ROOT)/data_api/elasticdump/src/requirements.txt:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/data_api/elasticdump/src/:/src micktwomey/pip-tools

$(val $(call stack_setup))
