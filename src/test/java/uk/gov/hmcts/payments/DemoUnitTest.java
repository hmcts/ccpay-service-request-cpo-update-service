package uk.gov.hmcts.payments;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.payments.controllers.RootController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DemoUnitTest {

    @Autowired
    RootController rootController;

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
