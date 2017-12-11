ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

miro_image_to_dynamo-build:
	$(call build_lambda,miro_preprocessor/miro_image_to_dynamo)

miro_image_to_dynamo-test:
	$(call test_lambda,miro_preprocessor/miro_image_to_dynamo)

miro_image_to_dynamo-publish:
	$(call publish_lambda,miro_preprocessor/miro_image_to_dynamo)
