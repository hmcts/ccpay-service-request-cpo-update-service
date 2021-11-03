package uk.gov.hmcts.reform.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoUnitTest {

    @InjectMocks
    RootController rootController = new RootController();

    @Test
    void exampleOfTest() {
        assertTrue(System.currentTimeMillis() > 0, "Example of Unit Test");
    }

    @Test
    void testRootController() {
        ResponseEntity<String> response = rootController.welcome();
        assertEquals(200,response.getStatusCode().value(),"Status code is not same");
        assertEquals("Welcome to CPO update service",response.getBody(),"Value is not same");
    }
}
