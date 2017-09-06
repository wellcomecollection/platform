ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/builds.Makefile
endif


loris-build: $(ROOT)/.docker/image_builder
	PROJECT=loris FILE=loris/Dockerfile $(ROOT)/builds/build_image.sh

loris-publish: loris-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=loris $(ROOT)/builds/publish_service.sh


.PHONY: loris-build loris-publish
