ROOT = $(shell git rev-parse --show-toplevel)

sqs_redrive-build:
	$(call build_image,sqs_redrive,shared_infra/sqs_redrive/Dockerfile)

sqs_redrive-publish: sqs_redrive-build $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=sqs_redrive \
		--infra-bucket=$(INFRA_BUCKET)
