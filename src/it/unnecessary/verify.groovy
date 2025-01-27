String buildLog = new File(basedir, "build.log").text
assert buildLog.contains("[ERROR]")
assert buildLog.contains("BUILD FAILURE")
assert buildLog.contains("org.apache.httpcomponents.core5:httpcore5-h2 is excluded from com.google.guava:guava:33.2.1-jre:jar but is not one of its dependency")