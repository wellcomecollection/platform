ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

xml_to_json_run_task-build: $(ROOT)/.docker/python3.6_ci
	$(call build_lambda,miro_preprocessor/xml_to_json_run_task)

xml_to_json_run_task-test: $(ROOT)/.docker/python3.6_ci
	$(call test_lambda,miro_preprocessor/xml_to_json_run_task)

xml_to_json_run_task-publish:
	$(call publish_lambda,miro_preprocessor/xml_to_json_run_task)
