ROOT = $$(git rev-parse --show-toplevel)

build-autogen:
	cd $(ROOT)/autogen && docker build --tag autogen .

run-autogen: build-autogen
	docker run -v $(ROOT):/repo autogen

check-autogen: run-autogen
	git diff --exit-code
