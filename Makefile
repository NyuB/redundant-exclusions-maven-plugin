dev: fmt test

test:
	mvn test

fmt:
	mvn spotless:apply

fmt-check:
	mvn spotless:check