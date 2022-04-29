<img src="https://atsign.dev/assets/img/@dev.png?sanitize=true" alt="@ Dev logo">

### Now for a little internet optimism

# The @ platform for Java developers

This repo contains libraries, tools, samples and examples for developers who wish
to work with the @ platform from Java code.

### Contributions welcome!

[![gitHub license](https://img.shields.io/badge/license-BSD3-blue.svg)](./LICENSE)

All of our software is open with intent. We welcome contributions - we want pull requests, and we want
to hear about issues. See also [CONTRIBUTING.md](CONTRIBUTING.md)

## What's here / changelog
### Next ...
* Java client library
  * Better README for the Java client library
  * fluid APIs for sharing data - e.g. share(value).with(atSign/s).as(keyName)
  * extend REPL so that you can call AtClient methods (e.g. the share() above) interactively 
### Apr 29 2022
* **at_client** : Initial implementation of Java client library for the @ platform. README will come soon 
but here's a very brief summary which will get you going if you already know the basics of the @ platform
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
    * **Delete** - deletes data that this @ sign previously shared with another
  * To run them, having done a mvn install 
    `java -cp "target/client-1.0-SNAPSHOT.jar:target/lib/*" org.atsign.client.cli.REPL` (or Onboard/Share/Get/Delete )
