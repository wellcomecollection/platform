ROOT = $(shell git rev-parse --show-toplevel)

lint-python:
	$(ROOT)/builds/docker_run.py -- \
		--volume $(ROOT):/data \
		--workdir /data \
		wellcome/flake8:latest \
		    --exclude .git,__pycache__,target,.terraform \
		    --ignore=E501,E122,E126

lint-js:
	$(ROOT)/builds/docker_run.py -- \
		--volume $(ROOT):/data \
		wellcome/jslint:latest

format-terraform:
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		--workdir /repo \
		hashicorp/terraform:light fmt

format-scala:
	$(ROOT)/builds/docker_run.py --sbt -- \
		--volume $(ROOT):/repo \
		wellcome/scalafmt

format-json:
	$(ROOT)/builds/docker_run.py -- \
		--volume $(ROOT):/src \
		--workdir /src \
		python:3-alpine python builds/format_json.py

format: format-terraform format-scala format-json

check-format: format lint-python lint-ontologies
	git diff --exit-code

travis-format:
	python3 .travis/run_autoformat.py
