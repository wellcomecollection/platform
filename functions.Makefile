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
		--volume $(1):/data \
		--env OP=apply \
		terraform_ci:latest
endef


define publish_lambda
	make $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip \
		"$(1)/$(2)/src" --key="lambdas/$(1)/$(2).zip" --bucket="$(INFRA_BUCKET)"
endef
