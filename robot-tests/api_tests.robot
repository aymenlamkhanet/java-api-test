*** Settings ***
Documentation     API Regression Tests for Product Service
Library           RequestsLibrary
Library           Collections
Library           JSONLibrary
Library           String
Suite Setup       Initialize Test Suite
Suite Teardown    Delete All Sessions

*** Variables ***
${BASE_URL}       http://localhost:8080
${API_PATH}       /api/v1
${RANDOM}         0

*** Keywords ***
Initialize Test Suite
    Create Session    api    ${BASE_URL}
    ${random_num}=    Generate Random String    8    [NUMBERS]
    Set Suite Variable    ${RANDOM}    ${random_num}

*** Test Cases ***

# ==========================================
# Health Check Tests
# ==========================================

Health Check Should Return UP
    [Documentation]    Verify that the application health endpoint returns UP status
    [Tags]    smoke    health
    ${response}=    GET On Session    api    /actuator/health
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['status']}    UP

Readiness Probe Should Return UP
    [Documentation]    Verify the Kubernetes readiness probe endpoint
    [Tags]    smoke    health
    ${response}=    GET On Session    api    /actuator/health/readiness
    Should Be Equal As Strings    ${response.status_code}    200

Liveness Probe Should Return UP
    [Documentation]    Verify the Kubernetes liveness probe endpoint
    [Tags]    smoke    health
    ${response}=    GET On Session    api    /actuator/health/liveness
    Should Be Equal As Strings    ${response.status_code}    200

Custom Health Endpoint Should Return Service Info
    [Documentation]    Verify custom health endpoint returns service information
    [Tags]    smoke    health
    ${response}=    GET On Session    api    ${API_PATH}/health
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['status']}    UP
    Dictionary Should Contain Key    ${json}    service
    Dictionary Should Contain Key    ${json}    version

# ==========================================
# Product CRUD Tests
# ==========================================

Create Product Successfully
    [Documentation]    Create a new product and verify it's saved correctly
    [Tags]    crud    product
    ${product}=    Create Dictionary
    ...    name=Test Robot Product
    ...    description=Product created by Robot Framework
    ...    price=99.99
    ...    stockQuantity=50
    ...    category=Testing
    ...    sku=ROBOT-001
    ...    active=true
    
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['name']}    Test Robot Product
    Should Be Equal As Strings    ${json['sku']}    ROBOT-001
    
    # Store product ID for later tests
    Set Suite Variable    ${CREATED_PRODUCT_ID}    ${json['id']}

Get Product By ID
    [Documentation]    Retrieve a product by its ID
    [Tags]    crud    product
    [Setup]    Create Test Product
    ${response}=    GET On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Not Be Empty    ${json['name']}

Get All Products
    [Documentation]    Retrieve all products from the API
    [Tags]    crud    product
    ${response}=    GET On Session    api    ${API_PATH}/products
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be True    len(${json}) >= 0

Update Product Successfully
    [Documentation]    Update an existing product
    [Tags]    crud    product
    [Setup]    Create Test Product
    
    ${updated_product}=    Create Dictionary
    ...    name=Updated Robot Product
    ...    description=Updated by Robot Framework
    ...    price=149.99
    ...    stockQuantity=100
    ...    category=Updated Testing
    ...    sku=ROBOT-UPD-001
    ...    active=true
    
    ${response}=    PUT On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}    json=${updated_product}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['name']}    Updated Robot Product

Delete Product Successfully
    [Documentation]    Delete a product from the system
    [Tags]    crud    product
    [Setup]    Create Test Product
    
    ${response}=    DELETE On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}
    Should Be Equal As Strings    ${response.status_code}    204
    
    # Verify product is deleted
    ${response}=    GET On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}    expected_status=404
    Should Be Equal As Strings    ${response.status_code}    404

# ==========================================
# Product Search & Filter Tests
# ==========================================

Get Products By Category
    [Documentation]    Retrieve products filtered by category
    [Tags]    search    product
    [Setup]    Create Test Product With Category
    
    ${response}=    GET On Session    api    ${API_PATH}/products/category/Electronics
    Should Be Equal As Strings    ${response.status_code}    200

Search Products By Keyword
    [Documentation]    Search products using a keyword
    [Tags]    search    product
    [Setup]    Create Test Product
    
    ${response}=    GET On Session    api    ${API_PATH}/products/search    params=keyword=Robot
    Should Be Equal As Strings    ${response.status_code}    200

