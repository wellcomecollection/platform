ROOT = $(shell git rev-parse --show-toplevel)
SIERRA_WINDOW_GENERATOR = $(ROOT)/sierra_adapter/sierra_window_generator

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(SIERRA_WINDOW_GENERATOR)/requirements.txt: $(SIERRA_WINDOW_GENERATOR)/requirements.in
	docker run --volume $(SIERRA_WINDOW_GENERATOR):/src micktwomey/pip-tools
