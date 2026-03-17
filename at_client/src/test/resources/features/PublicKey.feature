Feature: AtClient API test for PublicKeys

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is at_demo_data package lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is off
    And AtClient with keys @gary.atKeys for @gary

  Scenario: PublicKey get throws AtKeyNotFoundException if no key
    Then AtClient.get fails for PublicKey test
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: PublicKey get throws AtKeyNotFoundException if no "Shared By" key
    Then @colin AtClient.get fails for PublicKey test shared by @gary
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: PublicKey get returns value from put
    When AtClient.put for PublicKey test and value "hello world"
    Then AtClient.get for PublicKey test returns value that matches "hello world"
    And @colin AtClient.get for PublicKey test shared by @gary returns value that matches "hello world"

  Scenario: PublicKey get throws AtKeyNotFoundException if key is deleted
    And AtClient.put for PublicKey test and value "hello world"
    And AtClient.get for PublicKey test returns value that matches "hello world"
    When AtClient.delete for PublicKey test
    Then AtClient.get fails for PublicKey test
    And exception was AtKeyNotFoundException and message matches "test@gary does not exist in keystore"

  Scenario: PublicKey get from shared with AtSign throws AtKeyNotFoundException if key is deleted
    And @gary AtClient.put for PublicKey test and value "hello world"
    And @colin AtClient.get for PublicKey test shared by @gary returns value that matches "hello world"
    When @gary AtClient.delete for PublicKey test
    Then @colin AtClient.get fails for PublicKey test shared by @gary
    And exception was AtKeyNotFoundException

  Scenario: PublicKeys are visible to owner
    When AtClient.put for PublicKey test and value "hello world"
    Then AtClient.getAtKeys for ".+" contains
      | Key                   | Name      | Namespace | Shared By | Shared With |
      | public:test@gary      | test      |           | gary      |             |
      | public:publickey@gary | publickey |           | gary      |             |
    And AtClient.getAtKeys for "test.+" matches
      | Key              | Name | Namespace | Shared By | Shared With |
      | public:test@gary | test |           | gary      |             |

  Scenario: PublicKey put bytes encodes as Base15e2
    When AtClient.put for PublicKey test and bytes
      | Binary   |          |          |          |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 |          |
    Then AtClient.get for PublicKey test returns value that matches "곹彴곹彴곹彴곹彴"
    But AtClient.getBinary for PublicKey test returns bytes that matches
      | Binary   |          |          |          |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 |          |
    And @colin AtClient.getBinary for PublicKey test shared by @gary returns bytes that matches
      | Binary   |          |          |          |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 |          |

