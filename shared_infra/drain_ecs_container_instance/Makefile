export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

drain_ecs_container_instance-build:
	$(call build_lambda,shared_infra/drain_ecs_container_instance)

drain_ecs_container_instance-test:
	$(call test_lambda,shared_infra/drain_ecs_container_instance)

drain_ecs_container_instance-publish:
	$(call publish_lambda,shared_infra/drain_ecs_container_instance)
