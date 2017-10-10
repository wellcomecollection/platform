ROOT = $(shell git rev-parse --show-toplevel)
LORIS = $(ROOT)/loris

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


loris-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder \
		--project=loris \
		--file=loris/Dockerfile

loris-run: loris-build
	$(ROOT)/builds/docker_run.py --aws -- \
		--publish 8888:8888 \
		--env INFRA_BUCKET=$(INFRA_BUCKET) \
		--env CONFIG_KEY=config/prod/loris.ini \
		loris

loris-publish: loris-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=loris $(ROOT)/builds/publish_service.sh

loris-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(LORIS)/terraform:/data \
		--volume $(ROOT)/terraform:/terraform \
		--env OP=plan \
		terraform_ci:latest

loris-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(LORIS)/terraform:/data \
		--volume $(ROOT)/terraform:/terraform \
		--env OP=apply \
		terraform_ci:latest


cache_cleaner-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- \
		image_builder \
		--project=cache_cleaner \
		--file=loris/cache_cleaner/Dockerfile

cache_cleaner-publish: cache_cleaner-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=cache_cleaner ./builds/publish_service.sh


.PHONY: loris-build loris-run loris-publish
