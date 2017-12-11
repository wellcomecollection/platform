export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

dynamo_to_sns-build:
	$(call build_lambda,shared_infra/dynamo_to_sns)

dynamo_to_sns-test:
	$(call test_lambda,shared_infra/dynamo_to_sns)

dynamo_to_sns-publish:
	$(call publish_lambda,shared_infra/dynamo_to_sns)
