export ROOT = $(shell git rev-parse --show-toplevel)
export LAMBDAS = $(ROOT)/lambdas
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


.docker/_lambda_deps: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(LAMBDAS):/data \
		-e OP=install-deps \
		python3.6_ci:latest

## Run tests for our Lambda code
lambdas-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		-v $(LAMBDAS):/data \
		-e OP=test \
		-e FIND_MATCH_PATHS='./*/target common/tests' \
		python3.6_ci:latest

lambdas-publish: $(ROOT)/.docker/publish_lambda_zip
	$(LAMBDAS)/publish_all_lambdas.sh

## Run a plan on lambda stack
lambdas-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci .docker/_lambda_deps
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/terraform:/terraform \
	-v $(LAMBDAS):/data \
	-e OP=plan \
	terraform_ci:latest

## Run an apply on lambda stack
lambdas-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/terraform:/terraform \
	-v $(LAMBDAS):/data \
	-e OP=apply \
	terraform_ci:latest


.PHONY: lambdas-test lambdas-terraform--plan lambdas-terraform-apply
