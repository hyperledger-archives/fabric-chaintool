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
var CA = require('fabric-ca-client/lib/FabricCAClientImpl.js');
var User = require('fabric-client/lib/User.js');

var chain;
var peer;

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

    peer = new Peer('grpc://localhost:7051');
    var orderer = new Orderer('grpc://localhost:7050');

    chain.addOrderer(orderer);
    chain.addPeer(peer);

    return utils.setStateStore(client, ".hfc-kvstore")
        .then(function() {
            var ca = new CA('http://localhost:7054');

            return utils.getUser(client, ca, 'admin', 'adminpw');
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
        .then(function(response) { utils.processProposalResponse(chain, response); })
        .then(utils.intradelay);
}

function sendTransaction(fcn, args) {
    var request = createRequest(fcn, args);
    return chain.sendTransactionProposal(request)
        .then(function(response) { utils.processProposalResponse(chain, response); })
        .then(utils.intradelay);
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
        .then(function(results) {
            return app.BalanceResult.decode(results[0]);
        });
}

program
    .version('0.0.1');

program
    .command('deploy')
    .description('deploy description')
    .option("-p, --path <path>", "Path to chaincode.car")
    .action(function(options){
        return connect()
            .then(function() {
                return deploy({
                    'partyA': {'entity':'A', 'value':100},
                    'partyB': {'entity':'B', 'value':200}},
                              options.path);
            })
            .catch(function(err) {
                console.log("error:" + err);
            });
    });

program
    .command('makepayment <partySrc> <partyDst> <amount>')
    .description('makepayment description')
    .action(function(partySrc, partyDst, amount){
        return connect()
            .then(function() {
                return makePayment({
                    'partySrc': partySrc,
                    'partyDst': partyDst,
                    'amount':   parseInt(amount)});
            })
            .catch(function(err) {
                console.log("error:" + err);
            });
    });

program
    .command('checkbalance <id>')
    .description('checkbalance description')
    .action(function(id){
        return connect()
            .then(function() {
                return checkBalance({'id':id});
            })
            .then(function(result) {
                console.log("balance:" + result.balance);
            })
            .catch(function(err) {
                console.log("error:" + err);
            });
    });


program.parse(process.argv);
