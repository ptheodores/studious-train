#!/usr/bin/python3

from sys import argv
from subprocess import Popen, PIPE
import time

if len(argv) != 2:
    print("Usage: ./sol.py <path-to-router-binary>\n")
    exit(1)

router_path = argv[1]
router  = Popen(router_path, stdin=PIPE, stdout=PIPE, stderr=PIPE)#,text=True)

#
# TODO: Implement your attack here.
#
# You can send data to the STDIN of the router binary with the following:
#
#   router.stdin.write("data to send\n")
#   router.stdin.flush()  # don't forget to flush so stdin sees your input
#
# You can read the next line from the router binary's STDOUT or STDERR with the
# following:
#
#   router.stdout.readline()
#

nextline = router.stdout.readline();
print(nextline)
outputline = nextline.split()
while True:
    nextline = None
    router.stdin.write("1122334455667788\n")
    router.stdin.flush()
    nextline = router.stdout.readline()
    print(nextline)
    nextline = nextline.split()
    if nextline[0] == outputline[0]:
        print("Initial router output: " + str(outputline[0]) + ", " + str(outputline[1]))   
        print("Our message: " + "1122334455667788")
        print("Router output with our message: " + str(nextline[0]) + ", " + str(nextline[1]))
        r = hex(int(nextline[1], 16)) ^ 112233445566778
        print(hex(int(nextline[1], 16)))
        print("here's r: " + str(r))
        key = hex(int(outputline[1], 16)) ^ r
        print("The key: " + str(key))
        exit()






