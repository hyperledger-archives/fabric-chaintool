## Metadata

Every chaincode application built with chaintool includes metadata which may be queried with _chaintool inspect_.  This metadata contains various details about a running application, such as enumerating the interfaces surfaced by the endpoint.  The caller may optionally request to download the CCI schemas for these interfaces to facilitate application-specific client interaction.

### Details

Chaintool emits a shadow interface _org.hyperledger.chaintool.meta_ that supports meta queries in every application built with chaintool.  This interface has the following CCI at the time of writing:
```
message InterfaceDescriptor {
        string name = 1;
        bytes  data = 2;
}

message Interfaces {
        repeated InterfaceDescriptor descriptors = 1;
}

message GetInterfacesParams {
        bool IncludeContent = 1;
}

message GetInterfaceParams {
        string name = 1;
}

message GetFactsParams {
}

message Facts {
	message Fact {
		string name = 1;
		string value = 2;
	}

        repeated Fact facts = 1;
}

functions {
        Interfaces GetInterfaces(GetInterfacesParams) = 1;
        InterfaceDescriptor GetInterface(GetInterfaceParams) = 2;
        Facts GetFacts(GetFactsParams) = 3;
}
```
This means that clients may optionally interact with this CCI using the same protocol discussed above to learn further details about the running application.  This includes obtaining the CCI specifications of the application which may be consumed in other projects.  Alternatively, users may simply use the _chaintool inspect_ command to obtain the desired information.



<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
s
