ROOT = $(shell git rev-parse --show-toplevel)
ONTOLOGIES = $(ROOT)/ontologies

lint-turtle:
	docker run \
		--rm \
		--volume $(ONTOLOGIES):/data \
		--tty \
		wellcome/turtlelint:latest

lint-json:
	docker run \
	--volume $(ROOT)/ontologies:/data \
	wellcome/jslint:latest

lint-ontologies: lint-turtle lint-json
