ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_API = $(ROOT)/catalogue_api

include $(ROOT)/functions.Makefile

api_docs-build:
	$(call build_image,update_api_docs,catalogue_api/update_api_docs/Dockerfile)

api_docs-deploy: api_docs-build
	$(call publish_service,update_api_docs)

catalogue_api-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_API)/terraform)

catalogue_api-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_API)/terraform)
