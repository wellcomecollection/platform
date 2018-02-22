ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= loris

SBT_APPS 	=
ECS_TASKS 	= loris cache_cleaner
LAMBDAS 	=

TF_NAME 	= loris
TF_PATH 	= loris/terraform
TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))


# TODO: Flip this to using micktwomey/pip-tools when that's updated
# with a newer version of pip-tools.
$(LORIS)/loris/requirements.txt: $(LORIS)/requirements.in
	docker run --rm \
		-v $(LORIS):/data \
		wellcome/build_tooling:latest \
		pip-compile

loris-run: loris-build
	$(ROOT)/.scripts/docker_run.py --aws -- \
		--publish 8888:8888 \
		--env INFRA_BUCKET=$(INFRA_BUCKET) \
		--env CONFIG_KEY=config/prod/loris.ini \
		loris
