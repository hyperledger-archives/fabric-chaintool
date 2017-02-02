var http = require('http');
var pb = require("protobufjs");

var builder = pb.newBuilder({ convertFieldsToCamelCase: true });

pb.loadProtoFile("./appinit.proto", builder);
var init = builder.build("appinit");

pb.loadProtoFile("./org.hyperledger.chaincode.example02.proto", builder);
var app = builder.build("org.hyperledger.chaincode.example02");

function Deploy(id, params) {

    invoke('deploy', id, 'init', new init.Init(params), function(res) {
        res.setEncoding('utf8');
        res.on('data', function (chunk) {
            var resp = JSON.parse(chunk);
            console.log('Response: ' + chunk);
        });

    });
}

function CheckBalance(id, params) {

    invoke('query', id, 'org.hyperledger.chaincode.example02/query/1', new app.Entity(params), function(res) {
        res.setEncoding('utf8');
        res.on('data', function (chunk) {
            var resp = JSON.parse(chunk);
            if (resp.result.status == "OK") {
                var result = app.BalanceResult.decode64(resp.result.message);
                console.log("BalanceResult: " + result.balance);
            } else {
                console.log('ERROR: ' + chunk);
            }
        });

    });
}

function invoke(method, id, func, args, cb) {
    var post_data = JSON.stringify({
        'jsonrpc': '2.0',
        'method': method,
        'params': {
            'type': 3,
            'chaincodeID': id,
            'ctorMsg': {
                'function': func,
                'args':[args.toBase64()]
            }
        },
        "id": 1
    });
    console.log(post_data);
    post(post_data, '/chaincode', cb);
}

function post(pdata, path, cb) {
    var post_options = {
        host: 'localhost',
        port: '5000',
        path: path,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    var post_req = http.request(post_options, cb);

    // post the data
    post_req.write(pdata);
    post_req.end();

}

Deploy(
    {
        'name': 'mycc'
    },
    {
        'partyA': {
            'entity': 'foo',
            'value': 100
        },
        'partyB': {
            'entity': 'bar',
            'value': 100
        }
    }
);

CheckBalance(
    {
        'name': 'mycc'
    },
    {
        "id": "foo"
    }
);
