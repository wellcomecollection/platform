ROOT = $(shell git rev-parse --show-toplevel)
FREEZERAY = $(ROOT)/shared_infra/sqs_freezeray

include $(ROOT)/functions.Makefile

$(FREEZERAY)/requirements.txt: $(FREEZERAY)/requirements.in
	docker run --volume $(FREEZERAY):/src --rm micktwomey/pip-tools

sqs_freezeray-build:
	$(call build_image,sqs_freezeray,shared_infra/sqs_freezeray/Dockerfile)

sqs_freezeray-publish: $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=sqs_freezeray \
		--infra-bucket=$(INFRA_BUCKET)

.PHONY: sqs_freezeray-build
