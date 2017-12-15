ROOT = $(shell git rev-parse --show-toplevel)
MIRO_PREPROC = $(ROOT)/miro_preprocessor

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

include $(MIRO_PREPROC)/xml_to_json_converter/Makefile
include $(MIRO_PREPROC)/image_sorter/Makefile
include $(MIRO_PREPROC)/xml_to_json_run_task/Makefile
include $(MIRO_PREPROC)/miro_image_to_dynamo/Makefile
include $(MIRO_PREPROC)/miro_image_to_tandem_vault/Makefile
include $(MIRO_PREPROC)/miro_copy_s3_asset/Makefile
include $(MIRO_PREPROC)/miro_inventory/Makefile

miro_preprocessor-build: xml_to_json_converter-build \
						 xml_to_json_run_task-build \
						 image_sorter-build \
						 miro_image_to_dynamo-build \
						 miro_copy_s3_asset-build \
						 miro_inventory-build \
						 miro_image_to_tandem_vault-build

miro_preprocessor-test: xml_to_json_converter-test \
						xml_to_json_run_task-test \
						image_sorter-test \
						miro_image_to_dynamo-test \
						miro_copy_s3_asset-test \
						miro_inventory-test \
						miro_image_to_tandem_vault-test

miro_preprocessor-publish: xml_to_json_converter-publish \
						   xml_to_json_run_task-publish \
						   image_sorter-publish \
						   miro_image_to_dynamo-publish \
						   miro_copy_s3_asset-publish \
						   miro_inventory-publish \
						   miro_image_to_tandem_vault-publish

miro_preprocessor-terraform-plan:
	$(call terraform_plan,$(MIRO_PREPROC),false)

miro_preprocessor-terraform-apply:
	$(call terraform_apply,$(MIRO_PREPROC))
