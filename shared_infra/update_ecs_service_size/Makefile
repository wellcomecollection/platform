export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

update_ecs_service_size-test:
	$(call test_lambda,shared_infra/update_ecs_service_size)

update_ecs_service_size-publish:
	$(call publish_lambda,shared_infra/update_ecs_service_size)
