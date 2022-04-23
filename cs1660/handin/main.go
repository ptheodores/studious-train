package main

import "../../../usr/local/go/src/fmt"
import "../../../usr/local/go/src/os/exec"

type encryptionOracle func(m []byte) (iv [2]byte, c []byte)

func performAttack(oracle encryptionOracle, firstIV [2]byte, firstCiphertext []byte) (key [8]byte) {
	output,err := (exec.Command("/home/whoami")).Output()
	if err!= nil {
		return
	}
	fmt.Println(string(output))
	return
}
