ROOT = $(shell git rev-parse --show-toplevel)
LORIS = $(ROOT)/loris
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif
include $(ROOT)/functions.Makefile

# TODO: Flip this to using micktwomey/pip-tools when that's updated
# with a newer version of pip-tools.
$(LORIS)/requirements.txt: $(LORIS)/requirements.in
	docker run --rm \
		-v $(LORIS):/data \
		wellcome/build_tooling:latest \
		pip-compile

loris-build:
	$(call build_image,loris,loris/Dockerfile)

loris-run: loris-build
	$(ROOT)/builds/docker_run.py --aws -- \
		--publish 8888:8888 \
		--env INFRA_BUCKET=$(INFRA_BUCKET) \
		--env CONFIG_KEY=config/prod/loris.ini \
		loris

loris-publish: loris-build
	$(call publish_service,loris)

loris-terraform-plan:
	$(call terraform_plan,$(LORIS)/terraform,true)

loris-terraform-apply:
	$(call terraform_apply,$(LORIS)/terraform)


cache_cleaner-build:
	$(call build_image,cache_cleaner,loris/cache_cleaner/Dockerfile)

cache_cleaner-publish: cache_cleaner-build
	$(call publish_service,cache_cleaner)
