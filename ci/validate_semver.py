import sys
import re

if len(sys.argv) < 2:
    print(f"Usage: {sys.argv[0]} <VERSION>")
    exit(2)
else:
    if re.match("^[0-9]+[.][0-9]+[.][0-9]+$", sys.argv[1]):
        exit(0)
    else:
        print(f"Version '{sys.argv[1]}' does not match semantic versioning pattern major.minor.patch")
        exit(1)


