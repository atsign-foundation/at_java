Feature: AtClient API test for PublicKeys

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is target/at_demo_data/lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is on
    And AtClient for gary

  Scenario: PublicKey get throws AtKeyNotFoundException if no key
    Then AtClient.get for PublicKey test receives AtKeyNotFoundException and message "test@gary does not exist in keystore"

  Scenario: PublicKey get returns value from put
    When AtClient.put for PublicKey test and value "hello world"
    Then AtClient.get for PublicKey test returns value that matches "hello world"
    Then colin AtClient.get for PublicKey test shared by gary returns value that matches "hello world"

  Scenario: PublicKey get throws AtKeyNotFoundException if key is deleted
    And AtClient.put for PublicKey test and value "hello world"
    And AtClient.get for PublicKey test returns value that matches "hello world"
    When AtClient.delete for PublicKey test
    Then AtClient.get for PublicKey test receives AtKeyNotFoundException and message "test@gary does not exist in keystore"

  Scenario: PublicKeys are visible to owner
    When AtClient.put for PublicKey test and value "hello world"
    Then AtClient.getAtKeys for ".+" contains
      | Key                   | Name      | Namespace | Shared By | Shared With |
      | public:test@gary      | test      |           | gary      |             |
      | public:publickey@gary | publickey |           | gary      |             |
    And AtClient.getAtKeys for "test.+" matches
      | Key              | Name | Namespace | Shared By | Shared With |
      | public:test@gary | test |           | gary      |             |

