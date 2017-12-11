ROOT = $(shell git rev-parse --show-toplevel)
FREEZERAY = $(ROOT)/shared_infra/sqs_freezeray

include $(ROOT)/functions.Makefile

$(FREEZERAY)/requirements.txt: $(FREEZERAY)/requirements.in
	docker run --rm --volume $(FREEZERAY):/src --rm micktwomey/pip-tools

sqs_freezeray-build:
	$(call build_image,sqs_freezeray,shared_infra/sqs_freezeray/Dockerfile)

sqs_freezeray-publish: sqs_freezeray-build
	$(call publish_service,sqs_freezeray)
