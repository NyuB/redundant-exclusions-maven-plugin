String buildLog = new File(basedir, "build.log").text
assert !buildLog.contains("[ERROR]")
assert buildLog.contains("BUILD SUCCESS")