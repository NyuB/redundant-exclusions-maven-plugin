dev: fmt test

ci: fmt-check enforce test

test:
	mvn clean verify

enforce:
	mvn dependency:analyze -DfailOnWarning=true -DignoreNonCompile=true

fmt:
	mvn spotless:apply

fmt-check:
	mvn spotless:check