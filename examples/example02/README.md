# Example02
## Introduction
This directory contains an implementation of the chaincode application called "example02" as found in the hyperledger fabric distribution.  The application has been ported to chaintool to demonstrate the features, capabilities, and techniques for working with chaintool based development.
## Directory Layout
```
├── README.md
├── app
│   ├── chaincode.yaml
│   └── src
│       ├── chaincode
│       │   └── chaincode_example02.go
│       └── interfaces
│           ├── appinit.cci
│           └── org.hyperledger.chaincode.example02.cci
└── client
    ├── cljs
    │   ├── Makefile
    │   ├── appinit.proto
    │   ├── org.hyperledger.chaincode.example02.proto
    │   ├── project.clj
    │   └── src
    │       └── example02
    │           ├── core.cljs
    │           ├── hlc
    │           │   ├── core.cljs
    │           │   └── user.cljs
    │           ├── main.cljs
    │           ├── rpc.cljs
    │           └── util.cljs
    └── nodejs
        ├── appinit.proto
        ├── index.js
        ├── util.js
        ├── org.hyperledger.chaincode.example02.proto
        └── package.json
```
* app - contains a org.hyperledger.chaincode.golang platform based chaincode application.
  * This is the code deployed to the blockchain
* client - client applications for interacting with the chaincode application
  * nodejs - A simple demonstration of using nodejs.
  * cljs - A complete client for example02 written in ClojureScript

## Deploying and interacting with the example02
### Step 1 - Fabric environment
You will need a functioning peer that has chaintool v0.10.1 or higher available in the $PATH.  You may check the version of chaintool you have with 'chaintool -h'.  Once confirmed, start the peer with _peer node start_ as you normally would.  It is advised to keep the configuration as simple as possible (1 VP, no security, noops consensus)

### Step 2 - Package the chaincode application
Run 'chaintool package' from the app folder, noting the CAR output path
```
$ cd app
$ chaintool package
Writing CAR to: /Users/ghaskins/sandbox/git/chaintool/examples/example02/app/build/org.hyperledger.chaincode.example02-0.1-SNAPSHOT.car
Using path ./ ["src" "chaincode.yaml"]
|------+------------------------------------------+--------------------------------------------------------|
| Size |                   SHA1                   |                          Path                          |
|------+------------------------------------------+--------------------------------------------------------|
| 438  | d28b22c7c30506af926dcb5bc8b946ac35ddac7f | chaincode.yaml                                         |
| 3856 | 542d088197e1a46bc21326e67e5d84d2d2807283 | src/chaincode/chaincode_example02.go                   |
| 143  | 7305f65e18e4aab860b201d40916bb7adf97544f | src/interfaces/appinit.cci                             |
| 375  | 9492a1e96f380a97bba1f16f085fc70140154c65 | src/interfaces/org.hyperledger.chaincode.example02.cci |
|------+------------------------------------------+--------------------------------------------------------|
Platform:            org.hyperledger.chaincode.golang version 1
Digital Signature:   none
Raw Data Size:       4812 bytes
Archive Size:        2371 bytes
Compression Alg:     gzip
Chaincode SHA3:      f7026e0675b22a9d78b9f7f0cb97c93165bdefedc86de97f00e76b506c707b4ddbdfe97ad702ad600eae518891b9f0f1c8cb9a8b29b83908c2f6d46a6bcf4ecd
```
#### Note:
The _chaintool package_ command is designed to package for deployment, not development. If you started your node with _peer node start --peer-chaincodedev_, run _chaintool build_ instead. This is analogous to building non-chaintool chaincode using _go build_. The output will be placed in the _app/build/bin/_ directory.
### Step 3 - Compile the client
Run 'make' from the client/cljs folder
```
$ make
lein npm install
example02@0.1.0-SNAPSHOT /Users/ghaskins/sandbox/git/chaintool/examples/example02/client/cljs
├─┬ protobufjs@5.0.1
│ ├─┬ ascli@1.0.0
│ │ ├── colour@0.7.1
│ │ └── optjs@3.2.2
│ ├─┬ bytebuffer@5.0.1
│ │ └── long@3.1.0
│ ├─┬ glob@5.0.15
│ │ ├─┬ inflight@1.0.4
│ │ │ └── wrappy@1.0.1
│ │ ├── inherits@2.0.1
│ │ ├─┬ minimatch@3.0.0
│ │ │ └─┬ brace-expansion@1.1.4
│ │ │   ├── balanced-match@0.4.1
│ │ │   └── concat-map@0.0.1
│ │ ├── once@1.3.3
│ │ └── path-is-absolute@1.0.0
│ └─┬ yargs@3.32.0
│   ├── camelcase@2.1.1
│   ├─┬ cliui@3.2.0
│   │ ├─┬ strip-ansi@3.0.1
│   │ │ └── ansi-regex@2.0.0
│   │ └── wrap-ansi@2.0.0
│   ├── decamelize@1.2.0
│   ├─┬ os-locale@1.4.0
│   │ └─┬ lcid@1.0.0
│   │   └── invert-kv@1.0.0
│   ├─┬ string-width@1.0.1
│   │ ├─┬ code-point-at@1.0.0
│   │ │ └── number-is-nan@1.0.0
│   │ └── is-fullwidth-code-point@1.0.0
│   ├── window-size@0.1.4
│   └── y18n@3.2.1
└─┬ source-map-support@0.4.0
  └─┬ source-map@0.1.32
    └── amdefine@1.0.0

lein cljsbuild once
Compiling ClojureScript...
Compiling "out/example02.js" from ["src"]...
Successfully compiled "out/example02.js" in 3.075 seconds.
Compilation complete: use "node out/example02.js --help" for execution instructions
```
This will generate a nodejs application in _out/example02.js_.  You may run it with --help to get a command summary.
```
$ node out/example02.js --help
Usage: example02 [options]

Options Summary:
      --host HOST    localhost      Host name
      --port PORT    3000           Port number
  -p, --path PATH                   Path/URL to the chaincode (deploy only, mutually exclsive with -n)
  -n, --name NAME                   Name of the chaincode (mutually exclusive with -p)
  -c, --command CMD  check-balance  One of [deploy make-payment delete-account check-balance]
  -a, --args ARGS                   JSON formatted arguments to submit
  -h, --help
```
### Step 4 - Deploy the CAR
We can deploy the CAR we packaged in Step 2 using the "-c deploy" feature of the client.  We specify the path the CAR with -p and args with -a.

