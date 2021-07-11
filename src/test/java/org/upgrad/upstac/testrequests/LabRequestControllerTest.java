package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.exception.UpgradResponseStatusException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.LabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabRequestControllerTest {

    @Autowired
    LabRequestController labRequestController;

    @Autowired
    TestRequestUpdateService testRequestUpdateService;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Autowired
    TestRequestRepository testRequestRepository;

    @BeforeAll
    public void createTestRequest() {
        TestRequestCreateServiceTest trcst = new TestRequestCreateServiceTest ();
        TestRequest tr = trcst.getMockedTestRequest();
        testRequestUpdateService.saveTestRequest(tr);
    }

    @Test
    @WithUserDetails(value = "tester")
    @Order(1)
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);

        // assign test for lab testing and assert that status is lab test in progress
        TestRequest tr2 = labRequestController.assignForLabTest(testRequest.getRequestId());

        assertEquals(testRequest.getRequestId(), tr2.getRequestId());
        assertEquals( tr2.getStatus(), RequestStatus.LAB_TEST_IN_PROGRESS);
        assertNotNull(tr2.getLabResult());
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "tester")
    @Order(2)
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){

        Long InvalidRequestId= -34L;

        // assert that exception if Id is invalid
        AppException  rse =  assertThrows( AppException.class, ()->
        { labRequestController.assignForLabTest(InvalidRequestId); } );

        assertThat(rse.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    @Order(5)
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);

        CreateLabResult clr = getCreateLabResult(testRequest);

        // Update the lab test to complete and assert that the status is lab test completed
        TestRequest tr2  = labRequestController.updateLabTest(testRequest.getRequestId(), clr);

        assertEquals(testRequest.getRequestId(), tr2.getRequestId());
        assertEquals(RequestStatus.LAB_TEST_COMPLETED, tr2.getStatus());
    }


    @Test
    @WithUserDetails(value = "tester")
    @Order(3)
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);

        // Create an object of CreateLabResult and call getCreateLabResult() to create the object
        // assert that exception is thrown if id is invalid
        CreateLabResult clr = getCreateLabResult(testRequest);

        UpgradResponseStatusException rse = assertThrows(UpgradResponseStatusException.class, () ->
                                        {labRequestController.updateLabTest(-1L, clr);});
        assertThat(rse.getMessage() , containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    @Order(4)
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);

        CreateLabResult clr = getCreateLabResult(testRequest);
        clr.setResult(null);

        // assert that exception is thrown if result is null
        UpgradResponseStatusException rse = assertThrows(UpgradResponseStatusException.class, () ->
        {labRequestController.updateLabTest(testRequest.getRequestId(), clr);});

        assertThat(rse.getMessage(), containsString("ConstraintViolationException"));
    }

    public static CreateLabResult getCreateLabResult(TestRequest testRequest) {
        LabResult lr = new LabResult();
        lr.setBloodPressure("100.00");
        lr.setComments("NA");
        lr.setResult(TestStatus.POSITIVE);
        lr.setTemperature("97.7");
        lr.setOxygenLevel("94");
        lr.setHeartBeat("120");
        testRequest.setLabResult(lr);

        //Create an object of CreateLabResult and set all the values
        // Return the object
        CreateLabResult clr = new CreateLabResult();
        clr.setResult(lr.getResult());
        clr.setComments(lr.getComments());
        clr.setBloodPressure(lr.getBloodPressure());
        clr.setHeartBeat(lr.getHeartBeat());
        clr.setTemperature(lr.getTemperature());
        clr.setOxygenLevel(lr.getOxygenLevel());

        return clr; // Replace this line with your code
    }
}