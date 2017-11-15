ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

SIERRA_ADAPTER = $(ROOT)/sierra_adapter

include $(SIERRA_ADAPTER)/sierra_objects_to_s3/Makefile
include $(SIERRA_ADAPTER)/sierra_window_generator/Makefile

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

sierra_adapter-test: sierra_objects_to_s3-test sierra_window_generator-test
sierra_adapter-adapter-publish: sierra_objects_to_s3-publish sierra_window_generator-publish

sierra_adapter-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER):/data \
		--env OP=plan \
		terraform_ci:latest

sierra_adapter-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER):/data \
		--env OP=apply \
		terraform_ci:latest

sierra_adapter-publish:
	echo "Nothing to do!"


.PHONY: sierra_adapter-terraform-plan sierra_adapter-terraform-apply
