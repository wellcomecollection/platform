export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

update_ecs_service_size-build:
	$(call build_lambda,shared_infra/update_ecs_service_size)

update_ecs_service_size-test:
	$(call test_lambda,shared_infra/update_ecs_service_size)

update_ecs_service_size-publish:
	$(call publish_lambda,shared_infra/update_ecs_service_size)
