@settings
Feature: Delete Currency Iso by API

  Background: System is configured.
    Create Currency Iso by API already executed.


  @admin @superadmin
  Scenario Outline: Delete Currency Iso by API
    Given The entity has the following information "<jsonFile>" as "<dto>"
    When I call the delete "<api>" with identifier "code"
    Then The entity is deleted
    And Validate that the statusCode is "<statusCode>"
    And The status is "<status>"
    And The message  is "<message>"
    And The errorCode  is "<errorCode>"

    Examples: 
      | jsonFile                                                         | dto            | api           | statusCode | status  | errorCode                        | message                                      |
      | settings/00006-currencyIso-api-create/SuccessTest.json           | CurrencyIsoDto | /currencyIso/ |        200 | SUCCESS |                                  |                                              |
      | settings/10006-currencyIso-api-delete/ENTITY_DOES_NOT_EXIST.json | CurrencyIsoDto | /currencyIso/ |        404 | FAIL    | ENTITY_DOES_NOT_EXISTS_EXCEPTION | Currency with code=NOT_EXIST does not exists. |
