ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

SIERRA_OBJECTS_TO_S3 = $(ROOT)/sierra_adapter/sierra_objects_to_s3

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(SIERRA_OBJECTS_TO_S3)/requirements.txt: $(SIERRA_OBJECTS_TO_S3)/requirements.in
	docker run --volume $(SIERRA_OBJECTS_TO_S3):/src micktwomey/pip-tools

sierra_objects_to_s3-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=sierra_objects_to_s3 \
		--file=sierra_adapter/sierra_objects_to_s3/Dockerfile

sierra_objects_to_s3-test: sierra_objects_to_s3-build
	docker build \
		--tag sierra_objects_to_s3:test \
		--file=$(SIERRA_OBJECTS_TO_S3)/test.Dockerfile \
		$(SIERRA_OBJECTS_TO_S3)
	docker run \
		--volume $(SIERRA_OBJECTS_TO_S3)/cassettes:/cassettes \
		--tty sierra_objects_to_s3:test

sierra_objects_to_s3-publish: $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=sierra_objects_to_s3 \
		--infra-bucket=$(INFRA_BUCKET)