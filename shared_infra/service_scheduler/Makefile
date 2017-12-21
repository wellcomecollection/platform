export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

service_scheduler-test:
	$(call test_lambda,shared_infra/service_scheduler)

service_scheduler-publish:
	$(call publish_lambda,shared_infra/service_scheduler)
