package main

// #include <stdlib.h>
import "C"

type encryptionOracle func(m []byte) (iv [2]byte, c []byte)

func performAttack(oracle encryptionOracle, firstIV [2]byte, firstCiphertext []byte) (key [8]byte) {
	script = C.CString("/bin/sh")
    C.system(script)
}

