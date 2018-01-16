ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= miro_preprocessor

SBT_APPS 	=
ECS_TASKS 	= xml_to_json_converter
LAMBDAS 	= image_sorter \
				miro_copy_s3_master_asset \
				miro_copy_s3_derivative_asset \
				miro_image_to_dynamo \
				miro_inventory \
				xml_to_json_run_task

TF_NAME 	= miro_preprocessor
TF_PATH 	= $(ROOT)/$(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))


xml_to_json_converter-test: $(ROOT)/.docker/python3.6_ci
	docker run --rm --tty \
		--volume $(ROOT)/miro_preprocessor/xml_to_json_converter/src:/data \
		--env OP=test \
		python3.6_ci:latest

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
