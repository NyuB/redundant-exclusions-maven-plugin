String buildLog = new File(basedir, "build.log").text
assert buildLog.contains("[ERROR]")
assert buildLog.contains("BUILD FAILURE")
assert buildLog.contains("httpcore5-h2")
assert buildLog.contains("it would not clash with any other dependency")