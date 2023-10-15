Feature: Login

  Scenario: Successful load application settings
    Given I am on the application settings page
    When I enter an application id
    And I tap the "Load application settings" button
    Then I should be logged in
