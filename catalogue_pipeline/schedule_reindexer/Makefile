export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

schedule_reindexer-build:
	$(call build_lambda,catalogue_pipeline/schedule_reindexer)

schedule_reindexer-test:
	$(call test_lambda,catalogue_pipeline/schedule_reindexer)

schedule_reindexer-publish:
	$(call publish_lambda,catalogue_pipeline/schedule_reindexer)
