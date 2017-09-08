ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif


xml_to_json_converter-build: xml_to_json_converter-test $(ROOT)/.docker/image_builder
	PROJECT=xml_to_json_converter FILE=miro_preprocessor/xml_to_json_converter.Dockerfile $(ROOT)/./builds/build_image.sh

xml_to_json_converter-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh -v $$(pwd)/xml_to_json_converter:/data -e OP=test -e FIND_MATCH_PATHS='./' python3.6_ci:latest

xml_to_json_converter-publish: xml_to_json_converter-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=xml_to_json_converter $(ROOT)/builds/publish_service.sh

miro_preprocessor-test: xml_to_json_converter-test

miro_preprocessor-build: xml_to_json_converter-build

miro_preprocessor-publish: xml_to_json_converter-publish

miro_preprocessor-terraform-plan: $(ROOT)/.docker/terraform_ci
	docker run -v $(ROOT)/miro_preprocessor:/data -v $(ROOT)/terraform:/terraform -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=plan terraform_ci:latest

miro_preprocessor-terraform-apply: $(ROOT)/.docker/terraform_ci
	docker run -v $(ROOT)/miro_preprocessor:/data -v $(ROOT)/terraform:/terraform -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=apply terraform_ci:latest

miro_preprocessor-run:
	echo "Not implemented!"


.PHONY: miro_preprocessor-build miro_preprocessor-test miro_preprocessor-run miro_preprocessor-publish