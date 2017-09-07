ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif


## Run a plan on main stack
terraform-shared-plan: uptodate-git .docker/terraform_ci
	docker run -v $$(pwd)/terraform:/terraform -v $$(pwd)/shared_infra:/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=plan terraform_ci:latest

## Run an apply on main stack
terraform-shared-apply: .docker/terraform_ci
	docker run -v $$(pwd)/terraform:/terraform -v $$(pwd)/shared_infra:/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=apply terraform_ci:latest


.PHONY: terraform-shared-plan terraform-shared-apply
