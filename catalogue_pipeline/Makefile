ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


catalogue_pipeline-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(CATALOGUE_PIPELINE)/terraform:/data \
		--env OP=plan \
		terraform_ci:latest

catalogue_pipeline-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(CATALOGUE_PIPELINE)/terraform:/data \
		--env OP=apply \
		terraform_ci:latest


.PHONY: catalogue_pipeline-terraform-plan catalogue_pipeline-terraform-apply
