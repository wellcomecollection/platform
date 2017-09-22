ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


## Run a plan on main stack
shared-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		-v $(ROOT)/terraform:/terraform \
		-v $(ROOT)/shared_infra:/data \
		-e OP=plan \
		terraform_ci:latest

## Run an apply on main stack
shared-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		-v $(ROOT)/terraform:/terraform \
		-v $(ROOT)/shared_infra:/data \
		-e OP=apply \
		terraform_ci:latest


.PHONY: shared-terraform-plan shared-terraform-apply
