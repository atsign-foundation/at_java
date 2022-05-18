# Getting Started Guide



## Requirements

* [Java 8 or higher](https://www.java.com/en/download/)
* [maven](https://maven.apache.org/download.cgi)

## Setting Up your environment

1. [Install java](https://www.java.com/en/download/help/download_options.html)
2. [Install maven](https://maven.apache.org/install.html)
3. Add maven to you path variable
4. Open your terminal 
5. cd to `at_java/at_client`
6. run `mvn install`, after instillation you should see `[INFO] BUILD SUCCESS`

## Registration

To register an atsign run the following commands in your terminal after setting up your environment

1. Set proper configurations in at_client/src/main/resources/config.yaml
2. run `mvn install` in directory at_java/at_client
3. run `java -cp "target/client-1.0-SNAPSHOT.jar:target/lib/*" org.atsign.client.cli.Register <email@email.com>`

```
Getting free atsign
Got atsign: @anxiouswangga5
Sending one-time-password to :<email@email.c0m>
Got response: Sent Successfully
Enter OTP received on: <email@email.com>
```
4. Enter the OTP received on email provided in the previous step. NB: OTP is case sensitive
```
xxxx
Validating one-time-password
Got response: Verified
```
Congratulations you have sucessfully registered.
5. The .atKeys file for this new atsign is stored in at_client/keys.
Note: The default properties in config.yaml point to the testing environment. Information has been provided in the same file on how to use the production environment.
