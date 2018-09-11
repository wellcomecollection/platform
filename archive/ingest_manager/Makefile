ROOT = $(shell git rev-parse --show-toplevel)

INGEST_MANAGER = $(ROOT)/archive/ingest_manager


$(INGEST_MANAGER)/requirements.txt: $(INGEST_MANAGER)/requirements.in
	docker run --rm --volume $(INGEST_MANAGER):/src micktwomey/pip-tools

ingest_manager-run:
	docker run --rm --publish 5000:5000 ingest_manager
