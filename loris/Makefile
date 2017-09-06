ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/builds.Makefile
endif


loris-build: $(ROOT)/.docker/image_builder
	PROJECT=loris FILE=loris/Dockerfile $(ROOT)/builds/build_image.sh

loris-serve: loris-build
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
		--publish 8888:8888 \
		--env INFRA_BUCKET=$(INFRA_BUCKET) \
		--env CONFIG_KEY=config/prod/loris.ini \
		loris

loris-publish: loris-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=loris $(ROOT)/builds/publish_service.sh


.PHONY: loris-build loris-serve loris-publish
