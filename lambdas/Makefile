ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


.docker/_lambda_deps: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/lambdas:/data \
		-e OP=install-deps \
		python3.6_ci:latest

## Run tests for our Lambda code
lambdas-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		-v $(ROOT)/lambdas:/data \
		-e OP=test \
		-e FIND_MATCH_PATHS='./*/target common/tests' \
		python3.6_ci:latest

## Run a plan on lambda stack
lambdas-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci .docker/_lambda_deps
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/terraform:/terraform \
	-v $(ROOT)/lambdas:/data \
	-e OP=plan \
	terraform_ci:latest

## Run an apply on lambda stack
lambdas-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/terraform:/terraform \
	-v $(ROOT)/lambdas:/data \
	-e OP=apply \
	terraform_ci:latest


.PHONY: lambdas-test lambdas-terraform--plan lambdas-terraform-apply
