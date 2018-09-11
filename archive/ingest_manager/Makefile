ROOT = $(shell git rev-parse --show-toplevel)
INGEST_MANAGER = $(ROOT)/archive/ingest_manager


$(INGEST_MANAGER)/src/requirements.txt: $(INGEST_MANAGER)/src/requirements.in
	docker run --rm --volume $(INGEST_MANAGER)/src:/src micktwomey/pip-tools

$(INGEST_MANAGER)/src/test_requirements.txt: $(INGEST_MANAGER)/src/test_requirements.in
	docker run --rm --volume $(INGEST_MANAGER)/src:/src micktwomey/pip-tools \
		pip-compile test_requirements.in


ingest_manager-run:
	docker run --rm --publish 8888:8888 ingest_manager
