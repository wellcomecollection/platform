ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

miro_tandem_vault_uploader-build:
	$(call build_image,tandem_vault_uploader,miro_preprocessor/miro_image_to_tandem_vault/tandem_vault_uploader.Dockerfile)

miro_image_to_tandem_vault-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_image_to_tandem_vault/src:/data \
		--env OP=test \
		python3.6_ci:latest

miro_tandem_vault_uploader-publish: miro_tandem_vault_uploader-build
	$(call publish_service,tandem_vault_uploader)

miro_tandem_vault_enrichment-build:
	$(call build_image,tandem_vault_enrichment,miro_preprocessor/miro_image_to_tandem_vault/tandem_vault_enrichment.Dockerfile)

miro_tandem_vault_enrichment-publish: miro_tandem_vault_enrichment-build $(ROOT)/.docker/publish_service_to_aws
	$(call publish_service,tandem_vault_enrichment)
