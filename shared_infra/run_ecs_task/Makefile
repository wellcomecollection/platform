export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

run_ecs_task-test:
	$(call test_lambda,shared_infra/run_ecs_task)

run_ecs_task-publish:
	$(call publish_lambda,shared_infra/run_ecs_task)
