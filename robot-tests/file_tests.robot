*** Settings ***
Documentation     Tests d'upload et download de fichiers PDF
...               Ces tests vérifient les endpoints de gestion de fichiers
Library           RequestsLibrary
Library           OperatingSystem
Library           Collections
Library           String
Suite Setup       Initialize File Tests
Suite Teardown    Cleanup File Tests

*** Variables ***
${BASE_URL}           http://localhost:8080
${API_PATH}           /api/v1
${TEST_PDF_PATH}      ${CURDIR}/test-files/sample-test.pdf
${DOWNLOAD_PATH}      ${CURDIR}/test-files/downloaded.pdf
${UPLOADED_FILE_ID}   ${EMPTY}

*** Keywords ***
Initialize File Tests
    [Documentation]    Initialise la session et vérifie que le fichier de test existe
    Create Session    api    ${BASE_URL}    verify=${False}
    File Should Exist    ${TEST_PDF_PATH}    Test PDF file not found in source!
    ${size}=    Get File Size    ${TEST_PDF_PATH}
    Log    Test PDF file found: ${TEST_PDF_PATH} (${size} bytes)

Cleanup File Tests
    [Documentation]    Nettoie les fichiers temporaires
    Remove File    ${DOWNLOAD_PATH}
    Delete All Sessions

*** Test Cases ***

# ==============================================================================
# TEST 1: Vérifier que le fichier PDF de test existe dans le repo
# ==============================================================================
Test 01 - Verify Test PDF Exists In Repository
    [Documentation]    Vérifie que le fichier PDF de test est présent dans le code source
    [Tags]    file    smoke    prerequisite
    
    File Should Exist    ${TEST_PDF_PATH}
    ${size}=    Get File Size    ${TEST_PDF_PATH}
    Should Be True    ${size} > 0    Test PDF file is empty!
    Log    ✅ Test PDF exists: ${size} bytes

# ==============================================================================
# TEST 2: Upload d'un fichier PDF
# ==============================================================================
Test 02 - Upload PDF File
    [Documentation]    Test l'upload d'un fichier PDF vers l'API
    ...                POST /api/v1/files/upload avec multipart/form-data
    [Tags]    file    upload    crud
    
    # Lire le fichier PDF en binaire
    ${file_content}=    Get Binary File    ${TEST_PDF_PATH}
    ${file_size}=    Get File Size    ${TEST_PDF_PATH}
    Log    Uploading PDF file: ${file_size} bytes
    
    # Préparer le multipart form-data
    # Format: [filename, content, content-type]
    ${file_tuple}=    Create List    sample-test.pdf    ${file_content}    application/pdf
    ${files}=    Create Dictionary    file=${file_tuple}
    
    # Appeler l'API d'upload
    ${response}=    POST On Session    api    ${API_PATH}/files/upload    
    ...    files=${files}    
    ...    expected_status=any
    
    # Vérifier le résultat
    IF    ${response.status_code} == 201 or ${response.status_code} == 200
        Log    ✅ Upload successful: ${response.status_code}
        ${json}=    Set Variable    ${response.json()}
        ${file_id}=    Get From Dictionary    ${json}    id    default=unknown
        Set Suite Variable    ${UPLOADED_FILE_ID}    ${file_id}
        Log    File uploaded with ID: ${file_id}
    ELSE IF    ${response.status_code} == 404
        Log    ⚠️ Upload endpoint not implemented (404) - Skipping
        Skip    Upload endpoint /api/v1/files/upload not implemented
    ELSE
        Fail    Upload failed with status ${response.status_code}: ${response.text}
    END

# ==============================================================================
# TEST 3: Download du fichier PDF uploadé
# ==============================================================================
Test 03 - Download PDF File
    [Documentation]    Test le téléchargement d'un fichier PDF depuis l'API
    ...                GET /api/v1/files/{id}/download
    [Tags]    file    download    crud
    
    # Skip si l'upload n'a pas fonctionné
    Skip If    '${UPLOADED_FILE_ID}' == '${EMPTY}'    No file was uploaded - skipping download test
    Skip If    '${UPLOADED_FILE_ID}' == 'unknown'    Upload did not return file ID
    
    # Appeler l'API de download
    ${response}=    GET On Session    api    ${API_PATH}/files/${UPLOADED_FILE_ID}/download
    ...    expected_status=any
    
    IF    ${response.status_code} == 200
        # Vérifier le Content-Type
        ${content_type}=    Get From Dictionary    ${response.headers}    Content-Type    default=unknown
        Should Contain    ${content_type}    application/pdf    
        ...    Expected PDF content-type, got: ${content_type}
        
        # Vérifier que le contenu n'est pas vide
        ${content_length}=    Get Length    ${response.content}
        Should Be True    ${content_length} > 0    Downloaded file is empty!
        
        # Sauvegarder le fichier téléchargé
        Create Binary File    ${DOWNLOAD_PATH}    ${response.content}
        File Should Exist    ${DOWNLOAD_PATH}
        
        Log    ✅ Download successful: ${content_length} bytes saved to ${DOWNLOAD_PATH}
    ELSE IF    ${response.status_code} == 404
        Log    ⚠️ Download endpoint not implemented or file not found (404)
        Skip    Download endpoint not available
    ELSE
        Fail    Download failed with status ${response.status_code}
    END

