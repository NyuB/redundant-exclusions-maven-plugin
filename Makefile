dev: fmt test

test:
	mvn verify

fmt:
	mvn spotless:apply

fmt-check:
	mvn spotless:check