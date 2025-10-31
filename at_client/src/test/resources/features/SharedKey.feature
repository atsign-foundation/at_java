Feature: AtClient API test for SharedKeys

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is target/at_demo_data/lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is on
    And AtClient for gary

  Scenario: SharedKey get throws AtKeyNotFoundException if no key
    Then AtClient.get for SharedKey test shared with colin receives AtKeyNotFoundException and message "test@gary does not exist in keystore"

  Scenario: SharedKey get returns expected value for "Shared By" and "Shared With" atsigns
    When AtClient.put for SharedKey test shared with colin and value "hello world"
    Then gary AtClient.get for SharedKey test shared with colin returns value that matches "hello world"
    And colin AtClient.get for SharedKey test shared by gary returns value that matches "hello world"

  Scenario: SharedKey get throws AtKeyNotFoundException for "Shared By" and "Shared With" atsigns if key is deleted
    And AtClient.put for SharedKey test shared with colin and value "hello world"
    And AtClient.get for SharedKey test shared with colin returns value that matches "hello world"
    When AtClient.delete for SharedKey test shared with colin
    Then gary AtClient.get for SharedKey test shared with colin receives AtKeyNotFoundException with message "test@gary does not exist in keystore"
    And colin AtClient.get for SharedKey test shared by gary receives AtKeyNotFoundException and message "test@gary does not exist in keystore"

  # this doesn't work
#  Scenario: SharedKey get returns expected value for public key when no shared key
#    When AtClient.put for PublicKey test and value "hello world"
#    And AtClient.put for SharedKey test shared with colin and value "howdy world"
#    Then colin AtClient.get for SharedKey test shared by gary returns value that matches "howdy world"
#    But don AtClient.get for SharedKey test by gary returns value that matches "hello world"

  Scenario: SharedKeys are visible to owner
    When AtClient.put for SharedKey test shared with colin and value "hello world"
    Then AtClient.getAtKeys for ".+" contains
      | @colin:test@gary |
    And AtClient.getAtKeys for "test.+" contains
      | @colin:test@gary |

  Scenario: SharedKeys are not visible to other at signs
    And AtClient.put for SharedKey test shared with colin and value "hello world"
    Then don AtClient.getAtKeys for ".+" does NOT contain
      | public:test@gary |

