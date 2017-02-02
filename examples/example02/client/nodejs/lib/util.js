var hfc = require('fabric-client');
var utils = require('fabric-client/lib/utils.js');
var User = require('fabric-client/lib/User.js');

module.exports = {
    setStateStore: function(client, path) {
        return new Promise(function(resolve, reject) {
            return hfc.newDefaultKeyValueStore({path: path})
                .then(function(store) {
                    client.setStateStore(store);
                    resolve(true);
                });
        });
    },

    getUser: function(client, cop, username, password) {
        return client.getUserContext(username)
            .then(
                function(user) {
                    if (user && user.isEnrolled()) {
                        return Promise.resolve(user);
                    } else {
                        // need to enroll it with COP server
                        console.log("enrolling");
                        return cop.enroll({
                            enrollmentID: username,
                            enrollmentSecret: password
                        }).then(
                            function(enrollment) {
                                console.log("enrollment");
                                var member = new User(username, client);
                                return member.setEnrollment(enrollment.key, enrollment.certificate)
                                    .then(function() {
                                        return client.setUserContext(member);
                                    });
                            }
                        );
                    }
                }
            );
    },

    intradelay: function() {
        return new Promise(function(resolve, reject) {
            setTimeout(resolve, 20000);
        });
    },

    processProposalResponse: function(chain, results) {
        var proposalResponses = results[0];
        //logger.debug('deploy proposalResponses:'+JSON.stringify(proposalResponses));
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
            throw "bad result:" + results;
        }

    }
};
