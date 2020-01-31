@payments
Feature: Create Payment Schedule Template by API

  Background: The system is configured
    Classic offer is executed

  @admin @superadmin
  Scenario Outline: Create Payment Schedule Template by API
    Given The entity has the following information "<jsonFile>" as "<dto>"
    When I call the "<api>"
    Then The payment schedule template is created
    And Validate that the statusCode is "<statusCode>"
    And The status is "<status>"
    And The message  is "<message>"
    And The errorCode  is "<errorCode>"

    Examples: 
      | jsonFile                                                                                               | dto                        | api                                             | statusCode | status  | errorCode                        | message                                                                                                                                                                                                                                                           |
      | payments/00005-paymentScheduleTemplate-api-create/SuccessTest.json                                     | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        200 | SUCCESS |                                  |                                                                                                                                                                                                                                                                   |
      | payments/00005-paymentScheduleTemplate-api-create/SuccessTest.json                                     | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/               |        403 | FAIL    | ENTITY_ALREADY_EXISTS_EXCEPTION  | PaymentScheduleTemplate with code=TEST already exists.                                                                                                                                                                                                            |
      | payments/00005-paymentScheduleTemplate-api-create/SuccessTest1.json                                    | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        200 | SUCCESS |                                  |                                                                                                                                                                                                                                                                   |
      | payments/00005-paymentScheduleTemplate-api-create/MISSING_PARAMETER.json                               | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        400 | FAIL    | MISSING_PARAMETER                | The following parameters are required or contain invalid values: code, calendarCode, serviceTemplateCode, paymentDayInMonth, amount, paymentLabel, advancePaymentInvoiceTypeCode, advancePaymentInvoiceSubCategoryCode, generateAdvancePaymentInvoice, doPayment. |
      | payments/00005-paymentScheduleTemplate-api-create/INVALID_PARAMETER_amount.json                        | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        400 | FAIL    | INVALID_PARAMETER                | Cannot deserialize value of type `java.lang.Integer` from String                                                                                                                                                                                                  |
      | payments/00005-paymentScheduleTemplate-api-create/INVALID_PARAMETER_doPayment.json                     | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        400 | FAIL    | INVALID_PARAMETER                | Cannot deserialize value of type `java.lang.Boolean` from String                                                                                                                                                                                                  |
      | payments/00005-paymentScheduleTemplate-api-create/INVALID_PARAMETER_generateAdvancePaymentInvoice.json | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        400 | FAIL    | INVALID_PARAMETER                | Cannot deserialize value of type `java.math.BigDecimal` from String                                                                                                                                                                                               |
      | payments/00005-paymentScheduleTemplate-api-create/INVALID_PARAMETER_paymentDateInMonth.json            | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        400 | FAIL    | INVALID_PARAMETER                | Cannot deserialize value of type `java.lang.Integer` from String                                                                                                                                                                                                  |
      | payments/00005-paymentScheduleTemplate-api-create/ENTITY_DOES_NOT_EXIST_advPaymentInvSubCat.json       | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        404 | FAIL    | ENTITY_DOES_NOT_EXISTS_EXCEPTION | InvoiceSubCategory with code=NOT_EXIST does not exists.                                                                                                                                                                                                           |
      | payments/00005-paymentScheduleTemplate-api-create/ENTITY_DOES_NOT_EXIST_advPaymentInvType.json         | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        404 | FAIL    | ENTITY_DOES_NOT_EXISTS_EXCEPTION | InvoiceType with code=NOT_EXIST does not exists.                                                                                                                                                                                                                  |
      | payments/00005-paymentScheduleTemplate-api-create/ENTITY_DOES_NOT_EXIST_calendarCode.json              | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        404 | FAIL    | ENTITY_DOES_NOT_EXISTS_EXCEPTION | Calendar with code=NOT_EXIST does not exists.                                                                                                                                                                                                                     |
      | payments/00005-paymentScheduleTemplate-api-create/ENTITY_DOES_NOT_EXIST_serviceTemplate.json           | PaymentScheduleTemplateDto | /payment/paymentScheduleTemplate/createOrUpdate |        404 | FAIL    | ENTITY_DOES_NOT_EXISTS_EXCEPTION | ServiceTemplate with code=NOT_EXIST does not exists.                                                                                                                                                                                                              |