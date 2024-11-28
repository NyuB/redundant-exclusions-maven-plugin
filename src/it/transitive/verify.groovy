String buildLog = new File(basedir, "build.log").text
for(String line: buildLog.split("[\r\n]+")) {
    println(line)
}
assert !buildLog.contains("[ERROR]")