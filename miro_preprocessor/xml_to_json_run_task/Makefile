ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

xml_to_json_run_task-test:
	$(call test_lambda,miro_preprocessor/xml_to_json_run_task)

xml_to_json_run_task-publish:
	$(call publish_lambda,miro_preprocessor/xml_to_json_run_task)
