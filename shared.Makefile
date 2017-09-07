# This Makefile contains tasks and underlying pieces that are shared
# across our other Makefiles.

export INFRA_BUCKET = platform-infra

ROOT = $(shell git rev-parse --show-toplevel)

$(ROOT)/.docker/image_builder:
	$(ROOT)/scripts/build_ci_docker_image.py \
		--project=image_builder \
		--dir=builds \
		--file=builds/image_builder.Dockerfile

$(ROOT)/.docker/publish_service_to_aws:
	$(ROOT)/scripts/build_ci_docker_image.py \
		--project=publish_service_to_aws \
		--dir=builds \
		--file=builds/publish_service_to_aws.Dockerfile
