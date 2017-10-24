ROOT = $(shell git rev-parse --show-toplevel)
FREEZERAY = $(ROOT)/shared_infra/sqs_freezeray

$(FREEZERAY)/requirements.txt: $(FREEZERAY)/requirements.in
	docker run --volume $(FREEZERAY):/src --rm micktwomey/pip-tools

sqs_freezeray-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=sqs_freezeray \
		--file=shared_infra/sqs_freezeray/Dockerfile

sqs_freezeray-publish: $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=sqs_freezeray \
		--infra-bucket=$(INFRA_BUCKET)

.PHONY: sqs_freezeray-build