Get Products By Price Range
    [Documentation]    Filter products by price range
    [Tags]    search    product
    
    ${response}=    GET On Session    api    ${API_PATH}/products/price-range    params=minPrice=10&maxPrice=200
    Should Be Equal As Strings    ${response.status_code}    200

Get Low Stock Products
    [Documentation]    Retrieve products with low stock
    [Tags]    search    product
    
    ${response}=    GET On Session    api    ${API_PATH}/products/low-stock    params=threshold=10
    Should Be Equal As Strings    ${response.status_code}    200

# ==========================================
# Stock Management Tests
# ==========================================

Add Stock To Product
    [Documentation]    Add stock to an existing product
    [Tags]    stock    product
    [Setup]    Create Test Product
    
    ${response}=    POST On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}/stock/add    params=quantity=25
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be True    ${json['stockQuantity']} >= 25

Remove Stock From Product
    [Documentation]    Remove stock from an existing product
    [Tags]    stock    product
    [Setup]    Create Test Product With High Stock
    
    ${response}=    POST On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}/stock/remove    params=quantity=10
    Should Be Equal As Strings    ${response.status_code}    200

Check Stock Availability
    [Documentation]    Check if product has sufficient stock
    [Tags]    stock    product
    [Setup]    Create Test Product With High Stock
    
    ${response}=    GET On Session    api    ${API_PATH}/products/${TEST_PRODUCT_ID}/stock/check    params=quantity=10
    Should Be Equal As Strings    ${response.status_code}    200
    Should Be Equal As Strings    ${response.text}    true

# ==========================================
# Order Tests
# ==========================================

