ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif

xml_to_json_converter-build: xml_to_json_converter-test $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		-v $(ROOT):/repo \
		image_builder:latest \
		--project=xml_to_json_converter \
		--file=miro_preprocessor/xml_to_json_converter/Dockerfile

xml_to_json_converter-test: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/miro_preprocessor/xml_to_json_converter/src:/data \
		-e OP=test \
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
		-v $(ROOT)/miro_preprocessor/xml_to_json_run_task/src:/data \
		-e OP=test \
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

miro_copy_s3_asset-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
	-v $(ROOT)/miro_preprocessor/miro_copy_s3_asset/src:/data \
	-e OP=test \
	python3.6_ci:latest

miro_copy_s3_asset-build: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
	-v $(ROOT)/miro_preprocessor/miro_copy_s3_asset:/data \
	-e OP=build-lambda \
	python3.6_ci:latest


image_sorter-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ROOT)/miro_preprocessor/image_sorter:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

image_sorter-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh \
		--volume $(ROOT)/miro_preprocessor/image_sorter/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" \
		python3.6_ci:latest


miro_preprocessor-test: xml_to_json_converter-test xml_to_json_run_task-test miro_image_to_dynamo-test image_sorter-test miro_copy_s3_asset-test

miro_preprocessor-build-lambdas: xml_to_json_run_task-build miro_image_to_dynamo-build image_sorter-build miro_copy_s3_asset-build

miro_preprocessor-build: xml_to_json_converter-build miro_preprocessor-build-lambdas

miro_preprocessor-publish: xml_to_json_converter-publish


miro_preprocessor-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci miro_preprocessor-build-lambdas
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