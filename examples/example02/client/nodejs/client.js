var program = require('commander');
var pb = require("protobufjs");

var builder = pb.newBuilder({ convertFieldsToCamelCase: true });

pb.loadProtoFile("./protos/appinit.proto", builder);
var init = builder.build("appinit");

pb.loadProtoFile("./protos/org.hyperledger.chaincode.example02.proto", builder);
var app = builder.build("org.hyperledger.chaincode.example02");

var hfc = require('fabric-client');
var hfcutils = require('fabric-client/lib/utils.js');
var utils = require('./lib/util.js');
var Peer = require('fabric-client/lib/Peer.js');
var Orderer = require('fabric-client/lib/Orderer.js');
var EventHub = require('fabric-client/lib/EventHub.js');
var CA = require('fabric-ca-client');
var User = require('fabric-client/lib/User.js');

var chain;
var peer;
var eventhub;

function createRequest(fcn, args) {
    var tx_id = hfcutils.buildTransactionID({length:12});
    var nonce = hfcutils.getNonce();

    // send proposal to endorser
    var request = {
        type: 'car',
        targets: [peer],
        chainId: 'testchainid',
        chaincodeId: 'mycc',
        fcn: fcn,
        args: [args.toBuffer()],
        txId: tx_id,
        nonce: nonce
    };

    return request;
}

function connect() {
    var client = new hfc();
    chain = client.newChain('chaintool-demo');

    eventhub = new EventHub();
    eventhub.setPeerAddr('grpc://localhost:7053');
    eventhub.connect();

    peer = new Peer('grpc://localhost:7051');
    var orderer = new Orderer('grpc://localhost:7050');

    chain.addOrderer(orderer);
    chain.addPeer(peer);

    return utils.setStateStore(client, ".hfc-kvstore")
        .then(() => {
            var ca = new CA('http://localhost:7054');

            return utils.getUser(client, ca, 'admin', 'adminpw');
        });
}

function disconnect() {
    return new Promise((resolve, reject) => {
        eventhub.disconnect();
        resolve();
    });
}

function deploy(args, path) {

    var request = createRequest('init', new init.Init(args));
    if (path) {
        request.chaincodePath = path;
    } else {
        chain.setDevMode(true);
    }

    // send proposal to endorser
    return chain.sendDeploymentProposal(request)
        .then((response) => {
            return utils.processResponse(chain, eventhub, request, response, 60000);
        });
}

function sendTransaction(fcn, args) {

    var request = createRequest(fcn, args);

    return chain.sendTransactionProposal(request)
        .then((response) => {
            return utils.processResponse(chain, eventhub, request, response, 20000);
        });
}

function sendQuery(fcn, args) {
    var request = createRequest(fcn, args);
    return chain.queryByChaincode(request);
}

function makePayment(args) {
    return sendTransaction('org.hyperledger.chaincode.example02/fcn/1',
                           new app.PaymentParams(args));
}

function checkBalance(args) {
    return sendQuery('org.hyperledger.chaincode.example02/fcn/3',
                     new app.Entity(args))
        .then((results) => {
            return app.BalanceResult.decode(results[0]);
        });
}

program
    .version('0.0.1');

program
    .command('deploy')
    .description('deploy description')
    .option("-p, --path <path>", "Path to chaincode.car")
    .action((options) => {
        return connect()
            .then(() => {
                return deploy({
                    'partyA': {'entity':'A', 'value':100},
                    'partyB': {'entity':'B', 'value':200}},
                              options.path);
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
    .description('makepayment description')
    .action((partySrc, partyDst, amount) => {
        return connect()
            .then(() => {
                return makePayment({
                    'partySrc': partySrc,
                    'partyDst': partyDst,
                    'amount':   parseInt(amount)});
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
    .description('checkbalance description')
    .action((id) => {
        return connect()
            .then(() => {
                return checkBalance({'id':id});
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
