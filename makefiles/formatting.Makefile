ROOT = $(shell git rev-parse --show-toplevel)

lint-python:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/data \
		--workdir /data \
		wellcome/flake8:latest \
		    --exclude .git,__pycache__,target,.terraform \
		    --ignore=E501,E122,E126,E203,W503

format-terraform:
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		--workdir /repo \
		hashicorp/terraform:light fmt

format-python:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/repo \
		wellcome/format_python

format-scala:
	$(ROOT)/docker_run.py --sbt -- \
		--volume $(ROOT):/repo \
		wellcome/scalafmt

format: format-terraform format-scala format-python

lint: lint-python
	git diff --exit-code

travis-format:
	python3 run_autoformat.py
