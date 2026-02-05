*** Settings ***
Documentation     API Regression Tests for Product Service - Non-Regression Testing
...               30 tests E2E pour valider la non-régression de l'API
...               Chaque test est INDÉPENDANT pour éviter les effets de cascade
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
    Log To Console    \nTest Suite initialized with RANDOM_ID: ${RANDOM_ID}

Create Product With SKU
    [Arguments]    ${sku_suffix}    ${stock}=100
    ${unique_sku}=    Set Variable    SKU-${RANDOM_ID}-${sku_suffix}
    ${product}=    Create Dictionary
    ...    name=Product ${sku_suffix} ${RANDOM_ID}
    ...    description=Test product for non-regression
    ...    price=99.99
    ...    stockQuantity=${stock}
    ...    category=Testing
    ...    sku=${unique_sku}
    ...    active=true
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    RETURN    ${response}

Create Order For Product
    [Arguments]    ${product_id}    ${customer_suffix}    ${quantity}=1
    ${order_item}=    Create Dictionary    productId=${product_id}    quantity=${quantity}
    ${items}=    Create List    ${order_item}
    ${order}=    Create Dictionary
    ...    customerName=Customer ${customer_suffix}
    ...    customerEmail=${customer_suffix}-${RANDOM_ID}@test.com
    ...    shippingAddress=123 Test Street ${customer_suffix}
    ...    items=${items}
    ${response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
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
    ${response}=    Create Product With SKU    test05
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    # Verify ID exists and is a positive number
    ${id}=    Set Variable    ${json['id']}
    Should Be True    ${id} > 0    Product ID should be positive

Test 06 - Create And Get Product By ID
    [Documentation]    Create a product then retrieve it by ID
    [Tags]    crud    product
    # Create product first
    ${create_response}=    Create Product With SKU    test06
    Should Be Equal As Strings    ${create_response.status_code}    201
    ${created}=    Set Variable    ${create_response.json()}
    ${product_id}=    Set Variable    ${created['id']}
    # Get product by ID
    ${response}=    GET On Session    api    ${API_PATH}/products/${product_id}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Contain    ${json['name']}    test06

Test 07 - Get All Products
    [Documentation]    Retrieve all products from the API
    [Tags]    crud    product
    ${response}=    GET On Session    api    ${API_PATH}/products
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    ${length}=    Get Length    ${json}
    Should Be True    ${length} >= 1

Test 08 - Create And Update Product
    [Documentation]    Create a product then update it
    [Tags]    crud    product
    # Create product first
    ${create_response}=    Create Product With SKU    test08
    Should Be Equal As Strings    ${create_response.status_code}    201
    ${created}=    Set Variable    ${create_response.json()}
    ${product_id}=    Set Variable    ${created['id']}
    # Update the product
    ${updated_product}=    Create Dictionary
    ...    name=Updated Product ${RANDOM_ID}
    ...    description=Updated by Robot Framework
    ...    price=149.99
    ...    stockQuantity=200
    ...    category=Updated
    ...    sku=SKU-${RANDOM_ID}-test08
    ...    active=true
    ${response}=    PUT On Session    api    ${API_PATH}/products/${product_id}    json=${updated_product}
    Should Be Equal As Strings    ${response.status_code}    200

Test 09 - Get Products By Category
    [Documentation]    Retrieve products filtered by category
    [Tags]    search    product
    ${response}=    GET On Session    api    ${API_PATH}/products/category/Testing
    Should Be Equal As Strings    ${response.status_code}    200

Test 10 - Search Products By Keyword
    [Documentation]    Search products using a keyword
    [Tags]    search    product
    ${response}=    GET On Session    api    ${API_PATH}/products/search    params=keyword=Product
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

Test 13 - Create And Delete Product
    [Documentation]    Create a product then delete it
    [Tags]    crud    product
    # Create product first
    ${create_response}=    Create Product With SKU    test13-delete
    Should Be Equal As Strings    ${create_response.status_code}    201
    ${created}=    Set Variable    ${create_response.json()}
    ${product_id}=    Set Variable    ${created['id']}
    # Delete the product
    ${response}=    DELETE On Session    api    ${API_PATH}/products/${product_id}
    Should Be Equal As Strings    ${response.status_code}    204

Test 14 - Get All Categories
    [Documentation]    Retrieve all product categories
    [Tags]    search    product
    ${response}=    GET On Session    api    ${API_PATH}/products/categories
    Should Be Equal As Strings    ${response.status_code}    200

# ==========================================
# ORDER TESTS (10 tests)
# ==========================================

Test 15 - Get All Orders
    [Documentation]    Retrieve all orders
    [Tags]    crud    order
    ${response}=    GET On Session    api    ${API_PATH}/orders
    Should Be Equal As Strings    ${response.status_code}    200

Test 16 - Create Order Successfully
    [Documentation]    Create a product and order it
    [Tags]    crud    order    critical
    # Create a product first
    ${product_response}=    Create Product With SKU    order16
    Should Be Equal As Strings    ${product_response.status_code}    201
    ${product}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product['id']}
    # Create order
    ${order_response}=    Create Order For Product    ${product_id}    order16    2
    Should Be Equal As Strings    ${order_response.status_code}    201
    ${order}=    Set Variable    ${order_response.json()}
    Should Be True    ${order['id']} > 0

