package com.kvs.core.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(RequestAndRepositoryLoggingAspectTest.TestBeans.class)
@ExtendWith(OutputCaptureExtension.class)
class RequestAndRepositoryLoggingAspectTest {

    @Autowired
    private LoggingTestController controller;

    @Autowired
    private TestRepository testRepository;

    private final Logger aspectLogger =
            (Logger) LoggerFactory.getLogger(RequestAndRepositoryLoggingAspect.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureAspectLogs() {
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    void stopCapturingAndClearRequest() {
        aspectLogger.detachAppender(appender);
        appender.stop();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void controllerProxyLogsRequestUrlAndRedactedBody() {
        assertTrue(AopUtils.isAopProxy(controller));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/logging");
        request.setServerName("localhost");
        request.setServerPort(8082);
        request.setQueryString("tenantId=tenant-secret&access_token=query-secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String response = controller.accept(new LoggingRequest("safe-user", "body-secret"));

        assertEquals("accepted:safe-user", response);
        String logs = capturedLogs();
        assertTrue(logs.contains("POST http://localhost:8082/test/logging"));
        assertTrue(logs.contains("tenantId=[REDACTED]"));
        assertTrue(logs.contains("access_token=[REDACTED]"));
        assertTrue(logs.contains("safe-user"));
        assertTrue(logs.contains("[REDACTED]"));
        assertFalse(logs.contains("body-secret"));
        assertFalse(logs.contains("query-secret"));
    }

    @Test
    void repositoryProxyLogsReturnedDataWithSensitiveFieldsRedacted() {
        assertTrue(AopUtils.isAopProxy(testRepository));

        List<RepositoryResult> results = testRepository.findResults();

        assertEquals(1, results.size());
        assertEquals("db-user", results.get(0).username());
        String logs = capturedLogs();
        assertTrue(logs.contains("Repository result"));
        assertTrue(logs.contains("findResults"));
        assertTrue(logs.contains("db-user"));
        assertTrue(logs.contains("[REDACTED]"));
        assertFalse(logs.contains("database-token-secret"));
    }

    private String capturedLogs() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + System.lineSeparator() + right);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        LoggingTestController loggingTestController() {
            return new LoggingTestController();
        }

        @Bean
        TestRepository testRepository() {
            return new TestRepository();
        }
    }

    @RestController
    static class LoggingTestController {

        @PostMapping("/test/logging")
        String accept(@RequestBody LoggingRequest request) {
            return "accepted:" + request.username();
        }
    }

    static class TestRepository {

        List<RepositoryResult> findResults() {
            return List.of(new RepositoryResult("db-user", "database-token-secret"));
        }
    }

    record LoggingRequest(String username, String password) {
    }

    record RepositoryResult(String username, String apiToken) {
    }
}
