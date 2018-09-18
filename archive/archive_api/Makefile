ROOT = $(shell git rev-parse --show-toplevel)
ARCHIVE_API = $(ROOT)/archive/archive_api


$(ARCHIVE_API)/src/requirements.txt: $(ARCHIVE_API)/src/requirements.in
	docker run --rm --volume $(ARCHIVE_API)/src:/src micktwomey/pip-tools

$(ARCHIVE_API)/src/test_requirements.txt: $(ARCHIVE_API)/src/test_requirements.in
	docker run --rm --volume $(ARCHIVE_API)/src:/src micktwomey/pip-tools \
		pip-compile test_requirements.in


archive_api-run:
	docker run --rm --publish 8888:8888 archive_api
