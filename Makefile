ifeq ($(OS), Windows_NT)
	MVN=mvnw
	PY=py
else
	MVN=./mvnw
	PY=python3
endif

dev: fmt test

ci: fmt-check enforce test

test:
	$(MVN) verify

enforce:
	$(MVN) dependency:analyze -DfailOnWarning=true -DignoreNonCompile=true

fmt:
	$(MVN) spotless:apply

fmt-check:
	$(MVN) spotless:check

release: validate_version
	$(MVN) versions:set -DnewVersion=$(VERSION)
	$(MVN) clean deploy
	$(MVN) versions:set -DnewVersion=0.0.1-SNAPSHOT

validate_version:
	$(PY) scripts/validate_semver.py $(VERSION)
