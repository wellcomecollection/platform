ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif


.docker/_lambda_deps: $(ROOT)/.docker/python3.6_ci
	docker run -v $$(pwd)/lambdas:/data -e OP=install-deps python3.6_ci:latest

## Run tests for our Lambda code
lambdas-test: .docker/python3.6_ci
	./scripts/run_docker_with_aws_credentials.sh \
		-v $$(pwd)/lambdas:/data \
		-e OP=test \
		-e FIND_MATCH_PATHS='./*/target common/tests' \
		python3.6_ci:latest

## Run a plan on lambda stack
terraform-lambda-plan: uptodate-git .docker/terraform_ci .docker/_lambda_deps
	./scripts/run_docker_with_aws_credentials.sh \
	-v $$(pwd)/terraform:/terraform \
	-v $$(pwd)/lambdas:/data \
	-e OP=plan \
	terraform_ci:latest

## Run an apply on lambda stack
terraform-lambda-apply: .docker/terraform_ci
	./scripts/run_docker_with_aws_credentials.sh \
	-v $$(pwd)/terraform:/terraform \
	-v $$(pwd)/lambdas:/data \
	-e OP=apply \
	terraform_ci:latest


.PHONY: lambdas-test terraform-lambda-plan terraform-lambda-apply
