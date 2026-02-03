Feature: AtClient API Monitor tests

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is at_demo_data package lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is off
    And AtClient and startMonitor for @gary

  Scenario: PublicKey put triggers a statsNotification
    And AtClient.put for PublicKey test and value "hello world"
    And AtClient monitor receives the following
      | Event Type        | messageType     | from  | to    | operation | key                     |
      | statsNotification | MessageType.key | @gary | @gary | update    | statsNotification.@gary |
    When AtClient.put for PublicKey test and value "bonjour le monde"
    Then AtClient monitor receives a new statsNotification

  Scenario: PublicKey does NOT trigger any notifications after stopMonitor
    And AtClient.put for PublicKey test and value "hello world"
    And AtClient monitor receives a new statsNotification
    When @gary AtClient stopMonitor
    And AtClient.put for PublicKey test and value "bonjour le monde"
    Then AtClient monitor does NOT receive any notifications

  Scenario: SharedKey put triggers expected notifications for shared with atsign
    When @colin AtClient.put for SharedKey test shared with @gary and value "hello world"
    Then @gary AtClient monitor receives the following
      | Event Type                  | messageType     | from   | to    | operation | key                    | decryptedValue |
      | sharedKeyNotification       | MessageType.key | @colin | @gary | update    | @gary:shared_key@colin |                |
      | updateNotification          | MessageType.key | @colin | @gary | update    | @gary:test@colin       |                |
      | decryptedUpdateNotification | MessageType.key | @colin | @gary | update    | @gary:test@colin       | hello world    |

