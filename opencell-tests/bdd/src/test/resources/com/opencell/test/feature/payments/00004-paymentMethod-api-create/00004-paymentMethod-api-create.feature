@payments @ignore @review
Feature: Create Payment Method by API

  Background: The system is configured

  @admin @superadmin
  Scenario Outline: Create Payment Method by API
    Given The entity has the following information "<jsonFile>" as "<dto>"
    When I call the "<api>"
    Then The payment method is created
    And Validate that the statusCode is "<statusCode>"
    And The status is "<status>"
    And The message  is "<message>"
    And The errorCode  is "<errorCode>"

    Examples: 
      | jsonFile                                                           | dto              | api                     | statusCode | status  | errorCode             | message                                                                          |
      | payments/00004-paymentMethod-api-create/SuccessTest.json           | PaymentMethodDto | /payment/paymentMethod/ |        200 | SUCCESS |                       |                                                                                  |
      | payments/00004-paymentMethod-api-create/GENERIC_API_EXCEPTION.json | PaymentMethodDto | /payment/paymentMethod/ |        500 | FAIL    | GENERIC_API_EXCEPTION | org.meveo.api.message.exception.InvalidDTOException: Missing payment method type |
      | payments/00004-paymentMethod-api-create/INVALID_PARAMETER.json     | PaymentMethodDto | /payment/paymentMethod/ |        400 | FAIL    | INVALID_PARAMETER     | Cannot deserialize value of type `java.lang.Boolean` from String                 |