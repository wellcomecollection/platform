# This Makefile contains tasks and underlying pieces that are shared
# across our other Makefiles.
#
# Build the Docker images used for CI tasks.
#
# The script sticks a record that the image has been built in .docker, so the
# image isn't rebuilt unless you run 'make clean' first.  This makes CI tasks
# slightly less chatty when run locally.

export INFRA_BUCKET = platform-infra

ROOT = $(shell git rev-parse --show-toplevel)

CURRENT_DIR = $(shell pwd)

include $(ROOT)/builds/Makefile

$(ROOT)/.docker/publish_service_to_aws:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=publish_service_to_aws \
		--dir=builds \
		--file=builds/publish_service_to_aws.Dockerfile

$(ROOT)/.docker/_build_deps:
	pip3 install --upgrade boto3 docopt
	mkdir -p $(ROOT)/.docker && touch $(ROOT)/.docker/_build_deps

$(ROOT)/.docker/miro_adapter_tests:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=miro_adapter_tests \
		--dir=miro_adapter \
		--file=miro_adapter/miro_adapter_tests.Dockerfile


# Project utility tasks

## Run Python linting over the current directory
lint-python:
	docker run \
		--volume $(CURRENT_DIR):/data \
		--workdir /data \
		wellcome/flake8:latest --exclude target --ignore=E501

## Run JSON linting over the current directory
lint-js:
	docker run \
	--volume $(CURRENT_DIR):/data \
	wellcome/jslint:latest

## Check a git repo is up to date with remote master
uptodate-git: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/is_up_to_date_with_master.sh

format-terraform:
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		--workdir /repo \
		hashicorp/terraform:light fmt

format-scala:
	$(ROOT)/builds/docker_run.py --sbt -- \
		--volume $(ROOT):/repo \
		wellcome/scalafmt


clean:
	rm -rf $(ROOT)/.docker

.PHONY: lint-python lint-js uptodate-git format-terraform format-scala
