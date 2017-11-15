ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile
include $(CATALOGUE_PIPELINE)/schedule_reindexer/Makefile

## Build catalogue_pipeline
catalogue_pipeline-build: schedule_reindexer-build

## Test catalogue_pipeline
catalogue_pipeline-test: schedule_reindexer-test

## Deploy catalogue_pipeline
catalogue_pipeline-deploy: schedule_reindexer-publish

catalogue_pipeline-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_PIPELINE))

catalogue_pipeline-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(CATALOGUE_PIPELINE):/data \
		--env OP=apply \
		terraform_ci:latest


.PHONY: catalogue_pipeline-terraform-plan catalogue_pipeline-terraform-apply
