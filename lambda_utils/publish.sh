#!/usr/bin/env sh

set -o errexit
set -o nounset

if [ "$(PUBLISH_VERSION)" = "$(SETUP_VERSION)" ]
then
	echo "*** Uploaded package is up-to-date, nothing to do"
else
	echo "*** Newer version available, uploading new package"
	docker run                                                                  \
		--volume $(LAMBDA_UTILS):/src                                             \
		--workdir /src                                                            \
		--env TWINE_USERNAME="$(PYPI_USERNAME)"                                   \
		--env TWINE_PASSWORD="$(PYPI_PASSWORD)"                                   \
		greengloves/twine:1.9.1 upload dist/*.tar.gz
fi
