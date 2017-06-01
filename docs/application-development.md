# Chaincode Application Development

## Project Structure

Like many modern build tools, _chaintool_ is opinionated.  It expects a specific structure to your project as follows:

- [chaincode.yaml](#chaincodeyaml) in the top-level directory of your project
- a chaincode entry-point in ./src/chaincode ([example](https://github.com/hyperledger/fabric-chaintool/tree/master/examples/example02/app/src/chaincode))
- interface files in ./src/interfaces ([example](https://github.com/hyperledger/fabric-chaintool/tree/master/examples/example02/app/src/interfaces))
   - every project must define an appinit interface ./src/interfaces/appinit.cci ([example](interface.md#appinit-interface))

### chaincode.yaml

_chaincode.yaml_ is the central configuration file for a given chaintool-managed chaincode project.  An example looks like this:

```
# ----------------------------------
# chaincode example02
# ----------------------------------
#
# Copyright (C) 2016 - Hyperledger
# All rights reserved
#

Schema:  1
Name:    org.hyperledger.chaincode.example02
Version: 0.1-SNAPSHOT

Platform:
        Name: org.hyperledger.chaincode.golang
        Version: 1

Provides: [self] # 'self' is a keyword that means there should be $name.cci (e.g. org.hyperledger.chaincode.example02.cci)
```

All chaincode.yaml should minimally contain:

- schema
- project name/version
- platform
- interface declarations (provides/consumes)

#### Schema
This helps to relay compatibility with the structures used in the chaincode.yaml itself.  At the time of writing, it should be "1".

#### Project name/version

This is something that should uniquely identify your chaincode project for human/UX consumption.  It is generally advised that a DNS name of some kind be incorporated to promote global uniqueness.  Note that the Hyperledger subsystem in general does not interpret these names in any meaningful way other than for display purposes.

#### Platform

It is here that a chaincode may declare the compatibility/conformity to a specific platform.  The idea is to promote extensibility (e.g. other platforms may be added in the future) and also compatility (e.g. platform X, version Y may mean something very specifically about the type of chaincode language supported, the ABI for any peripheral libraries, etc).  It is analogous to the notion that java 1.7 is a different ABI than java 1.8, etc.  At the time of writing, the only supported platform are:

- [org.hyperledger.chaincode.golang](./platforms/golang.md) - The canonical platform for chaincode development, expressed as a chaintool managed superset.
- [org.hyperledger.chaincode.system](./platforms/system.md) - A special variant of [org.hyperledger.chaincode.golang](./platforms/golang.md) designed for system-chaincode development as an extension of the hyperledger fabric.  This platform is implicitly golang based and notably not compiled as system chaincode is compiled as part of the fabric build.

More platforms may be added in the future.

##### Adding platforms

The only core requirement is that both _chaintool_ and the chosen Hyperledger network are in agreement to support said platform.  The details of implementing this are "coming soon".

#### Interface Declarations

Interfaces (as included in ./src/interfaces) may be in one or two categories: Provided or Consumed.  _Provided_ means that the chaincode implements the interface and supports having clients or other chaincode invoke methods as declared.  Likewise, _consumed_ indicates that the chaincode expects to perform inter-chaincode invoke operations to a disparate chaincode instance that provides the interface.  It is perfectly fine (though perhaps uncommon) for a chaincode to both provide and consume a given interface (such as for proxy contracts which may accept operations in a polymorphic manner before passing operations on to a concrete instance).

Both Provides and Consumes are expressed as an array of 1 or more entries.  For example:

```
Provides: [org.hyperledger.chaincode.example02, org.hyperledger.chaincode.example03]
Consumes: [org.hyperledger.chaincode.example02]
```

If there aren't any interfaces in a particular category, the entry may be omitted.  Note that a chaincode that doesn't provide any interfaces doesn't sound particularly useful, however.  Therefore, it is expected that every project will include at least a Provides clause.

##### "self"

The keyword _self_ may be used as shorthand for an interface that shares the same name as the project (for instance, the org.hyperledger.chaincode.example02 project surfacing the org.hyperledger.chaincode.example02.cci interface), as a convenience.  It is idiomatic for a project to name its primary interfaces after itself, and therefore this shortcut is expected to be commonly used.  Example:

```
Provides: [self]
```

### Chaincode

The opinionated portion of chaincode path solely applies to the entry-point for your application.  Other paths for non-entry point code are generally fine if you are using a language that supports namespaces, etc.  For instance, the org.hyperledger.chaincode.golang platform assumes a $GOPATH of ./src and tries to build "chaincode" (via $GOPATH/src/chaincode).  However, if your chaincode uses go imports such as:

```golang
import (
   "foo"
   "bar/baz"
)
```

placed in ./src/foo and ./src/bar/baz respectively, they will be discovered perfectly fine.


<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
s
