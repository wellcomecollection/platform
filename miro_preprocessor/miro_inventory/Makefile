ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile


miro_inventory-test:
	$(call test_lambda,miro_preprocessor/miro_inventory)

miro_inventory-publish:
	$(call publish_lambda,miro_preprocessor/miro_inventory)
