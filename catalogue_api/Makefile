ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_API = $(ROOT)/catalogue_api

include $(ROOT)/functions.Makefile

api_docs-build:
	$(call build_image,update_api_docs,catalogue_api/update_api_docs/Dockerfile)

api_docs-deploy: api_docs-build
	$(call publish_service,update_api_docs)


api-test:
	$(call sbt_test,api)

api-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=catalogue_api/api/target \
	PROJECT=api ./builds/run_sbt_image_build.sh

api-deploy: api-build
	$(call publish_service,api)


catalogue_api-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_API)/terraform)

catalogue_api-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_API)/terraform)


catalogue_api-build: api-build api_docs-build

catalogue_api-test: api-test

catalogue_api-deploy: api-deploy api_docs-deploy
