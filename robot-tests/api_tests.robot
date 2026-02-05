*** Settings ***
Documentation     API Regression Tests for Product Service - Non-Regression Testing
...               30 tests E2E pour valider la non-régression de l'API
Library           RequestsLibrary
Library           Collections
Library           String
Suite Setup       Initialize Test Suite
Suite Teardown    Delete All Sessions

*** Variables ***
${BASE_URL}       http://localhost:8080
${API_PATH}       /api/v1
${RANDOM_ID}      0

*** Keywords ***
Initialize Test Suite
    Create Session    api    ${BASE_URL}
    ${random_num}=    Generate Random String    8    [NUMBERS]
    Set Suite Variable    ${RANDOM_ID}    ${random_num}

Create Unique Product
    [Arguments]    ${suffix}=default
    ${unique_sku}=    Set Variable    SKU-${RANDOM_ID}-${suffix}
    ${product}=    Create Dictionary
    ...    name=Product ${suffix} ${RANDOM_ID}
    ...    description=Test product for non-regression
    ...    price=99.99
    ...    stockQuantity=100
    ...    category=Testing
    ...    sku=${unique_sku}
    ...    active=true
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    RETURN    ${response}

*** Test Cases ***

# ==========================================
# HEALTH CHECK TESTS (4 tests)
# ==========================================

Test 01 - Health Check Should Return UP
    [Documentation]    Verify that the application health endpoint returns UP status
    [Tags]    smoke    health    critical
    ${response}=    GET On Session    api    /actuator/health
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['status']}    UP

Test 02 - Readiness Probe Should Return UP
    [Documentation]    Verify the Kubernetes readiness probe endpoint
    [Tags]    smoke    health
    ${response}=    GET On Session    api    /actuator/health/readiness
    Should Be Equal As Strings    ${response.status_code}    200

Test 03 - Liveness Probe Should Return UP
    [Documentation]    Verify the Kubernetes liveness probe endpoint
    [Tags]    smoke    health
    ${response}=    GET On Session    api    /actuator/health/liveness
    Should Be Equal As Strings    ${response.status_code}    200

Test 04 - Custom Health Endpoint Returns Service Info
    [Documentation]    Verify custom health endpoint returns service information
    [Tags]    smoke    health
    ${response}=    GET On Session    api    ${API_PATH}/health
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['status']}    UP

# ==========================================
# PRODUCT CRUD TESTS (10 tests)
# ==========================================

Test 05 - Create Product Successfully
    [Documentation]    Create a new product and verify it's saved correctly
    [Tags]    crud    product    critical
    ${response}=    Create Unique Product    test05
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Should Not Be Empty    ${json['id']}
    Set Suite Variable    ${PRODUCT_ID_05}    ${json['id']}

Test 06 - Get Product By ID
    [Documentation]    Retrieve a product by its ID
    [Tags]    crud    product
    ${response}=    GET On Session    api    ${API_PATH}/products/${PRODUCT_ID_05}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Not Be Empty    ${json['name']}

Test 07 - Get All Products
    [Documentation]    Retrieve all products from the API
    [Tags]    crud    product
    ${response}=    GET On Session    api    ${API_PATH}/products
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be True    len(${json}) >= 1

Test 08 - Update Product Successfully
    [Documentation]    Update an existing product
    [Tags]    crud    product
    ${updated_product}=    Create Dictionary
    ...    name=Updated Product ${RANDOM_ID}
    ...    description=Updated by Robot Framework
    ...    price=149.99
    ...    stockQuantity=200
    ...    category=Updated
    ...    sku=SKU-${RANDOM_ID}-test05
    ...    active=true
    ${response}=    PUT On Session    api    ${API_PATH}/products/${PRODUCT_ID_05}    json=${updated_product}
    Should Be Equal As Strings    ${response.status_code}    200

Test 09 - Get Products By Category
    [Documentation]    Retrieve products filtered by category
    [Tags]    search    product
    ${response}=    GET On Session    api    ${API_PATH}/products/category/Updated
    Should Be Equal As Strings    ${response.status_code}    200

Test 10 - Search Products By Keyword
    [Documentation]    Search products using a keyword
    [Tags]    search    product
    ${response}=    GET On Session    api    ${API_PATH}/products/search    params=keyword=Updated
    Should Be Equal As Strings    ${response.status_code}    200

Test 11 - Get Products By Price Range
    [Documentation]    Filter products by price range
    [Tags]    search    product
    ${response}=    GET On Session    api    ${API_PATH}/products/price-range    params=minPrice=10&maxPrice=500
    Should Be Equal As Strings    ${response.status_code}    200

Test 12 - Get Low Stock Products
    [Documentation]    Retrieve products with low stock (threshold 10)
    [Tags]    inventory    product
    ${response}=    GET On Session    api    ${API_PATH}/products/low-stock    params=threshold=10
    Should Be Equal As Strings    ${response.status_code}    200

Test 13 - Create Second Product For Testing
    [Documentation]    Create another product for additional tests
    [Tags]    crud    product
    ${response}=    Create Unique Product    test13
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Suite Variable    ${PRODUCT_ID_13}    ${json['id']}