Create Order Successfully
    [Documentation]    Create a new order with valid products
    [Tags]    crud    order
    [Setup]    Create Test Product With High Stock
    
    ${item}=    Create Dictionary
    ...    productId=${TEST_PRODUCT_ID}
    ...    quantity=2
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=Robot Test Customer
    ...    customerEmail=robot@test.com
    ...    items=${items}
    
    ${response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Should Not Be Empty    ${json['orderNumber']}
    Should Be Equal As Strings    ${json['status']}    PENDING
    
    Set Suite Variable    ${CREATED_ORDER_ID}    ${json['id']}

Get Order By ID
    [Documentation]    Retrieve an order by its ID
    [Tags]    crud    order
    Skip If    '${CREATED_ORDER_ID}' == '${EMPTY}'    No order was created
    
    ${response}=    GET On Session    api    ${API_PATH}/orders/${CREATED_ORDER_ID}
    Should Be Equal As Strings    ${response.status_code}    200
    ${json}=    Set Variable    ${response.json()}
    Should Be Equal As Strings    ${json['customerEmail']}    robot@test.com

Get All Orders
    [Documentation]    Retrieve all orders
    [Tags]    crud    order
    
    ${response}=    GET On Session    api    ${API_PATH}/orders
    Should Be Equal As Strings    ${response.status_code}    200

# ==========================================
# Error Handling Tests
# ==========================================

Get Non Existent Product Returns 404
    [Documentation]    Verify 404 response for non-existent product
    [Tags]    error    product
    
    ${response}=    GET On Session    api    ${API_PATH}/products/99999    expected_status=404
    Should Be Equal As Strings    ${response.status_code}    404

Create Product With Invalid Data Returns 400
    [Documentation]    Verify validation error for invalid product data
    [Tags]    error    product
    
    ${invalid_product}=    Create Dictionary
    ...    name=
    ...    price=-10
    ...    stockQuantity=-5
    ...    category=
    
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${invalid_product}    expected_status=400
    Should Be Equal As Strings    ${response.status_code}    400

# ==========================================
# End-to-End Workflow Tests (Non-Regression)
# ==========================================

Complete Order Workflow From Creation To Delivery
    [Documentation]    Full workflow: Create product -> Place order -> Confirm -> Ship -> Deliver
    [Tags]    e2e    workflow    regression    critical
    
    # Step 1: Create a product with stock
    ${product}=    Create Dictionary
    ...    name=E2E Workflow Product
    ...    description=Product for end-to-end workflow testing
    ...    price=150.00
    ...    stockQuantity=100
    ...    category=E2E Testing
    ...    sku=E2E-WORKFLOW-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${product_response.status_code}    201
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    ${initial_stock}=    Set Variable    ${product_json['stockQuantity']}
    
    # Step 2: Create an order with this product
    ${item}=    Create Dictionary
    ...    productId=${product_id}
    ...    quantity=5
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=E2E Test Customer
    ...    customerEmail=e2e@workflow.test
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${order_response.status_code}    201
    ${order_json}=    Set Variable    ${order_response.json()}
    ${order_id}=    Set Variable    ${order_json['id']}
    Should Be Equal As Strings    ${order_json['status']}    PENDING
    
    # Step 3: Confirm the order
    ${confirm_response}=    PUT On Session    api    ${API_PATH}/orders/${order_id}/confirm
    Should Be Equal As Strings    ${confirm_response.status_code}    200
    ${confirm_json}=    Set Variable    ${confirm_response.json()}
    Should Be Equal As Strings    ${confirm_json['status']}    CONFIRMED
    
    # Step 4: Ship the order
    ${ship_response}=    PUT On Session    api    ${API_PATH}/orders/${order_id}/ship
    Should Be Equal As Strings    ${ship_response.status_code}    200
    ${ship_json}=    Set Variable    ${ship_response.json()}
    Should Be Equal As Strings    ${ship_json['status']}    SHIPPED
    
    # Step 5: Deliver the order
    ${deliver_response}=    PUT On Session    api    ${API_PATH}/orders/${order_id}/deliver
    Should Be Equal As Strings    ${deliver_response.status_code}    200
    ${deliver_json}=    Set Variable    ${deliver_response.json()}
    Should Be Equal As Strings    ${deliver_json['status']}    DELIVERED
    
    # Step 6: Verify stock was decremented
    ${final_product}=    GET On Session    api    ${API_PATH}/products/${product_id}
    ${final_json}=    Set Variable    ${final_product.json()}
    Should Be True    ${final_json['stockQuantity']} == ${initial_stock} - 5

Order Cancellation Workflow
    [Documentation]    Test order cancellation and stock restoration
    [Tags]    e2e    workflow    regression    cancellation
    
    # Step 1: Create a product
    ${product}=    Create Dictionary
    ...    name=Cancellation Test Product
    ...    description=Product for cancellation testing
    ...    price=75.00
    ...    stockQuantity=50
    ...    category=Cancel Testing
    ...    sku=CANCEL-TEST-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${product_response.status_code}    201
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    ${initial_stock}=    Set Variable    ${product_json['stockQuantity']}
    
    # Step 2: Create an order
    ${item}=    Create Dictionary
    ...    productId=${product_id}
    ...    quantity=10
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=Cancel Test Customer
    ...    customerEmail=cancel@test.com
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${order_response.status_code}    201
    ${order_json}=    Set Variable    ${order_response.json()}
    ${order_id}=    Set Variable    ${order_json['id']}
    
    # Step 3: Cancel the order
    ${cancel_response}=    PUT On Session    api    ${API_PATH}/orders/${order_id}/cancel
    Should Be Equal As Strings    ${cancel_response.status_code}    200
    ${cancel_json}=    Set Variable    ${cancel_response.json()}
    Should Be Equal As Strings    ${cancel_json['status']}    CANCELLED
    
    # Step 4: Verify stock was restored
    ${final_product}=    GET On Session    api    ${API_PATH}/products/${product_id}
    ${final_json}=    Set Variable    ${final_product.json()}
    Should Be True    ${final_json['stockQuantity']} == ${initial_stock}

Order With Insufficient Stock Should Fail
    [Documentation]    Verify order fails when product has insufficient stock
    [Tags]    e2e    regression    stock    error
    
    # Step 1: Create a product with low stock
    ${product}=    Create Dictionary
    ...    name=Low Stock Product
    ...    description=Product with very low stock
    ...    price=200.00
    ...    stockQuantity=3
    ...    category=Stock Testing
    ...    sku=LOW-STOCK-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${product_response.status_code}    201
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    
    # Step 2: Try to order more than available stock
    ${item}=    Create Dictionary
    ...    productId=${product_id}
    ...    quantity=100
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=Stock Test Customer
    ...    customerEmail=stock@test.com
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}    expected_status=400
    Should Be Equal As Strings    ${order_response.status_code}    400

