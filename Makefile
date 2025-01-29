ifeq ($(OS), Windows_NT)
	MVN=mvnw
	PY=py
else
	MVN=./mvnw
	PY=python3
endif

dev: fmt test

ci: fmt-check enforce test validate_documentation

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
	$(PY) ci/validate_semver.py $(VERSION)

validate_documentation: ci/ydoc.jar
	java -jar ci/ydoc.jar check README.md

YDOC_VERSION=0.5.0
ci/ydoc.jar:
	curl -L -o ci/ydoc.jar "https://github.com/NyuB/yadladoc/releases/download/$(YDOC_VERSION)/ydoc.jar"
