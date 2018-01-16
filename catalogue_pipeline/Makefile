ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

PROJECTS = id_minter ingestor reindexer transformer
PROJECT_PATH = catalogue_pipeline

LAMBDAS = schedule_reindexer

TF_NAME = catalogue_pipeline
TF_PATH = $(ROOT)/catalogue_pipeline/terraform
TF_IS_PUBLIC_FACING = false

$(foreach proj,$(PROJECTS),$(eval $(call scala_target_template,$(proj),$(PROJECT_PATH)/$(proj))))
$(foreach lamb,$(LAMBDAS),$(eval $(call lambda_target_template,$(lamb),$(PROJECT_PATH)/$(lamb))))
$(eval $(call terraform_target_template,$(TF_NAME),$(TF_PATH),$(TF_IS_PUBLIC_FACING)))
