export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

update_task_for_config_change-build:
	$(call build_lambda,shared_infra/update_task_for_config_change)

update_task_for_config_change-test:
	$(call test_lambda,shared_infra/update_task_for_config_change)

update_task_for_config_change-publish:
	$(call publish_lambda,shared_infra/update_task_for_config_change)
