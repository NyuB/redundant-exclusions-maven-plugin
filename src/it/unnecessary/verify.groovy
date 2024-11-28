String buildLog = new File(basedir, "build.log").text
assert buildLog.contains("[ERROR]")
assert buildLog.contains("httpcore5-h2")
assert buildLog.contains("is not a dependency of com.google.guava:guava:33.2.1-jre")