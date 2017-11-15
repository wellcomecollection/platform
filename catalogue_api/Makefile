ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_API = $(ROOT)/catalogue_api

include $(ROOT)/functions.Makefile


catalogue_api-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_API)/terraform)

catalogue_api-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_API)/terraform)
