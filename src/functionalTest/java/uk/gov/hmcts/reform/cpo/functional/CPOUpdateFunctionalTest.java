package uk.gov.hmcts.reform.cpo.functional;

import org.junit.jupiter.api.TestInstance;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles({"functional", "liberataMock"})
@ContextConfiguration(classes = TestContextConfiguration.class)
@RunWith(SpringIntegrationSerenityRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
public class CPOUpdateFunctionalTest {

    @BeforeAll
    public void setUp() throws Exception {

    }

    @Test
    public void cpo_update() {

     }

}
