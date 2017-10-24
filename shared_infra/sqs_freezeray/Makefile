ROOT = $(shell git rev-parse --show-toplevel)
FREEZERAY = $(ROOT)/shared_infra/sqs_freezeray

$(FREEZERAY)/requirements.txt: $(FREEZERAY)/requirements.in
	docker run --volume $(FREEZERAY):/src --rm micktwomey/pip-tools
