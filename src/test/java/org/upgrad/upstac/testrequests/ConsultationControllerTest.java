package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.exception.UpgradResponseStatusException;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequestQueryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsultationControllerTest {


    @Autowired
    ConsultationController consultationController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Autowired
    TestRequestUpdateService testRequestUpdateService;

    @Autowired
    LabRequestController labRequestController;

    @BeforeAll
    public void createTestRequest() {
        TestRequestCreateServiceTest trcst = new TestRequestCreateServiceTest ();
        TestRequest tr = trcst.getMockedTestRequest();
        tr = testRequestUpdateService.saveTestRequest(tr);
    }

    @Test
    @WithUserDetails(value = "tester")
    @Order(1)
    public void calling_updateLabTest_with_valid_test_request() {
        TestRequest tr = getTestRequestByStatus(RequestStatus.INITIATED);
        tr = labRequestController.assignForLabTest(tr.getRequestId());
        CreateLabResult clr = LabRequestControllerTest.getCreateLabResult(tr);
        tr = labRequestController.updateLabTest(tr.getRequestId(), clr);
        assertEquals(RequestStatus.LAB_TEST_COMPLETED, tr.getStatus());
    }

    @Test
    @WithUserDetails(value = "doctor")
    @Order(2)
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);

        TestRequest tr2 = consultationController.assignForConsultation(testRequest.getRequestId());

        assertThat(tr2.getRequestId(), equalTo(testRequest.getRequestId()) );
        assertThat(tr2.getStatus(), equalTo(RequestStatus.DIAGNOSIS_IN_PROCESS));
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "doctor")
    @Order(3)
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception(){
        Long InvalidRequestId= -34L;

        // Create an object of ResponseStatusException . Use assertThrows() method and pass assignForConsultation() method
        // of consultationController with InvalidRequestId as Id
        ResponseStatusException rse = assertThrows(ResponseStatusException.class,()->{ consultationController.assignForConsultation(InvalidRequestId);});
        assertThat(rse.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    @Order(6)
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        //Create CreateConsultationRequest and update the status of this object to be 'COMPLETED'
        CreateConsultationRequest cr = getCreateConsultationRequest(testRequest);
        TestRequest tr2 =  consultationController.updateConsultation(testRequest.getRequestId(), cr );

        //  1. the request ids of both the objects created should be same
        //  2. the status of the second object should be equal to 'COMPLETED'
        assertThat(testRequest.getRequestId(), equalTo(tr2.getRequestId()));
        assertThat(RequestStatus.COMPLETED, equalTo(tr2.getStatus()));
    }


    @Test
    @WithUserDetails(value = "doctor")
    @Order(4)
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        //Create an object of CreateConsultationRequest and call getCreateConsultationRequest() to create the object. Pass the above created object as the parameter
        CreateConsultationRequest cr = getCreateConsultationRequest(testRequest);

        // assert that invalid id throws an exception
        ResponseStatusException result =assertThrows( ResponseStatusException.class, () ->
            { consultationController.updateConsultation(-1L, cr ); } );

        //assert that  exception message should be contain the string "Invalid ID"
        assertThat( result.getMessage(), containsString("Invalid ID") );
    }

    @Test
    @WithUserDetails(value = "doctor")
    @Order(5)
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        CreateConsultationRequest cr = getCreateConsultationRequest(testRequest);
        cr.setSuggestion(null);

        // assert that an exception is thrown if suggestion is null
        ResponseStatusException result =assertThrows( ResponseStatusException.class, () ->
        { consultationController.updateConsultation(testRequest.getRequestId(), cr ); } );
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {

        //Create an object of CreateLabResult and set all the values
        // if the lab result test status is Positive, set the doctor suggestion as "HOME_QUARANTINE" and comments accordingly
        // else if the lab result status is Negative, set the doctor suggestion as "NO_ISSUES" and comments as "Ok"
        // Return the object
        CreateConsultationRequest ccr = new CreateConsultationRequest();

        TestStatus ts = testRequest.getLabResult().getResult();
        if(ts == TestStatus.POSITIVE) {
            ccr.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
            ccr.setComments(DoctorSuggestion.HOME_QUARANTINE.toString());
        } else {
            ccr.setSuggestion(DoctorSuggestion.NO_ISSUES);
            ccr.setComments("Ok");
        }
        return ccr;
    }

}