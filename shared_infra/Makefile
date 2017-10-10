export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


.docker/_lambda_deps: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(SHARED_INFRA)/lambdas:/data \
		-e OP=install-deps \
		python3.6_ci:latest

## Run tests for our Lambda code
shared_infra-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		-v $(SHARED_INFRA)/lambdas:/data \
		-e OP=test \
		-e FIND_MATCH_PATHS='./*/target common/tests' \
		python3.6_ci:latest

shared_infra-lambdas-publish: $(ROOT)/.docker/publish_lambda_zip
	$(SHARED_INFRA)/lambdas/publish_all_lambdas.sh


## Run a plan on lambda stack
shared_infra-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci .docker/_lambda_deps
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(SHARED_INFRA)/terraform:/data \
	-v $(SHARED_INFRA)/lambdas:/lambdas \
	-e OP=plan \
	terraform_ci:latest

## Run an apply on lambda stack
shared_infra-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(SHARED_INFRA)/terraform:/data \
	-v $(SHARED_INFRA)/lambdas:/lambdas \
	-e OP=apply \
	terraform_ci:latest


.PHONY: lambdas-publish lambdas-test lambdas-terraform--plan lambdas-terraform-apply