Order Status Transition Validation
    [Documentation]    Verify invalid order status transitions are rejected
    [Tags]    e2e    regression    status    validation
    
    # Step 1: Create a product and order
    ${product}=    Create Dictionary
    ...    name=Status Test Product
    ...    description=Product for status testing
    ...    price=100.00
    ...    stockQuantity=50
    ...    category=Status Testing
    ...    sku=STATUS-TEST-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${product_response.status_code}    201
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    
    ${item}=    Create Dictionary
    ...    productId=${product_id}
    ...    quantity=2
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=Status Test Customer
    ...    customerEmail=status@test.com
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${order_response.status_code}    201
    ${order_json}=    Set Variable    ${order_response.json()}
    ${order_id}=    Set Variable    ${order_json['id']}
    
    # Step 2: Try to ship before confirming (invalid transition)
    ${invalid_ship}=    PUT On Session    api    ${API_PATH}/orders/${order_id}/ship    expected_status=400
    Should Be Equal As Strings    ${invalid_ship.status_code}    400
    
    # Step 3: Try to deliver before shipping (invalid transition)
    ${invalid_deliver}=    PUT On Session    api    ${API_PATH}/orders/${order_id}/deliver    expected_status=400
    Should Be Equal As Strings    ${invalid_deliver.status_code}    400

Multiple Products In Single Order
    [Documentation]    Test ordering multiple different products in a single order
    [Tags]    e2e    regression    order
    
    # Step 1: Create first product
    ${product1}=    Create Dictionary
    ...    name=Multi Order Product 1
    ...    description=First product in multi-order
    ...    price=50.00
    ...    stockQuantity=100
    ...    category=Multi Order
    ...    sku=MULTI-1-${RANDOM}
    ...    active=true
    
    ${response1}=    POST On Session    api    ${API_PATH}/products    json=${product1}
    ${json1}=    Set Variable    ${response1.json()}
    ${product1_id}=    Set Variable    ${json1['id']}
    
    # Step 2: Create second product
    ${product2}=    Create Dictionary
    ...    name=Multi Order Product 2
    ...    description=Second product in multi-order
    ...    price=75.00
    ...    stockQuantity=80
    ...    category=Multi Order
    ...    sku=MULTI-2-${RANDOM}
    ...    active=true
    
    ${response2}=    POST On Session    api    ${API_PATH}/products    json=${product2}
    ${json2}=    Set Variable    ${response2.json()}
    ${product2_id}=    Set Variable    ${json2['id']}
    
    # Step 3: Create order with both products
    ${item1}=    Create Dictionary
    ...    productId=${product1_id}
    ...    quantity=3
    
    ${item2}=    Create Dictionary
    ...    productId=${product2_id}
    ...    quantity=2
    
    ${items}=    Create List    ${item1}    ${item2}
    
    ${order}=    Create Dictionary
    ...    customerName=Multi Product Customer
    ...    customerEmail=multi@order.test
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${order_response.status_code}    201
    ${order_json}=    Set Variable    ${order_response.json()}
    
    # Verify total is correct: (50*3) + (75*2) = 150 + 150 = 300
    Should Be True    ${order_json['totalAmount']} == 300.00

Get Orders By Customer Email
    [Documentation]    Retrieve all orders for a specific customer
    [Tags]    e2e    regression    order    search
    
    # Create a unique customer email
    ${unique_email}=    Set Variable    unique-${RANDOM}@customer.test
    
    # Create a product and order
    ${product}=    Create Dictionary
    ...    name=Customer Search Product
    ...    description=Product for customer search
    ...    price=80.00
    ...    stockQuantity=50
    ...    category=Search Testing
    ...    sku=SEARCH-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    
    ${item}=    Create Dictionary
    ...    productId=${product_id}
    ...    quantity=1
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=Search Customer
    ...    customerEmail=${unique_email}
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}
    Should Be Equal As Strings    ${order_response.status_code}    201
    
    # Search orders by customer email
    ${search_response}=    GET On Session    api    ${API_PATH}/orders/customer/${unique_email}
    Should Be Equal As Strings    ${search_response.status_code}    200
    ${search_json}=    Set Variable    ${search_response.json()}
    Should Be True    len(${search_json}) >= 1

