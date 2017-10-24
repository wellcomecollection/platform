ROOT = $(shell git rev-parse --show-toplevel)
FREEZERAY = $(ROOT)/shared_infra/sqs_freezeray

$(FREEZERAY)/requirements.txt: $(FREEZERAY)/requirements.in
	docker run --volume $(FREEZERAY):/src --rm micktwomey/pip-tools

sqs_freezeray-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=sqs_freezeray \
		--file=shared_infra/sqs_freezeray/Dockerfile

.PHONY: sqs_freezeray-build
