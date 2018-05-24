ROOT = $(shell git rev-parse --show-toplevel)
ONTOLOGIES = $(ROOT)/ontologies

lint-turtle:
	$(ROOT)/docker_run.py -- \
		--volume $(ONTOLOGIES):/data wellcome/turtlelint

lint-json:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/ontologies:/data wellcome/jslint

lint-ontologies: lint-turtle lint-json

check-reference_data:
	$(ROOT)/docker_run.py -- \
		--workdir /repo \
		--volume $(ROOT):/repo wellcome/build_tooling \
		/repo/ontologies/Reference\ data/check_reference_data.py