Test 14 - Delete Product Successfully
    [Documentation]    Delete a product from the system
    [Tags]    crud    product
    ${response}=    DELETE On Session    api    ${API_PATH}/products/${PRODUCT_ID_13}
    Should Be Equal As Strings    ${response.status_code}    204

# ==========================================
# ORDER TESTS (10 tests)
# ==========================================

Test 15 - Get All Orders
    [Documentation]    Retrieve all orders
    [Tags]    crud    order
    ${response}=    GET On Session    api    ${API_PATH}/orders
    Should Be Equal As Strings    ${response.status_code}    200

Test 16 - Create Order Successfully
    [Documentation]    Create a new order with valid product
    [Tags]    crud    order    critical
    ${order_item}=    Create Dictionary    productId=${PRODUCT_ID_05}    quantity=2
    ${items}=    Create List    ${order_item}
    ${order}=    Create Dictionary
    ...    customerName=Robot Test Customer
    ...    customerEmail=robot-${RANDOM_ID}@test.com
    ...    shippingAddress=123 Test Street
    ...    items=${items}
    ${response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Suite Variable    ${ORDER_ID}    ${json['id']}

Test 17 - Get Order By ID
    [Documentation]    Retrieve an order by its ID
    [Tags]    crud    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/${ORDER_ID}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['customerName']}    Robot Test Customer

Test 18 - Get Orders By Status
    [Documentation]    Retrieve orders filtered by status
    [Tags]    search    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/status/PENDING
    Should Be Equal As Strings    ${response.status_code}    200

Test 19 - Create Another Order
    [Documentation]    Create another order for testing
    [Tags]    crud    order
    ${order_item}=    Create Dictionary    productId=${PRODUCT_ID_05}    quantity=1
    ${items}=    Create List    ${order_item}
    ${order}=    Create Dictionary
    ...    customerName=Second Customer
    ...    customerEmail=second-${RANDOM_ID}@test.com
    ...    shippingAddress=456 Another Street
    ...    items=${items}
    ${response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Suite Variable    ${ORDER_ID_2}    ${json['id']}

Test 20 - Verify Order Contains Items
    [Documentation]    Verify order has items list
    [Tags]    validation    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/${ORDER_ID}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Dictionary Should Contain Key    ${json}    items

Test 21 - Multiple Orders Exist
    [Documentation]    Verify multiple orders can be retrieved
    [Tags]    crud    order
    ${response}=    GET On Session    api    ${API_PATH}/orders
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be True    len(${json}) >= 2

Test 22 - Order Has Correct Customer Info
    [Documentation]    Verify order customer information
    [Tags]    validation    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/${ORDER_ID_2}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['customerName']}    Second Customer

Test 23 - Create Third Product
    [Documentation]    Create product for additional order tests
    [Tags]    crud    product
    ${response}=    Create Unique Product    test23
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Suite Variable    ${PRODUCT_ID_23}    ${json['id']}

Test 24 - Create Order With Different Product
    [Documentation]    Create order with a different product
    [Tags]    crud    order
    ${order_item}=    Create Dictionary    productId=${PRODUCT_ID_23}    quantity=3
    ${items}=    Create List    ${order_item}
    ${order}=    Create Dictionary
    ...    customerName=Third Customer
    ...    customerEmail=third-${RANDOM_ID}@test.com
    ...    shippingAddress=789 Third Avenue
    ...    items=${items}
    ${response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${response.status_code}    201

# ==========================================
# ERROR HANDLING TESTS (4 tests)
# ==========================================

Test 25 - Get Non Existent Product Returns 404
    [Documentation]    Verify 404 response for non-existent product
    [Tags]    error    product
    ${response}=    GET On Session    api    ${API_PATH}/products/99999    expected_status=404
    Should Be Equal As Strings    ${response.status_code}    404

Test 26 - Create Product With Invalid Data Returns 400
    [Documentation]    Verify validation errors for invalid product
    [Tags]    error    product
    ${invalid_product}=    Create Dictionary
    ...    name=
    ...    price=-10
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${invalid_product}    expected_status=400
    Should Be Equal As Strings    ${response.status_code}    400

Test 27 - Get Non Existent Order Returns 404
    [Documentation]    Verify 404 response for non-existent order
    [Tags]    error    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/99999    expected_status=404
    Should Be Equal As Strings    ${response.status_code}    404

Test 28 - Actuator Info Endpoint Works
    [Documentation]    Verify actuator info endpoint
    [Tags]    smoke    actuator
    ${response}=    GET On Session    api    /actuator/info
    Should Be Equal As Strings    ${response.status_code}    200

# ==========================================
# FINAL VALIDATION TESTS (2 tests)
# ==========================================

Test 29 - Products Count Increased After Tests
    [Documentation]    Verify products were created during tests
    [Tags]    validation    final
    ${response}=    GET On Session    api    ${API_PATH}/products
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be True    len(${json}) >= 2

Test 30 - Orders Count Increased After Tests
    [Documentation]    Verify orders were created during tests
    [Tags]    validation    final
    ${response}=    GET On Session    api    ${API_PATH}/orders
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be True    len(${json}) >= 3