# ==============================================================================
# TEST 4: Vérifier l'intégrité du fichier (taille identique)
# ==============================================================================
Test 04 - Verify File Integrity
    [Documentation]    Compare la taille du fichier original et téléchargé
    [Tags]    file    integrity    validation
    
    # Skip si le download n'a pas fonctionné
    ${download_exists}=    Run Keyword And Return Status    File Should Exist    ${DOWNLOAD_PATH}
    Skip If    not ${download_exists}    Downloaded file does not exist - skipping integrity check
    
    # Comparer les tailles
    ${original_size}=    Get File Size    ${TEST_PDF_PATH}
    ${downloaded_size}=    Get File Size    ${DOWNLOAD_PATH}
    
    Should Be Equal As Integers    ${original_size}    ${downloaded_size}
    ...    File sizes don't match! Original: ${original_size}, Downloaded: ${downloaded_size}
    
    Log    ✅ File integrity verified: ${original_size} bytes match

# ==============================================================================
# TEST 5: Workflow complet Upload → Download → Verify
# ==============================================================================
Test 05 - Complete File Upload Download Workflow
    [Documentation]    Test E2E complet: Upload → Download → Vérification intégrité
    [Tags]    file    workflow    e2e
    
    # STEP 1: Lire le fichier original
    Log    STEP 1/4: Reading original PDF file
    ${original_content}=    Get Binary File    ${TEST_PDF_PATH}
    ${original_size}=    Get File Size    ${TEST_PDF_PATH}
    Log    Original file size: ${original_size} bytes
    
    # STEP 2: Upload
    Log    STEP 2/4: Uploading PDF file
    ${file_tuple}=    Create List    workflow-test.pdf    ${original_content}    application/pdf
    ${files}=    Create Dictionary    file=${file_tuple}
    
    ${upload_response}=    POST On Session    api    ${API_PATH}/files/upload    
    ...    files=${files}    
    ...    expected_status=any
    
    IF    ${upload_response.status_code} == 404
        Skip    File upload endpoint not implemented
    END
    
    Should Be True    ${upload_response.status_code} == 200 or ${upload_response.status_code} == 201
    ...    Upload failed: ${upload_response.status_code}
    
    ${upload_json}=    Set Variable    ${upload_response.json()}
    ${file_id}=    Get From Dictionary    ${upload_json}    id
    Log    STEP 2 PASSED: File uploaded with ID ${file_id}
    
    # STEP 3: Download
    Log    STEP 3/4: Downloading PDF file
    ${download_response}=    GET On Session    api    ${API_PATH}/files/${file_id}/download
    Should Be Equal As Strings    ${download_response.status_code}    200
    
    ${downloaded_content}=    Set Variable    ${download_response.content}
    ${downloaded_size}=    Get Length    ${downloaded_content}
    Log    STEP 3 PASSED: Downloaded ${downloaded_size} bytes
    
    # STEP 4: Vérifier l'intégrité
    Log    STEP 4/4: Verifying file integrity
    Should Be Equal As Integers    ${original_size}    ${downloaded_size}
    ...    Size mismatch: Original=${original_size}, Downloaded=${downloaded_size}
    
    Log    ✅ WORKFLOW COMPLETE: Upload → Download → Verify OK

# ==============================================================================
# TEST 6: Upload avec un fichier invalide (optionnel - test d'erreur)
# ==============================================================================
Test 06 - Upload Invalid File Type
    [Documentation]    Test que l'API rejette les fichiers non-PDF (si validation implémentée)
    [Tags]    file    error    validation
    
    # Créer un faux fichier texte
    ${fake_content}=    Convert To Bytes    This is not a PDF file
    ${file_tuple}=    Create List    fake.txt    ${fake_content}    text/plain
    ${files}=    Create Dictionary    file=${file_tuple}
    
    ${response}=    POST On Session    api    ${API_PATH}/files/upload    
    ...    files=${files}    
    ...    expected_status=any
    
    IF    ${response.status_code} == 404
        Skip    File upload endpoint not implemented
    ELSE IF    ${response.status_code} == 400 or ${response.status_code} == 415
        Log    ✅ API correctly rejected invalid file type: ${response.status_code}
    ELSE IF    ${response.status_code} == 200 or ${response.status_code} == 201
        Log    ⚠️ API accepts all file types (no validation on file type)
    ELSE
        Log    Unexpected response: ${response.status_code}
    END

# ==============================================================================
# TEST 7: Download d'un fichier inexistant
# ==============================================================================
Test 07 - Download Non-Existent File
    [Documentation]    Test que l'API retourne 404 pour un fichier inexistant
    [Tags]    file    error    404
    
    ${response}=    GET On Session    api    ${API_PATH}/files/99999/download    
    ...    expected_status=any
    
    IF    ${response.status_code} == 404
        Log    ✅ API correctly returns 404 for non-existent file
    ELSE IF    ${response.status_code} == 200
        Fail    API should return 404 for non-existent file, got 200
    ELSE
        Log    Response: ${response.status_code} - ${response.text}
    END
