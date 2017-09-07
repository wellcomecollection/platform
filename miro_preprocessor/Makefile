ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif


miro_preprocessor-build: miro_preprocessor-test $(ROOT)/.docker/image_builder
	PROJECT=xml_to_json_converter FILE=miro_preprocessor/xml_to_json_converter.Dockerfile $(ROOT)/./builds/build_image.sh

miro_preprocessor-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/scripts/run_docker_with_aws_credentials.sh -v $$(pwd)/xml_to_json_converter:/data -e OP=test -e FIND_MATCH_PATHS='./' python3.6_ci:latest

miro_preprocessor-run: miro_preprocessor-build
	echo "Not implemented!"

miro_preprocessor-publish:
	echo "Not implemented!"


.PHONY: miro_preprocessor-build miro_preprocessor-test miro_preprocessor-run miro_preprocessor-publish