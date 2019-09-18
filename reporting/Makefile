ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT 	= reporting

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    = reporting_miro_transformer \
                  reporting_miro_inventory_transformer \
                  reporting_sierra_transformer \
                  reporting_calm_transformer

$(val $(call stack_setup))


reporting-test: \
    reporting_miro_transformer-test \
    reporting_miro_inventory_transformer-test \
    reporting_sierra_transformer-test \
    reporting_calm_transformer-test

reporting-publish: \
    reporting_miro_transformer-publish \
    reporting_miro_inventory_transformer-publish \
    reporting_sierra_transformer-publish \
    reporting_calm_transformer-publish