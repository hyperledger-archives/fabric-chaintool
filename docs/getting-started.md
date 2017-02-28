# Getting Started

## Installation

### Prerequisites

- A Java JRE/JDK v1.8 (or higher)
- make
- git

### Build

```
$ git clone https://github.com/hyperledger/fabric-chaintool.git
$ cd fabric-chaintool && make install
```

### Usage

```
$ chaintool -h
chaintool version: v0.10.1

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

## Quick Start Overview

- Define your [interfaces](interface.md)
    - [appinit.cci](interface.md#appinit-interface): Defines the "constructor" arguments for your application
    - One or more application specific interfaces: Defines the general methods and arguments for your application
- Define your [chaincode.yaml](application-development.md#chaincodeyaml)
- Write your application logic, implementing the interfaces defined in the previous step
- run _chaintool build_ to verify compilation
- run _chaintool package_ to create a deployment package for Fabric
- Deploy application to the network

