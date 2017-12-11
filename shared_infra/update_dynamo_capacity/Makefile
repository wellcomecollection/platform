export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

update_dynamo_capacity-build:
	$(call build_lambda,shared_infra/update_dynamo_capacity)

update_dynamo_capacity-test:
	$(call test_lambda,shared_infra/update_dynamo_capacity)

update_dynamo_capacity-publish:
	$(call publish_lambda,shared_infra/update_dynamo_capacity)
