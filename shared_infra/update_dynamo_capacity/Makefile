export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

update_dynamo_capacity-build:
	$(call build_lambda,shared_infra/update_dynamo_capacity)

update_dynamo_capacity-test:
	$(call test_lambda,shared_infra/update_dynamo_capacity)

update_dynamo_capacity-publish:
	$(call publish_lambda,shared_infra/update_dynamo_capacity)