```
$ node ./out/example02.js -c deploy -p /fqp/to/app.car --port 5000 --args '{"partyA":{"entity":"a", "value":100}, "partyB":{"entity":"b", "value":100}}'
```
This will return something that looks like:
```
Response: {:result {:status OK, :message a9114852d11579bb6000abd7b2d3b25403aa7ff4f365a80ab2382a1616a066cddacefd3422c62337ba1b5eda2b2f4f04f5a2e3dbd411159db188d6946e83a95b}}
```
Note the hash that is returned in the {:result {:message}}, as this is your chaincode instance ID or "name".

#### Note:
-p must be a fully qualified path since it is passed to the VP as is.  Future versions of the tool/peer may allow inline data, TBD.

-a is expected to be a JSON structure that matches the protobuf definition for the request in particular.  In this case, we are deploying so we are interested in the _Init_ message within the appinit.proto.

#####If you started your node with _peer node start --peer-chaincodedev_, deploy your chaintool build like you would with non-chaintool chaincode.
```
$ CORE_CHAINCODE_ID_NAME=org.hyperledger.chaincode.example02 CORE_PEER_ADDRESS=0.0.0.0:30303 ./app/build/bin/org.hyperledger.chaincode.example02-0.1-SNAPSHOT
$ node ./out/example02.js -c deploy -n org.hyperledger.chaincode.example02 --port 5000 --args '{"partyA":{"entity":"a", "value":100}, "partyB":{"entity":"b", "value":100}}'
```

#### Where did the .proto files come from?
_chaintool proto_ was used to generate .proto files from the .cci files defined in ./app/src/interfaces
### Step 5 - Query our current balances
We can use "-n $hash -c check-balance" to check the balance of one of our accounts
```
$ node ./out/example02.js -n a9114....946e83a95b --port 5000 -c check-balance --args '{"id":"a"}'
```
This should return with
```
Success: Balance = 100
```
We can repeat the process with id "b".  Likewise, we can confirm that the system should return an error for any ids besides "a" or "b".
#### Note:
If you started your node with _peer node start --peer-chaincodedev_, change the hash (_a9114....946e83a95b_) to the name you chose in Step 4.
```
$ node ./out/example02.js -n org.hyperledger.chaincode.example02 --port 5000 -c check-balance --args '{"id":"a"}'
```
### Step 6 - Make a Payment
Now lets transfer 10 tokens from a to b.
```
$ node ./out/example02.js -n  a9114....946e83a95b --port 5000 -c make-payment --args '{ "partySrc": "a", "partyDst": "b", "amount": 10}'
```
You should be able to repeat the query in Step 5 and confirm that a now holds 90 tokens while b holds 110.

<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
