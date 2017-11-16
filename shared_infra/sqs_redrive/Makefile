ROOT = $(shell git rev-parse --show-toplevel)

sqs_redrive-build:
	$(call build_image,sqs_redrive,shared_infra/sqs_redrive/Dockerfile)

sqs_redrive-publish: sqs_redrive-build
	$(call publish_service,sqs_redrive)
