ROOT = $(shell git rev-parse --show-toplevel)


define terraform_plan
	make uptodate-git
	make $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(1):/data \
		--env OP=plan \
		terraform_ci:latest
endef

define terraform_apply
	make uptodate-git
	make $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER)/terraform:/data \
		--env OP=apply \
		terraform_ci:latest
endef
