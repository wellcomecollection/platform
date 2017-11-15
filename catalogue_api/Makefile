ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_API = $(ROOT)/catalogue_api
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile


catalogue_api-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_API)/terraform)

## Run an apply on main stack
catalogue_api-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_API)/terraform)


.PHONY: catalogue_api-terraform-plan catalogue_api-terraform-apply
