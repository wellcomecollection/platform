ROOT = $(shell git rev-parse --show-toplevel)
REDRIVE = $(ROOT)/shared_infra/sqs_redrive

sqs_redrive-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=sqs_redrive \
		--file=shared_infra/sqs_redrive/Dockerfile

sqs_redrive-publish: sqs_redrive-build $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=sqs_redrive \
		--infra-bucket=$(INFRA_BUCKET)
