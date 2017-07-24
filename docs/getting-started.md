# Getting Started

## Installation

### Prerequisites
- [Java](https://www.java.com) JRE/JDK v1.8 (or higher)
- [Golang](https://golang.org) v1.7 or higher
- [Protobuf Compiler](https://developers.google.com/protocol-buffers/docs/downloads) v3.0 or higher
- [protoc-gen-go](https://github.com/golang/protobuf/tree/master/protoc-gen-go)

### Install
Download the latest [release](https://github.com/hyperledger/fabric-chaintool/releases) and install it in your $PATH

#### Example
```
$ sudo curl https://nexus.hyperledger.org/content/repositories/releases/org/hyperledger/fabric/hyperledger-fabric/chaintool-1.0.0/hyperledger-fabric-chaintool-1.0.0.jar -Lo /usr/local/bin/chaintool && sudo chmod +x /usr/local/bin/chaintool
```

### Usage
Chaintool supports a number of "actions" (e.g. "chaintool build").  See _chaintool -h_ for details.

```
$ chaintool -h
chaintool version: v1.0.0

Usage: chaintool [general-options] action [action-options]

General Options:
  -v, --version  Print the version and exit
  -h, --help

Actions:
  build -> Build the chaincode project
  buildcar -> Build the chaincode project from a CAR file
  clean -> Clean the chaincode project
  package -> Package the chaincode into a CAR file for deployment
  unpack -> Unpackage a CAR file
  ls -> List the contents of a CAR file
  proto -> Compiles a CCI file to a .proto
  inspect -> Retrieves metadata from a running instance

(run "chaintool <action> -h" for action specific help)
```

## Development Overview
Every chaintool based chaincode application minimally consists of the chaincode itself, and one or more clients to communicate with it.  For our walk-through, we will be using the standard [golang-based](platforms/golang.md#golang-chaincode-platform) chaincode model, version 1, and the [Node SDK](https://www.npmjs.com/package/fabric-client).

It is assumed that the reader is familiar with Hyperledger Fabric in general, and [how to develop](http://hyperledger-fabric.readthedocs.io/en/latest/chaincode.html) "standard" (i.e. non-chaintool based) chaincode and clients.

### Phase 1: Chaincode Development
We will begin by defining our chaincode application, since this will dictate the operations that a client needs to support.

- Create a directory to hold your chaincode application logic.
    - We will refer to this directory as $CHAINCODE in the remainder of this document.
    - Optional: Set up an SCM such as git.
- Define your [interfaces](interface.md) under $CHAINCODE/src/interfaces
    - [$CHAINCODE/src/interfaces/appinit.cci](interface.md#appinit-interface): This defines the "constructor" arguments for your application.
    - Create one or more application specific interfaces: These define the general methods and arguments for your application.
        - E.g. $CHAINCODE/src/interfaces/org.acme.myapplication.cci
        - Tip: Use reverse DNS naming to ensure your interface is globally unique.
- Define your [$CHAINCODE/chaincode.yaml](application-development.md#chaincodeyaml) project definition.
    - Set [schema v1](application-development.md#schema)
    - Set your project [name and version](application-development.md#project-nameversion)
    - Use platform specifier [org.hyperledger.chaincode.golang v1](platforms/golang.md#platform-specifier)
    - Be sure to update the [interface declarations](application-development#interface-declarations) (i.e. _Provides_ and _Consumes_) with the CCI files added in the previous step.
- Define your [chaincode entrypoint](platforms/golang.md#entry-point) under $CHAINCODE/src/chaincode.
    - E.g. $CHAINCODE/src/chaincode/main.go
    - [Import](platforms/golang.md#imports) the chaintool generated code and fabric shim.
    - [Register](platforms/golang.md#hooks-and-registration) your chaincode inside your main() function.
- Write your application logic by [implementing](platforms/golang.md#callbacks) your interfaces.
    - Init() for appinit.cci
    - Any other functions declared within interfaces specified as _Provided_ in your chaincode.yaml

Note: You may find a complete chaincode example [here](https://github.com/hyperledger/fabric-chaintool/tree/master/examples/example02/app)

You man run _chaintool build_ at this time to locally verify compilation of your application.  However, before you may deploy it to a Fabric network, you will need to develop a client for your application using one of the Fabric SDKs.  Proceed to Phase 2.

### Phase 2: Client Development
Chaintool-based chaincodes employ a specific [parameter encoding](client-development.md#protocol) based on [Google Protocol Buffers (protobufs)](https://developers.google.com/protocol-buffers/).  Because of this encoding, chaintool-based chaincodes are not generally compatible with the _peer CLI_ methods.  Rather, we must develop a client in code that is capable of performing the encoding/decoding for us.

Fortunately Fabric provides a variety of SDKs on platforms that also enjoy robust protobuf support.  Therefore, one only needs to build a standard client on the platform of the reader's choosing, with the additional understanding of the chaintool imposed parameter encoding scheme.

We will be building a client using the [Node SDK](https://www.npmjs.com/package/fabric-client).  It is beyond the scope of this document to cover basic Node SDK client development.  Instead, we will focus solely on the elements that are specific to chaintool.

#### Encoding Details
##### Input Parameter Encoding
Consider the following CCI snippet taken from _org.hyperledger.chaincode.example02.cci_:
```
message PaymentParams {
        string partySrc = 1;
        string partyDst = 2;
        int32  amount   = 3;
}

...

functions {
        void MakePayment(PaymentParams) = 1;
        ...
}
```

A client wishing to invoke MakePayment() would encode the request into an input array via the SDK as follows:

```
["org.hyperledger.chaincode.example02/fcn/1", <Buffer> "CgNmb28="]
```

Where "org.hyperledger.chaincode.example02" is the name of the interface we are invoking, "fcn" is a constant, "1" is the index of the "MakePayment" method, and _<Buffer> "CgNmb28="_ is a protobuf encoding of _PaymentParams_.

##### Output Parameter Encoding
For methods that return non-void types, the output will be a protobuf encoded byte array.

#### Integrating with the Node SDK

As mentioned above, a client for a chaintool-based application is virtually identical to a non-chaintool application in all aspects except for the parameter encoding.  In order to add support for the requisite encoding, we need three basic things:

- One or more .proto files representing the protobuf schema we want to use as the basis of encode/decode.
    - TIP: _chaintool proto_ can convert a CCI file to a pure .proto file for convenient client consumption.
- A NodeJS compatible protobuf library that can work with our .proto schemas.
    - [protobuf.js](https://www.npmjs.com/package/protobufjs) is an excellent choice.  It can work using just your .proto files and reflection, eliminating a discrete protoc compilation phase found in many other platforms.
- The integration of the two items above together to encode/decode our input and output parameters properly.

For the Node SDK, this can be as simple as defining a request object such as:
```
var args = new app.PaymentParams({'partySrc':'A', 'partyDst':'B', 'amount':100});
var request = {
    chaincodeType: 'car',
    fcn: 'org.hyperledger.chaincode.example02/fcn/1',
    args: [args.toBuffer()]
};
```

A complete example client can be found [here](https://github.com/hyperledger/fabric-chaintool/tree/master/examples/example02/client/nodejs)

### Phase 3: Deployment
Generally speaking, deployment of chaincode in Fabric involves two discrete steps: _Install_ and _Instantiate_.  The Install phase is where a chaincode application is provided to the network.  The Instantiate phase is where a previously installed application is initialized and enters an active state in the network.

Chaintool-based chaincode is not materially different in this overall flow.  What is different is the packaging and encodings that are used.

#### Packaging
Chaintool provides a package format called CAR (Chaincode Archive).  This format was designed from the ground up to be a deterministic and platform agnostic way to package up chaincodes for Fabric.

- Run _chaintool package_ to create a Fabric deployment package from your project.
- Use _chaintool ls_ or _chaintool unpack_ to work with CAR files previously generated.

#### Installation
Both the _peer CLI_ and _Node SDK_ have native support for installing CAR packages.  Since there are no parameter encodings that accompany _install_, you may chose the workflow that best suits you.

#### Instantiation
Instantiating a previously installed CAR file involves encoding the Init() parameters (as designed in your application's appinit.cci).  Therefore, it is most convenient to use the SDK's _chain.sendInstantiateProposal()_ for this operation.  Simply encode the appinit.cc::Init{} message in the request.fcn/request.args as previously noted.

### Phase 4: Interacting with your service
At this phase, your chaincode application is up and running.  You may execute Invoke() and Query() operations against it just like any other chaincode, as long as you adhere to the encoding schemas.

Congratulations!

### Questions/Comments?
Join us on the #fabric-chaintool channel on [Hyperledger Rocket Chat](https://chat.hyperledger.org) or reach out to us on the [hyperledger-fabric mailing list](https://lists.hyperledger.org/mailman/listinfo/hyperledger-fabric).

<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
s
