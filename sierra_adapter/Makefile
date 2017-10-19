ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

SIERRA_ADAPTER = $(ROOT)/sierra_adapter

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(SIERRA_ADAPTER)/requirements.txt: $(SIERRA_ADAPTER)/requirements.in
	docker run --volume $(SIERRA_ADAPTER):/src micktwomey/pip-tools

sierra_adapter-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=sierra_adapter \
		--file=sierra_adapter/Dockerfile

sierra_adapter-test: sierra_adapter-build
	docker build \
		--tag sierra_adapter:test \
		--file=$(SIERRA_ADAPTER)/test.Dockerfile \
		$(SIERRA_ADAPTER)
	docker run --tty sierra_adapter:test

sierra_adapter-adapter-publish: $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=sierra_adapter \
		--infra-bucket=$(INFRA_BUCKET)


sierra_adapter-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER)/terraform:/data \
		--env OP=plan \
		terraform_ci:latest

sierra_adapter-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER)/terraform:/data \
		--env OP=apply \
		terraform_ci:latest


.PHONY: sierra_adapter-terraform-plan sierra_adapter-terraform-apply
