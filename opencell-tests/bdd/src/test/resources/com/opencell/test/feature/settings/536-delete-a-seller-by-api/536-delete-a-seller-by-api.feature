Feature:  Delete seller Plan by API

Background: The classic offer is already executed
            Create seller Plan by API is already executed

@admin @superadmin
Scenario Outline: Delete a seller Plan by API
		Given  The entity has the following information "<jsonFile>" as "<dto>"
		When   I call the delete "<api>"
		Then   The entity is deleted
		And    Validate that the statusCode is "<statusCode>"
		And    The status is "<status>"
		And    The message  is "<message>"
		And    The errorCode  is "<errorCode>"


Examples: 
    |jsonFile                                                       |    dto    |   api   |statusCode|status  |errorCode                        |message|
    |settings/111-create-a-seller-by-api/Success.json               |SellerDto  |/seller/ |200       |SUCCESS |                                 |       |
    |settings/536-delete-a-seller-by-api/ENTITY_DOES_NOT_EXIST.json |SellerDto  |/seller/ |404       |FAIL    |ENTITY_DOES_NOT_EXISTS_EXCEPTION |Seller with code=NOT_EXIST does not exists.|
