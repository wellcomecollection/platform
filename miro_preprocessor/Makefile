ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
include $(ROOT)/shared.Makefile
endif


miro_preprocessor-build: miro_preprocessor-test $(ROOT)/.docker/image_builder
	PROJECT=xml_to_json_converter FILE=miro_preprocessor/xml_to_json_converter.Dockerfile $(ROOT)/./builds/build_image.sh

miro_preprocessor-test:
	echo "Not implemented!"

miro_preprocessor-run: miro_preprocessor-build
	echo "Not implemented!"

miro_preprocessor-publish:
	echo "Not implemented!"


.PHONY: miro_preprocessor-build miro_preprocessor-run miro_preprocessor-publish