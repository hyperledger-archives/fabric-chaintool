var hfc = require('fabric-client');
var utils = require('fabric-client/lib/utils.js');
var User = require('fabric-client/lib/User.js');

function registerTxEvent(eventhub, txid, tmo) {
    return new Promise((resolve, reject) => {

        var handle = setTimeout(() => {
            eventhub.unregisterTxEvent(txid);
            reject("Timeout!");
        }, tmo);

        eventhub.registerTxEvent(txid, (tx) => {
            resolve();
            eventhub.unregisterTxEvent(txid);
            clearTimeout(handle);
        });
    });
}

function sendTransaction(chain, results)  {
    var proposalResponses = results[0];
    var proposal = results[1];
    var header   = results[2];
    var all_good = true;

    for(var i in proposalResponses) {
        let one_good = false;

        if (proposalResponses &&
            proposalResponses[0].response &&
            proposalResponses[0].response.status === 200) {

            one_good = true;
        }
        all_good = all_good & one_good;
    }

    if (all_good) {
        var request = {
            proposalResponses: proposalResponses,
            proposal: proposal,
            header: header
        };
        return chain.sendTransaction(request);
    } else {
        return Promise.reject("bad result:" + results);
    }
}

module.exports = {
    setStateStore: (client, path) => {
        return new Promise((resolve, reject) => {
            return hfc.newDefaultKeyValueStore({path: path})
                .then((store) => {
                    client.setStateStore(store);
                    resolve(true);
                });
        });
    },

    getUser: (client, cop, mspid, username, password) => {
        return client.getUserContext(username, true)
            .then((user) => {
                if (user && user.isEnrolled()) {
                    return Promise.resolve(user);
                } else {
                    // need to enroll it with COP server
                    return cop.enroll({
                        enrollmentID: username,
                        enrollmentSecret: password
                    }).then((enrollment) => {
                        var member = new User(username, client);
                        return member.setEnrollment(enrollment.key,
                                                    enrollment.certificate,
                                                    mspid)
                            .then(() => {
                                return client.setUserContext(member);
                            });
                    });
                }
            });
    },

    processResponse: (chain, eventhub, request, response, tmo) => {
        return Promise.all(
            [registerTxEvent(eventhub, request.txId.toString(), tmo),
             sendTransaction(chain, response)]);
    }
};
