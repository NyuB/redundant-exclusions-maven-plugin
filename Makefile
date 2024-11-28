ifeq ($(OS), Windows_NT)
	MVN=mvnw
else
	MVN=./mvnw
endif

dev: fmt test

ci: fmt-check test

test:
	$(MVN) clean verify

enforce:
	$(MVN) dependency:analyze -DfailOnWarning=true -DignoreNonCompile=true

fmt:
	$(MVN) spotless:apply

fmt-check:
	$(MVN) spotless:check