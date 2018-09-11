ROOT = $(shell git rev-parse --show-toplevel)

INGEST_MANAGER = $(ROOT)/archive/ingest_manager


$(INGEST_MANAGER)/requirements.txt: $(INGEST_MANAGER)/requirements.in
	docker run --rm --volume $(INGEST_MANAGER):/src micktwomey/pip-tools

$(INGEST_MANAGER)/test_requirements.txt: $(INGEST_MANAGER)/test_requirements.in
	docker run --rm --volume $(INGEST_MANAGER):/src micktwomey/pip-tools \
		pip-compile test_requirements.in


$(ROOT)/.docker/ingest_manager_tests: $(INGEST_MANAGER)/test.Dockerfile $(INGEST_MANAGER)/test_requirements.txt
	docker build -f $(INGEST_MANAGER)/test.Dockerfile -t ingest_manager_tests $(INGEST_MANAGER)
	mkdir -p $(ROOT)/.docker
	touch $(ROOT)/.docker/ingest_manager_tests

ingest_manager-test: $(ROOT)/.docker/ingest_manager_tests
	docker run --rm --volume $(INGEST_MANAGER)/src:/src ingest_manager_tests


ingest_manager-run:
	docker run --rm --publish 5000:5000 ingest_manager
