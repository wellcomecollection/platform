ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

xml_to_json_converter-build:
	$(call build_image,xml_to_json_converter,miro_preprocessor/xml_to_json_converter/Dockerfile)

xml_to_json_converter-test: $(ROOT)/.docker/python3.6_ci
	docker run --rm --tty \
		--volume $(ROOT)/miro_preprocessor/xml_to_json_converter/src:/data \
		--env OP=test \
		python3.6_ci:latest

xml_to_json_converter-publish: xml_to_json_converter-build
	$(call publish_service,xml_to_json_converter)
