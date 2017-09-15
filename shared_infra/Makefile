ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


## Run a plan on main stack
terraform-shared-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
		-v $(ROOT)/terraform:/terraform \
		-v $(ROOT)/shared_infra:/data \
		-e OP=plan \
		terraform_ci:latest

## Run an apply on main stack
terraform-shared-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
		-v $(ROOT)/terraform:/terraform \
		-v $(ROOT)/shared_infra:/data \
		-e OP=apply \
		terraform_ci:latest


.PHONY: terraform-shared-plan terraform-shared-apply
