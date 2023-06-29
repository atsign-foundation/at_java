<img width=250px src="https://atsign.dev/assets/img/atPlatform_logo_gray.svg?sanitize=true">

[![gitHub license](https://img.shields.io/badge/license-BSD3-blue.svg)](./LICENSE)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/atsign-foundation/at_java/badge)](https://api.securityscorecards.dev/projects/github.com/atsign-foundation/at_java)

# The atPlatform for Java developers

This repo contains libraries, tools, samples and examples for developers who wish
to work with the atPlatform from Java code.


## Getting Started
#### Note: Java and Maven are Prerequisites to use at_java

Clone the at_java repo from GItHub using

```shell
git clone https://github.com/atsign-foundation/at_java.git
```
Change directory into at_java/at_client

```shell
cd at_client
```

Compile the package using maven with the following command

```shell
mvn install
```

Now that the programs have been compiled, execute the following command to use at_java

```shell
java -cp "target/at_client-1.0-SNAPSHOT.jar:target/lib/*" org.atsign.client.cli.<class> [required arguments]
```

### The different classes(functionalities) that at_java contains:
1) REPL
2) Share
3) Get
4) Delete
5) Register
6) Onboard

#### Note: Each of these classes requires a different set of arguments, make sure to read the help text and provide necessary arguments
** Text about the remaining functionalities coming soon **
### Register
A class that accepts command line arguments which are used to fetch a free atsign and register it to the email provided.
Further, this atsign can be activated using a verification code sent to the registered email.

### Register with SUPER_API Key
Register* can also be used with a SUPER_API Key* that has privileges to preset and atsign with an activation code. 
When using the SUPER_API Key to register an atsign, the following sequence of calls take place:
1) User provides at_java/Register with the SUPER_API Key passed as an argument
2) at_java calls the AtSign Registrar API* Endpoint(get-atsign) with the SUPER_API Key provided
3) The AtSign registrar API responds with an AtSign-ActivationKey pair
4) at_java now call the AtSign Registrar API* Endpoint(activate-atsign) with the AtSign-ActivationKey pair
5) The API responds with a json containing the CRAM_KEY* for the concerned atsign
6) This CRAM_KEY* can be used to activate the atsign further making it usable
7) at_java does the activation automatically for you and stores your atKeys* file at path '~/.atsign/keys'
8) Now the atsign is activated and the atKeys file can be used to authenticate and perform protected operation with/on the atSign.

#### **
1) Register: This is a class in at_java that has the functionality to call the necessary API, handle responses in order to fetch and register atsigns
2) AtSign Registrar API: An AtSign service that is responsible for handling atsign's server creation, registration, authentication, reset and deletion
3) SUPER_API Key: 
   - All calls to the AtSign Registrar API require an API_KEY. But the SUPER_API Key has some additional privileges.
   - SUPER_API Keys have the privilege to preset an AtSign with an activation key so that this AtSign can be activated 
   without manually entering a verification code that is sent to the registered email
   - All SUPER_API Keys have a name containing two elements [say pre and post], all the atsigns generated using this 
   API_Key will be of the following format: (pre)atsign(post). Now the atsign will be @preatsignpost. 
   This is done to separate atsigns generated using SUPER_API Keys to the atsigns that are generated through other methods.
4) CRAM_KEY: This is a authentication key that will be used for a one-time authentication to activate an atsign which allows for assigning random, secure non-symmetric keypairs which will be further stored in the users atKeys file.
    * Note: CRAM_KEY will be deleted from the atsign server after an atKeys file has been generated, so only you have the keys to authenticate into your atsign
5) atKeys file: This will be a file generated during activation of an atsign that stores all the keys necessary for authenticating into atSign
    * That would mean users have to keep this file in a secured location
    * Users should keep this file safe, as there's only one copy of this file and losing it would mean the user would be unable to login to the atsign
    * If lost, users can reset the atsign and get a new atKeys file. This would result in loss of all data stored in the atsign's server
