ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

image_sorter-test:
	$(call test_lambda,miro_preprocessor/image_sorter)

image_sorter-publish:
	$(call publish_lambda,miro_preprocessor/image_sorter)
