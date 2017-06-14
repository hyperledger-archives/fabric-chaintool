/*
Copyright Greg Haskins <gregory.haskins@gmail.com> 2017. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"fmt"

	"hyperledger/cci/appinit"
	"hyperledger/ccs"

	"org/hyperledger/chaincode/helloworld"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

type ChaincodeExample struct {
}

func (t *ChaincodeExample) Init(stub shim.ChaincodeStubInterface, param *appinit.Init) error {

	return nil
}

func main() {
	self := &ChaincodeExample{}
	hello := &helloworld.Interface{}
	interfaces := ccs.Interfaces{
		"org.hyperledger.chaincode.helloworld": hello,
		"appinit": self,
	}

	err := ccs.Start(interfaces) // Our one instance implements both Transactions and Queries interfaces
	if err != nil {
		fmt.Printf("Error starting example chaincode: %s", err)
	}
}
