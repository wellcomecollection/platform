ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

SIERRA_ADAPTER = $(ROOT)/sierra_adapter

include $(SIERRA_ADAPTER)/sierra_window_generator/Makefile
include $(ROOT)/functions.Makefile

sierra_to_dynamo-test: $(ROOT)/.docker/sbt_test
	PROJECT=sierra_to_dynamo $(ROOT)/builds/test_sbt_project.sh

sierra_to_dynamo-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=sierra_adapter/sierra_to_dynamo/target \
	PROJECT=sierra_to_dynamo $(ROOT)/builds/run_sbt_image_build.sh

sierra_to_dynamo-publish: sierra_to_dynamo-build
	$(call publish_service,sierra_to_dynamo)

sierra_adapter-test: sierra_window_generator-test sierra_to_dynamo-test

sierra_adapter-publish: sierra_window_generator-publish sierra_to_dynamo-publish


sierra_adapter-terraform-plan:
	$(call terraform_plan,$(SIERRA_ADAPTER)/terraform)

sierra_adapter-terraform-apply:
	$(call terraform_apply,$(SIERRA_ADAPTER)/terraform)