Get Orders By Status
    [Documentation]    Retrieve orders filtered by status
    [Tags]    e2e    regression    order    filter
    
    ${response}=    GET On Session    api    ${API_PATH}/orders/status/PENDING
    Should Be Equal As Strings    ${response.status_code}    200

Product Activation And Deactivation
    [Documentation]    Test activating and deactivating products
    [Tags]    e2e    regression    product    status
    
    # Create an active product
    ${product}=    Create Dictionary
    ...    name=Activation Test Product
    ...    description=Product for activation testing
    ...    price=60.00
    ...    stockQuantity=30
    ...    category=Activation Testing
    ...    sku=ACTIVATE-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${product_response.status_code}    201
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    Should Be True    ${product_json['active']} == True
    
    # Deactivate the product
    ${deactivate_response}=    PUT On Session    api    ${API_PATH}/products/${product_id}/deactivate
    Should Be Equal As Strings    ${deactivate_response.status_code}    200
    ${deactivate_json}=    Set Variable    ${deactivate_response.json()}
    Should Be True    ${deactivate_json['active']} == False
    
    # Reactivate the product
    ${activate_response}=    PUT On Session    api    ${API_PATH}/products/${product_id}/activate
    Should Be Equal As Strings    ${activate_response.status_code}    200
    ${activate_json}=    Set Variable    ${activate_response.json()}
    Should Be True    ${activate_json['active']} == True

Order With Inactive Product Should Fail
    [Documentation]    Verify that orders cannot be placed for inactive products
    [Tags]    e2e    regression    product    validation
    
    # Create and deactivate a product
    ${product}=    Create Dictionary
    ...    name=Inactive Product Test
    ...    description=Product that will be deactivated
    ...    price=90.00
    ...    stockQuantity=40
    ...    category=Inactive Testing
    ...    sku=INACTIVE-${RANDOM}
    ...    active=true
    
    ${product_response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    ${product_json}=    Set Variable    ${product_response.json()}
    ${product_id}=    Set Variable    ${product_json['id']}
    
    # Deactivate the product
    ${deactivate_response}=    PUT On Session    api    ${API_PATH}/products/${product_id}/deactivate
    Should Be Equal As Strings    ${deactivate_response.status_code}    200
    
    # Try to order the inactive product
    ${item}=    Create Dictionary
    ...    productId=${product_id}
    ...    quantity=1
    
    ${items}=    Create List    ${item}
    
    ${order}=    Create Dictionary
    ...    customerName=Inactive Test Customer
    ...    customerEmail=inactive@test.com
    ...    items=${items}
    
    ${order_response}=    POST On Session    api    ${API_PATH}/orders    json=${order}    expected_status=400
    Should Be Equal As Strings    ${order_response.status_code}    400

# ==========================================
# Keywords
# ==========================================

*** Keywords ***

Create Test Product
    [Documentation]    Helper keyword to create a test product
    ${product}=    Create Dictionary
    ...    name=Test Product for Robot
    ...    description=Temporary test product
    ...    price=50.00
    ...    stockQuantity=25
    ...    category=Testing
    ...    sku=ROBOT-TEST-${RANDOM}
    ...    active=true
    
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Test Variable    ${TEST_PRODUCT_ID}    ${json['id']}

Create Test Product With Category
    [Documentation]    Helper keyword to create a test product with specific category
    ${product}=    Create Dictionary
    ...    name=Electronics Test Product
    ...    description=Electronics category product
    ...    price=199.99
    ...    stockQuantity=30
    ...    category=Electronics
    ...    sku=ROBOT-ELEC-${RANDOM}
    ...    active=true
    
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Test Variable    ${TEST_PRODUCT_ID}    ${json['id']}

Create Test Product With High Stock
    [Documentation]    Helper keyword to create a test product with high stock for testing
    ${product}=    Create Dictionary
    ...    name=High Stock Product
    ...    description=Product with high stock for testing
    ...    price=75.00
    ...    stockQuantity=1000
    ...    category=Stock Testing
    ...    sku=ROBOT-STOCK-${RANDOM}
    ...    active=true
    
    ${response}=    POST On Session    api    ${API_PATH}/products    json=${product}
    Should Be Equal As Strings    ${response.status_code}    201
    ${json}=    Set Variable    ${response.json()}
    Set Test Variable    ${TEST_PRODUCT_ID}    ${json['id']}
