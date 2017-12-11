export ROOT = $(shell git rev-parse --show-toplevel)
export CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif
include $(ROOT)/functions.Makefile

schedule_reindexer-build:
	$(call build_lambda,catalogue_pipeline/schedule_reindexer)

schedule_reindexer-test:
	$(call test_lambda,catalogue_pipeline/schedule_reindexer)

schedule_reindexer-publish:
	$(call publish_lambda,catalogue_pipeline/schedule_reindexer)
