DOCKER_IMG_BUILD_TOOLING = wellcome/build_tooling
DOCKER_IMG_FLAKE8        = wellcome/flake8:latest
DOCKER_IMG_FORMAT_JSON   = wellcome/format_json:latest
DOCKER_IMG_FORMAT_PYTHON = wellcome/format_python
DOCKER_IMG_JSLINT        = wellcome/jslint:latest
DOCKER_IMG_SCALAFMT      = wellcome/scalafmt
DOCKER_IMG_TERRAFORM     = hashicorp/terraform:light


lint-python:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/data \
		--workdir /data \
		$(DOCKER_IMG_FLAKE8) \
		    --exclude .git,__pycache__,target,.terraform \
		    --ignore=E501,E122,E126,E203,W503

lint-js:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/data \
		$(DOCKER_IMG_JSLINT)

format-rfcs:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/data \
		$(DOCKER_IMG_BUILD_TOOLING) \
		python3 /data/fix_rfc_headers.py

format-terraform:
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		--workdir /repo \
		$(DOCKER_IMG_TERRAFORM) fmt

format-python:
	$(ROOT)/docker_run.py -- --volume $(ROOT):/repo $(DOCKER_IMG_FORMAT_PYTHON)

format-scala:
	$(ROOT)/docker_run.py --sbt -- --volume $(ROOT):/repo $(DOCKER_IMG_SCALAFMT)

format-json:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/src \
		--workdir /src \
		$(DOCKER_IMG_FORMAT_JSON)

format: format-terraform format-scala format-python format-json

check-format: format lint-python lint-ontologies
	git diff --exit-code

travis-format:
	python3 $(ROOT)/run_autoformat.py
