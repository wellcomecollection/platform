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

$(ROOT)/.docker/image_builder:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=image_builder \
		--dir=builds \
		--file=builds/image_builder.Dockerfile

$(ROOT)/.docker/publish_service_to_aws:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=publish_service_to_aws \
		--dir=builds \
		--file=builds/publish_service_to_aws.Dockerfile

$(ROOT)/.docker/jslint_ci:
	$(ROOT)/builds/build_ci_docker_image.py --project=jslint_ci --dir=docker/jslint_ci

$(ROOT)/.docker/python3.6_ci:
	$(ROOT)/builds/build_ci_docker_image.py --project=python3.6_ci --dir=docker/python3.6_ci

$(ROOT)/.docker/terraform_ci:
	$(ROOT)/builds/build_ci_docker_image.py --project=terraform_ci --dir=docker/terraform_ci

$(ROOT)/.docker/_build_deps:
	pip3 install --upgrade boto3 docopt
	mkdir -p $(ROOT)/.docker && touch $(ROOT)/.docker/_build_deps

$(ROOT)/.docker/miro_adapter_tests:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=miro_adapter_tests \
		--dir=miro_adapter \
		--file=miro_adapter/miro_adapter_tests.Dockerfile



# Project utility tasks

## Run flake8 linting over the current directory
lint-python: $(ROOT)/.docker/python3.6_ci
	docker run -v $$(pwd):/data -e OP=lint python3.6_ci:latest

## Run JSON linting over the current directory
lint-js: $(ROOT)/.docker/jslint_ci
	docker run -v $$(pwd):/data jslint_ci:latest

## Check a git repo is up to date with remote master
uptodate-git: $(ROOT)/.docker/python3.6_ci
	docker run -v $$HOME/.ssh:/root/.ssh -v $(ROOT):/data -e OP=is-master-head python3.6_ci:latest

## Format terraform in the current directory
format-terraform: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $$(pwd):/data \
		--env OP=fmt terraform_ci

## Format scala in the current directory
format-scala:
	sbt scalafmt


clean:
	rm -rf $(ROOT)/.docker

.PHONY: lint-python lint-js uptodate-git format-terraform format-scala
