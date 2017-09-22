ROOT = $(shell git rev-parse --show-toplevel)
MONITORING = $(ROOT)/monitoring

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

monitoring-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/terraform:/data \
		--volume $(ROOT)/terraform:/terraform \
		--env OP=plan \
		terraform_ci:latest

monitoring-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/terraform:/data \
		--volume $(ROOT)/terraform:/terraform \
		--env OP=apply \
		terraform_ci:latest

.PHONY: monitoring-terraform-plan monitoring-terraform-apply
