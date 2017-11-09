ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

SIERRA_ADAPTER = $(ROOT)/sierra_adapter

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(SIERRA_ADAPTER)/sierra_objects_to_s3/requirements.txt: $(SIERRA_ADAPTER)/requirements.in
	docker run --volume $(SIERRA_ADAPTER):/src micktwomey/pip-tools

sierra_objects_to_s3-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=sierra_objects_to_s3 \
		--file=sierra_adapter/sierra_objects_to_s3/Dockerfile

sierra_adapter-build: sierra_objects_to_s3-build

sierra_objects_to_s3-test: sierra_objects_to_s3-build
	docker build \
		--tag sierra_objects_to_s3:test \
		--file=$(SIERRA_ADAPTER)/sierra_objects_to_s3/test.Dockerfile \
		$(SIERRA_ADAPTER)/sierra_objects_to_s3
	docker run \
		--volume $(SIERRA_ADAPTER)/sierra_objects_to_s3/cassettes:/cassettes \
		--tty sierra_objects_to_s3:test

sierra_adapter-test: sierra_objects_to_s3-test

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

sierra_adapter-publish:
	echo "Nothing to do!"


.PHONY: sierra_adapter-terraform-plan sierra_adapter-terraform-apply
