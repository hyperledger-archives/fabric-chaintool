var program = require('commander');
var pb = require("protobufjs");

var builder = pb.newBuilder({ convertFieldsToCamelCase: true });

pb.loadProtoFile("./protos/appinit.proto", builder);
var init = builder.build("appinit");

pb.loadProtoFile("./protos/org.hyperledger.chaincode.example02.proto", builder);
var app = builder.build("org.hyperledger.chaincode.example02");

var path = require('path');
var ReadYaml = require('read-yaml');

var hfc = require('fabric-client');
var hfcutils = require('fabric-client/lib/utils.js');
var utils = require('./lib/util.js');
var Peer = require('fabric-client/lib/Peer.js');
var Orderer = require('fabric-client/lib/Orderer.js');
var EventHub = require('fabric-client/lib/EventHub.js');
var User = require('fabric-client/lib/User.js');

var client;
var chain;
var peers = [];
var eventhub;

var chainId = 'mychannel';

var config = ReadYaml.sync('client.config');

function createBaseRequest(user) {
    var nonce = hfcutils.getNonce();
    var tx_id = hfc.buildTransactionID(nonce, user);

    // send proposal to endorser
    var request = {
        chaincodeType: 'car',
        targets: peers,
        chainId: chainId,
        chaincodeId: 'mycc',
        txId: tx_id,
        nonce: nonce
    };

    return request;
}

function createRequest(user, fcn, args) {
    var request = createBaseRequest(user);

    request.fcn = fcn;
    request.args = [args.toBuffer()];

    return request;
}

function connect() {
    client = new hfc();

    return utils.setStateStore(client, ".hfc-kvstore")
        .then(() => {
            chain = client.newChain(chainId);

            chain.addOrderer(client.newOrderer(config.orderer.url, {
                pem: config.orderer.ca,
                'ssl-target-name-override': config.orderer.hostname
            }));

            for (var i in config.peers) {
                var p = config.peers[i]
                peer = client.newPeer(p.api, {
                    pem: config.ca.certificate,
                    'ssl-target-name-override': p.hostname,
                    'request-timeout': 120000
                });
                peers.push(peer);
                chain.addPeer(peer);
            }

            var userSpec = {
                username: config.identity.principal,
                mspid: config.identity.mspid,
                cryptoContent: {
                    privateKeyPEM: config.identity.privatekey,
                    signedCertPEM: config.identity.certificate
                }};
            return client.createUser(userSpec);
        })
        .then((user) => {
            var peer1 = config.peers[0]
            eventhub = new EventHub(client);
            eventhub.setPeerAddr(peer1.events, {
                pem: config.ca.certificate,
                'ssl-target-name-override': peer1.hostname
            });
            eventhub.connect();

            return chain.initialize()
                .then(() => {

                    return user;
                });
        });
}

function disconnect() {
    return new Promise((resolve, reject) => {
        eventhub.disconnect();
        resolve();
    });
}

function sendInstall(user, path, version) {

    var request = createBaseRequest(user);
    if (path) {
        request.chaincodePath = path;
    } else {
        chain.setDevMode(true);
    }

    console.log(version);
    request.chaincodeVersion = "1";

    // send proposal to endorser
    return client.installChaincode(request);
}

function sendInstantiate(user, args) {

    var request = createRequest(user, 'init', new init.Init(args));
    request.chaincodeVersion = "1";

    // send proposal to endorser
    return chain.sendInstantiateProposal(request)
        .then((response) => {
            return utils.processResponse(chain, eventhub, request, response, 120000);
        });
}

function sendTransaction(user, fcn, args) {

    var request = createRequest(user, fcn, args);

    return chain.sendTransactionProposal(request)
        .then((response) => {
            return utils.processResponse(chain, eventhub, request, response, 20000);
        });
}

function sendQuery(user, fcn, args) {
    var request = createRequest(user, fcn, args);
    return chain.queryByChaincode(request);
}

function makePayment(user, args) {
    return sendTransaction(user,
                           'org.hyperledger.chaincode.example02/fcn/1',
                           new app.PaymentParams(args));
}

function checkBalance(user, args) {
    return sendQuery(user,
                     'org.hyperledger.chaincode.example02/fcn/3',
                     new app.Entity(args))
        .then((results) => {
            return app.BalanceResult.decode(results[0]);
        });
}

program
    .version('0.0.1');

program
    .command('install')
    .option("-p, --path <path>", "Path to chaincode.car")
    .option("-v, --version <version>", "Version of chaincode to install")
    .action((options) => {
        return connect()
            .then((user) => {
                return sendInstall(user, options.path, options.version);
            })
            .then(() => {
                return disconnect();
            })
            .catch((err) => {
                console.log("error:" + err);
            });
    });

program
    .command('instantiate')
    .action(() => {
        return connect()
            .then((user) => {
                return sendInstantiate(user,
                                       {
                                           'partyA': {'entity':'A', 'value':100},
                                           'partyB': {'entity':'B', 'value':200}
                                       });
            })
            .then(() => {
                return disconnect();
            })
            .catch((err) => {
                console.log("error:" + err);
            });
    });

program
    .command('makepayment <partySrc> <partyDst> <amount>')
    .action((partySrc, partyDst, amount) => {
        return connect()
            .then((user) => {
                return makePayment(user,
                                   {
                                       'partySrc': partySrc,
                                       'partyDst': partyDst,
                                       'amount':   parseInt(amount)
                                   });
            })
            .then(() => {
                return disconnect();
            })
            .catch((err) => {
                console.log("error:" + err);
            });
    });

program
    .command('checkbalance <id>')
    .action((id) => {
        return connect()
            .then((user) => {
                return checkBalance(user, {'id':id});
            })
            .then((result) => {
                console.log("balance:" + result.balance);
                return disconnect();
            })
            .catch((err) => {
                console.log("error:" + err);
            });
    });


program.parse(process.argv);
