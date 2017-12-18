ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_API = $(ROOT)/catalogue_api

include $(ROOT)/functions.Makefile


$(ROOT)/.docker/switch_api_pins: $(CATALOGUE_API)/terraform/switch_api_pins.Dockerfile
	docker build --tag switch_api_pins --file $(CATALOGUE_API)/terraform/switch_api_pins.Dockerfile $(CATALOGUE_API)/terraform
	mkdir -p $(ROOT)/.docker
	touch $(ROOT)/.docker/switch_api_pins

switch_api_pins: $(ROOT)/.docker/switch_api_pins
	$(ROOT)/builds/docker_run.py --aws -- \
		 --volume $(CATALOGUE_API)/terraform:/terraform switch_api_pins


api_docs-build:
	$(call build_image,update_api_docs,catalogue_api/update_api_docs/Dockerfile)

api_docs-publish: api_docs-build
	$(call publish_service,update_api_docs)


api-test:
	$(call sbt_test,api)

api-build:
	$(call sbt_build,api)
	$(call build_image,api,catalogue_api/api/Dockerfile)

api-publish: api-build
	$(call publish_service,api)


catalogue_api-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_API)/terraform,true)

catalogue_api-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_API)/terraform)


catalogue_api-build: api-build api_docs-build

catalogue_api-test: api-test

catalogue_api-publish: api-publish api_docs-publish
