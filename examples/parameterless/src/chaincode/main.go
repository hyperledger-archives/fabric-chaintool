/*
 SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"fmt"

	"hyperledger/cci/appinit"
	app "hyperledger/cci/org/hyperledger/chaincode/example/parameterless"
	"hyperledger/ccs"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

type ChaincodeExample struct {
}

// Called to initialize the chaincode
func (t *ChaincodeExample) Init(stub shim.ChaincodeStubInterface, param *appinit.Init) error {

	return nil
}

func (t *ChaincodeExample) TestParameterless(stub shim.ChaincodeStubInterface) (*app.MyReturnType, error) {
	return nil, nil
}

func main() {
	self := &ChaincodeExample{}
	interfaces := ccs.Interfaces{
		"org.hyperledger.chaincode.example.parameterless": self,
		"appinit": self,
	}

	err := ccs.Start(interfaces) // Our one instance implements both Transactions and Queries interfaces
	if err != nil {
		fmt.Printf("Error starting example chaincode: %s", err)
	}
}
