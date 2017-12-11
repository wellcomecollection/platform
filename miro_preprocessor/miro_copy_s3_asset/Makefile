ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile


miro_copy_s3_master_asset-build:
	$(call build_lambda,miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_master_asset)

miro_copy_s3_master_asset-test:
	$(call test_lambda,miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_master_asset)

miro_copy_s3_master_asset-publish:
	$(call publish_lambda,miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_master_asset)


miro_copy_s3_derivative_asset-build:
	$(call build_lambda,miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_derivative_asset)

miro_copy_s3_derivative_asset-test:
	$(call test_lambda,miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_derivative_asset)

miro_copy_s3_derivative_asset-publish:
	$(call publish_lambda,miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_derivative_asset)


miro_copy_s3_asset-build: miro_copy_s3_master_asset-build miro_copy_s3_derivative_asset-build

miro_copy_s3_asset-test: miro_copy_s3_derivative_asset-test miro_copy_s3_master_asset-test

miro_copy_s3_asset-publish: miro_copy_s3_master_asset-publish miro_copy_s3_derivative_asset-publish
