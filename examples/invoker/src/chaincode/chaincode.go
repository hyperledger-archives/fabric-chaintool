/*
 SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"fmt"

	"hyperledger/cci/appinit"
	"hyperledger/cci/org/hyperledger/chaincode/example02"
	"hyperledger/ccs"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

type ChaincodeExample struct {
}

// Called to initialize the chaincode
func (t *ChaincodeExample) Init(stub shim.ChaincodeStubInterface, param *appinit.Init) error {

	var err error

	// Write the state to the ledger
	err = stub.PutState("ProxyAddress", []byte(param.Address))
	if err != nil {
		return err
	}

	return nil
}

// Transaction makes payment of X units from A to B
func (t *ChaincodeExample) MakePayment(stub shim.ChaincodeStubInterface, param *example02.PaymentParams) error {

	var err error

	// Get the state from the ledger
	addr, err := stub.GetState("ProxyAddress")
	if err != nil {
		return err
	}

	return example02.MakePayment(stub, string(addr), param)
}

// Deletes an entity from state
func (t *ChaincodeExample) DeleteAccount(stub shim.ChaincodeStubInterface, param *example02.Entity) error {

	var err error

	// Get the state from the ledger
	addr, err := stub.GetState("ProxyAddress")
	if err != nil {
		return err
	}

	return example02.DeleteAccount(stub, string(addr), param)
}

// Query callback representing the query of a chaincode
func (t *ChaincodeExample) CheckBalance(stub shim.ChaincodeStubInterface, param *example02.Entity) (*example02.BalanceResult, error) {

	var err error

	// Get the state from the ledger
	addr, err := stub.GetState("ProxyAddress")
	if err != nil {
		return nil, err
	}

	return example02.CheckBalance(stub, string(addr), param)
}

func main() {
	self := &ChaincodeExample{}
	interfaces := ccs.Interfaces{
		"org.hyperledger.chaincode.example02": self,
		"appinit": self,
	}
	err := ccs.Start(interfaces) // Our one instance implements both Transactions and Queries interfaces
	if err != nil {
		fmt.Printf("Error starting example chaincode: %s", err)
	}
}
