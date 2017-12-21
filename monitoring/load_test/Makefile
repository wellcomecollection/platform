ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile


gatling-build:
	$(call build_image,gatling,monitoring/load_test/gatling/Dockerfile)

gatling-publish: gatling-build
	$(call publish_service,gatling)


gatling_to_cloudwatch-test:
	$(call test_lambda,monitoring/load_test/gatling_to_cloudwatch)

gatling_to_cloudwatch-publish:
	$(call publish_lambda,monitoring/load_test/gatling_to_cloudwatch)


load_test-build: gatling-build gatling_to_cloudwatch-build

load_test-test: gatling_to_cloudwatch-test

load_test-publish: gatling-publish gatling_to_cloudwatch-publish
