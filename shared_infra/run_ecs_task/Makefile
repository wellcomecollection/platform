export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

run_ecs_task-build:
	$(call build_lambda,shared_infra/run_ecs_task)

run_ecs_task-test:
	$(call test_lambda,shared_infra/run_ecs_task)

run_ecs_task-publish:
	$(call publish_lambda,shared_infra/run_ecs_task)
