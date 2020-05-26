/*
 SPDX-License-Identifier: Apache-2.0
*/

package sample_syscc

import (
	"errors"
	"github.com/hyperledger/fabric/core/chaincode/shim"
)

// SampleSysCC example simple Chaincode implementation
type SampleSysCC struct {
}

func (t *SampleSysCC) Init(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	var key, val string    // Entities

	if len(args) != 2 {
		return nil, errors.New("need 2 args (key and a value).")
	}

	// Initialize the chaincode
	key = args[0]
	val = args[1]
	// Write the state to the ledger
	err := stub.PutState(key, []byte(val))
	if err != nil {
		return nil, err
	}

	return nil, nil
}

// Transaction makes payment of X units from A to B
func (t *SampleSysCC) Invoke(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	var key, val string    // Entities

	if len(args) != 2 {
		return nil, errors.New("need 2 args (key and a value).")
	}

	// Initialize the chaincode
	key = args[0]
	val = args[1]

	_, err := stub.GetState(key)
	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get val for " + key + "\"}"
		return nil, errors.New(jsonResp)
	}

	// Write the state to the ledger
	err = stub.PutState(key, []byte(val))
	if err != nil {
		return nil, err
	}

	return nil, nil
}

// Query callback representing the query of a chaincode
func (t *SampleSysCC) Query(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	if function != "getval" {
		return nil, errors.New("Invalid query function name. Expecting \"getval\"")
	}
	var key string // Entities
	var err error

	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting key to query")
	}

	key = args[0]

	// Get the state from the ledger
	valbytes, err := stub.GetState(key)
	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get state for " + key + "\"}"
		return nil, errors.New(jsonResp)
	}

	if valbytes == nil {
		jsonResp := "{\"Error\":\"Nil val for " + key + "\"}"
		return nil, errors.New(jsonResp)
	}

	return valbytes, nil
}