Test 17 - Create And Get Order By ID
    [Documentation]    Create an order then retrieve it by ID
    [Tags]    crud    order
    # Create product and order
    ${product_response}=    Create Product With SKU    order17
    ${product}=    Set Variable    ${product_response.json()}
    ${order_response}=    Create Order For Product    ${product['id']}    order17
    ${order}=    Set Variable    ${order_response.json()}
    # Get order by ID
    ${response}=    GET On Session    api    ${API_PATH}/orders/${order['id']}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['customerName']}    Customer order17

Test 18 - Get Orders By Status
    [Documentation]    Retrieve orders filtered by status
    [Tags]    search    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/status/PENDING
    Should Be Equal As Strings    ${response.status_code}    200

Test 19 - Create Multiple Orders
    [Documentation]    Create multiple orders for same product
    [Tags]    crud    order
    # Create product
    ${product_response}=    Create Product With SKU    order19
    ${product}=    Set Variable    ${product_response.json()}
    # Create first order
    ${order1_response}=    Create Order For Product    ${product['id']}    order19a    1
    Should Be Equal As Strings    ${order1_response.status_code}    201
    # Create second order
    ${order2_response}=    Create Order For Product    ${product['id']}    order19b    2
    Should Be Equal As Strings    ${order2_response.status_code}    201

Test 20 - Order Contains Items
    [Documentation]    Verify order has items list
    [Tags]    validation    order
    # Create product and order
    ${product_response}=    Create Product With SKU    order20
    ${product}=    Set Variable    ${product_response.json()}
    ${order_response}=    Create Order For Product    ${product['id']}    order20    3
    ${order}=    Set Variable    ${order_response.json()}
    # Verify items
    Dictionary Should Contain Key    ${order}    items
    ${items}=    Set Variable    ${order['items']}
    ${length}=    Get Length    ${items}
    Should Be True    ${length} >= 1

Test 21 - Get Order Total
    [Documentation]    Verify order total calculation
    [Tags]    crud    order
    # Create product and order
    ${product_response}=    Create Product With SKU    order21
    ${product}=    Set Variable    ${product_response.json()}
    ${order_response}=    Create Order For Product    ${product['id']}    order21    2
    ${order}=    Set Variable    ${order_response.json()}
    # Get order total
    ${response}=    GET On Session    api    ${API_PATH}/orders/${order['id']}/total
    Should Be Equal As Strings    ${response.status_code}    200

Test 22 - Get Orders Count By Status
    [Documentation]    Verify order count by status endpoint
    [Tags]    validation    order
    ${response}=    GET On Session    api    ${API_PATH}/orders/count/PENDING
    Should Be Equal As Strings    ${response.status_code}    200

Test 23 - Cancel Order
    [Documentation]    Create and cancel an order
    [Tags]    crud    order
    # Create product and order
    ${product_response}=    Create Product With SKU    order23
    ${product}=    Set Variable    ${product_response.json()}
    ${order_response}=    Create Order For Product    ${product['id']}    order23
    ${order}=    Set Variable    ${order_response.json()}
    # Cancel order
    ${response}=    POST On Session    api    ${API_PATH}/orders/${order['id']}/cancel
    Should Be Equal As Strings    ${response.status_code}    204

Test 24 - Update Order Status
    [Documentation]    Create order and update its status
    [Tags]    crud    order
    # Create product and order
    ${product_response}=    Create Product With SKU    order24
    ${product}=    Set Variable    ${product_response.json()}
    ${order_response}=    Create Order For Product    ${product['id']}    order24
    ${order}=    Set Variable    ${order_response.json()}
    # Update status to CONFIRMED
    ${response}=    PATCH On Session    api    ${API_PATH}/orders/${order['id']}/status    params=status=CONFIRMED
    Should Be Equal As Strings    ${response.status_code}    200
    ${updated}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${updated['status']}    CONFIRMED

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

Test 29 - Products API Is Functional
    [Documentation]    Final verification that product API works
    [Tags]    validation    final
    # Create a product
    ${response}=    Create Product With SKU    final29
    Should Be Equal As Strings    ${response.status_code}    201
    # Get all products
    ${get_response}=    GET On Session    api    ${API_PATH}/products
    Should Be Equal As Strings    ${get_response.status_code}    200
    ${products}=    Set Variable    ${get_response.json()}
    ${length}=    Get Length    ${products}
    Should Be True    ${length} >= 1

Test 30 - Orders API Is Functional
    [Documentation]    Final verification that order API works
    [Tags]    validation    final
    # Create product and order
    ${product_response}=    Create Product With SKU    final30
    ${product}=    Set Variable    ${product_response.json()}
    ${order_response}=    Create Order For Product    ${product['id']}    final30
    Should Be Equal As Strings    ${order_response.status_code}    201
    # Get all orders
    ${get_response}=    GET On Session    api    ${API_PATH}/orders
    Should Be Equal As Strings    ${get_response.status_code}    200
    ${orders}=    Set Variable    ${get_response.json()}
    ${length}=    Get Length    ${orders}
    Should Be True    ${length} >= 1
