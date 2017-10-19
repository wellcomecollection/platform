ROOT = $(shell git rev-parse --show-toplevel)
SIERRA_ADAPTER = $(ROOT)/sierra_adapter


$(SIERRA_ADAPTER)/adapter/requirements.txt: $(SIERRA_ADAPTER)/adapter/requirements.in
	docker run --volume $(SIERRA_ADAPTER)/adapter:/src micktwomey/pip-tools


sierra_adapter-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER)/terraform:/data \
		--env OP=plan \
		terraform_ci:latest

sierra_adapter-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SIERRA_ADAPTER)/terraform:/data \
		--env OP=apply \
		terraform_ci:latest


.PHONY: sierra_adapter-terraform-plan sierra_adapter-terraform-apply
