<img width=250px src="https://atsign.dev/assets/img/atPlatform_logo_gray.svg?sanitize=true">

# The atPlatform for Java developers

This repo contains libraries, tools, samples and examples for developers who wish
to work with the atPlatform from Java code.

NB: As of May 3 2022, the Java client library can still be considered a 1.0.0-Beta version - i.e. there may occasionally
be breaking changes, based on feedback from users of the library, until we get to a final version 1.0.0

### Contributions welcome!

[![gitHub license](https://img.shields.io/badge/license-BSD3-blue.svg)](./LICENSE)

All of our software is open with intent. We welcome contributions - we want pull requests, and we want
to hear about issues. See also [CONTRIBUTING.md](CONTRIBUTING.md)

## What's here / changelog
### Next ...
* Getting started guide - from nothing to end-to-end-encrypted chat session in < 5 minutes
* fluid client APIs for sharing data - e.g. share(value).with(atSign/s).as(keyName)
* extend client REPL so that you can call AtClient methods (e.g. the share() above) interactively 

### May 29 2022
* Retry bug fixed in Register CLI
* Config yaml parameters restructured and backwards compatibility provided to break existing usages.
* New parameter added to validateOtp method in RegisterUtil.java. The usage of this parameter is provided in java docs of the respective method.


### May 18 2022
* A new CLI tool Register has been introduced which can acquire a free atsign and register it to the provided email.
* Register CLI also handles calling the Onboard client with the cram secret which was received during the registration process.

### May 03 2022
* Better event distribution
* Improved Monitor's event generation
* Added 'userDefined' to the AtEventType enum, to allow the event bus to be used by application code
* Caches shared keys after first retrieval
* AtClientImpl listens for updateNotification events, decrypts the ciphertext on-the-fly, and publishes a decryptedUpdateNotification which is more useful for application code
* Enhanced REPL to optionally listen to only decryptedUpdateNotification; added command-line flag to listen to both

### Apr 29 2022
* **at_client** : Initial implementation of Java client library for the atPlatform. README will come soon 
but here's a very brief summary which will get you going if you already know the basics of the atPlatform
and have used the Dart/Flutter packages
  * **Uses Maven** 
    * The Maven target you want is 'install' which will put things in the 'target' output directory
  * The **CLI tools** will give you the best overview of how to use the library as a whole. There are five CLIs
    in the initial commit:
    * **Onboard** - generate keys for a new @-sign. If you already have a .keys file, you can reuse it.
      Currently, the Java library expects keys for @alice to be in ./keys/@alice.keys
    * **REPL** - you can use this to type @-protocol commands and see responses; but the best thing about the
      REPL currently is that it shows the data notifications as they are received. The REPL code has the
      essentials of what a 'receiving' client needs to do - i.e.
      * create an AtClient
      * add an event listener which
        * receives data update/delete notification events (the event data contains the ciphertext)
        * calls 'get' to decrypt
    * **Share** - a simple 'sender' client - shares some data with another @-sign
    * **Get** - gets data which was shared by another @-sign
    * **Delete** - deletes data that this Atsign previously shared with another
  * To run them, having done a mvn install 
    `java -cp "target/client-1.0-SNAPSHOT.jar:target/lib/*" org.atsign.client.cli.REPL` (or Onboard/Share/Get/Delete )
