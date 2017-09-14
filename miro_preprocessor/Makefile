ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif

xml_to_json_converter-build: xml_to_json_converter-test $(ROOT)/.docker/image_builder
	$(ROOT)/scripts/run_docker_in_docker.sh \
		-v $(ROOT):/repo \
		image_builder:latest \
		--project=xml_to_json_converter \
		--file=miro_preprocessor/xml_to_json_converter/Dockerfile

xml_to_json_converter-test: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/miro_preprocessor/xml_to_json_converter/src:/data \
		-e OP=test -e FIND_MATCH_PATHS='/data' \
		python3.6_ci:latest

xml_to_json_converter-publish: xml_to_json_converter-build $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
	    -v /var/run/docker.sock:/var/run/docker.sock \
		-v $(ROOT):/repo \
		publish_service_to_aws:latest \
		--project=xml_to_json_converter \
		--infra-bucket=platform-infra

xml_to_json_run_task-test: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/miro_preprocessor/xml_to_json_run_task:/data \
		-e OP=test -e FIND_MATCH_PATHS='/data/src' \
		python3.6_ci:latest

xml_to_json_run_task-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/miro_preprocessor/xml_to_json_run_task:/data \
		-e OP=build-lambda \
		python3.6_ci:latest

miro_image_to_dynamo-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
	-v $(ROOT)/miro_preprocessor/miro_image_to_dynamo/src:/data \
	-e OP=test -e FIND_MATCH_PATHS='/data' \
	python3.6_ci:latest

miro_image_to_dynamo-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/miro_preprocessor/miro_image_to_dynamo:/data \
		-e OP=build-lambda \
		python3.6_ci:latest

miro_preprocessor-test: xml_to_json_converter-test xml_to_json_run_task-test miro_image_to_dynamo-test

miro_preprocessor-build: xml_to_json_converter-build xml_to_json_run_task-build miro_image_to_dynamo-build

miro_preprocessor-publish: xml_to_json_converter-publish

miro_preprocessor-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci xml_to_json_run_task-build miro_image_to_dynamo-build
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
		-v $(ROOT)/miro_preprocessor:/data \
		-v $(ROOT)/terraform:/terraform \
		-e OP=plan \
		terraform_ci:latest

miro_preprocessor-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
	-v $(ROOT)/miro_preprocessor:/data \
	-v $(ROOT)/terraform:/terraform \
	-e OP=apply \
	terraform_ci:latest

miro_preprocessor-run:
	$(error Not implemented!)


.PHONY: miro_preprocessor-build miro_preprocessor-test miro_preprocessor-run miro_preprocessor-publish