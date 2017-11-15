ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline

include $(ROOT)/functions.Makefile
include $(CATALOGUE_PIPELINE)/schedule_reindexer/Makefile


catalogue_pipeline-build: schedule_reindexer-build

catalogue_pipeline-test: schedule_reindexer-test

catalogue_pipeline-deploy: schedule_reindexer-publish


catalogue_pipeline-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_PIPELINE))

catalogue_pipeline-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_PIPELINE))
