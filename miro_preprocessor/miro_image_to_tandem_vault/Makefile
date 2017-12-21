ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

miro_tandem_vault_uploader-build:
	$(call build_image,tandem_vault_uploader,miro_preprocessor/miro_image_to_tandem_vault/tandem_vault_uploader.Dockerfile)

miro_image_to_tandem_vault-test:
	$(call test_lambda,miro_preprocessor/miro_image_to_tandem_vault)

miro_tandem_vault_uploader-publish: miro_tandem_vault_uploader-build
	$(call publish_service,tandem_vault_uploader)

miro_tandem_vault_enrichment-build:
	$(call build_image,tandem_vault_enrichment,miro_preprocessor/miro_image_to_tandem_vault/tandem_vault_enrichment.Dockerfile)

miro_tandem_vault_enrichment-publish: miro_tandem_vault_enrichment-build
	$(call publish_service,tandem_vault_enrichment)
