package com.vpn.common.security.aspect;
import com.vpn.common.exception.ForbiddenException;
import com.vpn.common.exception.UnauthorizedException;
import com.vpn.common.security.annotations.*;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.context.SecurityContext;
import com.vpn.common.security.context.SecurityContextHolder;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        SecurityAspectTest.TestConfig.class,
        SecurityAspect.class
})
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SecurityAspectTest {

    @Autowired
    private TestSecurityService testService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("@Public: доступ БЕЗ авторизации")
    void testPublicAccessWithoutAuth() {
        assertDoesNotThrow(() -> testService.publicMethod());
    }

    @Test
    @DisplayName("@Public: доступ С авторизацией")
    void testPublicAccessWithAuth() {
        setUserContext(123456789L);

        assertDoesNotThrow(() -> testService.publicMethod());
    }

    @Test
    @DisplayName("@RequireUser: успешный доступ для USER")
    void testRequireUser_Success() {
        setUserContext(123456789L);

        String result = testService.userMethod();
        assertEquals("user-ok", result);
    }

    @Test
    @DisplayName("@RequireUser: доступ для ADMIN (разрешено)")
    void testRequireUser_AdminAllowed() {
        setAdminContext(999999L);

        String result = testService.userMethod();
        assertEquals("user-ok", result);
    }

    @Test
    @DisplayName("@RequireUser: отказ БЕЗ авторизации")
    void testRequireUser_Unauthorized() {

        assertThrows(UnauthorizedException.class, () -> {
            testService.userMethod();
        });
    }

    @Test
    @DisplayName("@RequireUser: отказ для SERVICE")
    void testRequireUser_ServiceDenied() {
        setServiceContext();

        assertThrows(ForbiddenException.class, () -> {
            testService.userMethod();
        });
    }

    @Test
    @DisplayName("@RequireAdmin: успешный доступ для ADMIN")
    void testRequireAdmin_Success() {
        setAdminContext(999999L);

        String result = testService.adminMethod();
        assertEquals("admin-ok", result);
    }

    @Test
    @DisplayName("@RequireAdmin: отказ для USER")
    void testRequireAdmin_UserDenied() {
        setUserContext(123456789L);

        assertThrows(ForbiddenException.class, () -> {
            testService.adminMethod();
        });
    }

    @Test
    @DisplayName("@RequireAdmin: отказ для SERVICE")
    void testRequireAdmin_ServiceDenied() {
        setServiceContext();

        assertThrows(ForbiddenException.class, () -> {
            testService.adminMethod();
        });
    }

    @Test
    @DisplayName("@RequireAdmin: отказ БЕЗ авторизации")
    void testRequireAdmin_Unauthorized() {
        assertThrows(ForbiddenException.class, () -> {
            testService.adminMethod();
        });
    }

    @Test
    @DisplayName("@RequireService: успешный доступ для SERVICE")
    void testRequireService_Success() {
        setServiceContext();

        String result = testService.serviceMethod();
        assertEquals("service-ok", result);
    }

    @Test
    @DisplayName("@RequireService: отказ для USER")
    void testRequireService_UserDenied() {
        setUserContext(123456789L);

        assertThrows(ForbiddenException.class, () -> {
            testService.serviceMethod();
        });
    }

    @Test
    @DisplayName("@RequireService: отказ для ADMIN")
    void testRequireService_AdminDenied() {
        setAdminContext(999999L);

        assertThrows(ForbiddenException.class, () -> {
            testService.serviceMethod();
        });
    }

    @Test
    @DisplayName("@RequireAnyRole: успешный доступ для USER")
    void testRequireAnyRole_UserSuccess() {
        setUserContext(123456789L);

        String result = testService.anyRoleMethod();
        assertEquals("any-role-ok", result);
    }

    @Test
    @DisplayName("@RequireAnyRole: успешный доступ для ADMIN")
    void testRequireAnyRole_AdminSuccess() {
        setAdminContext(999999L);

        String result = testService.anyRoleMethod();
        assertEquals("any-role-ok", result);
    }

    @Test
    @DisplayName("@RequireAnyRole: отказ для SERVICE")
    void testRequireAnyRole_ServiceDenied() {
        setServiceContext();

        assertThrows(ForbiddenException.class, () -> {
            testService.anyRoleMethod();
        });
    }

    @Test
    @DisplayName("Ownership check: успешная проверка владения")
    void testOwnershipCheck_Success() {
        Long userId = 123456789L;
        setUserContext(userId);

        TestRequest request = new TestRequest();
        request.setUserId(userId);

        String result = testService.ownershipMethod(request);
        assertEquals("ownership-ok", result);
    }

    @Test
    @DisplayName("Ownership check: отказ при чужом ресурсе")
    void testOwnershipCheck_Denied() {
        setUserContext(123456789L);

        TestRequest request = new TestRequest();
        request.setUserId(987654321L);

        assertThrows(ForbiddenException.class, () -> {
            testService.ownershipMethod(request);
        });
    }

    @Test
    @DisplayName("SecurityContextHolder: получение userId")
    void testSecurityContextHolder_GetUserId() {
        Long expectedUserId = 123456789L;
        setUserContext(expectedUserId);

        Long actualUserId = SecurityContextHolder.getUserId();
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    @DisplayName("SecurityContextHolder: проверка isUser()")
    void testSecurityContextHolder_IsUser() {
        setUserContext(123456789L);

        assertTrue(SecurityContextHolder.isUser());
        assertFalse(SecurityContextHolder.isAdmin());
        assertFalse(SecurityContextHolder.isService());
    }

    @Test
    @DisplayName("SecurityContextHolder: проверка isAdmin()")
    void testSecurityContextHolder_IsAdmin() {
        setAdminContext(999999L);

        assertTrue(SecurityContextHolder.isAdmin());
        assertFalse(SecurityContextHolder.isService());
    }

    @Test
    @DisplayName("SecurityContextHolder: проверка hasAnyRole()")
    void testSecurityContextHolder_HasAnyRole() {
        setUserContext(123456789L);

        assertTrue(SecurityContextHolder.hasAnyRole(UserRole.USER, UserRole.ADMIN));
        assertFalse(SecurityContextHolder.hasAnyRole(UserRole.ADMIN, UserRole.SERVICE));
    }




    private void setUserContext(Long userId) {
        SecurityContext context = SecurityContext.builder()
                .userId(userId)
                .role(UserRole.USER)
                .ipAddress("127.0.0.1")
                .requestId("test-request-id")
                .internal(false)
                .build();

        SecurityContextHolder.setContext(context);
    }

    private void setAdminContext(Long userId) {
        SecurityContext context = SecurityContext.builder()
                .userId(userId)
                .role(UserRole.ADMIN)
                .roles(Set.of(UserRole.USER, UserRole.ADMIN))
                .ipAddress("127.0.0.1")
                .requestId("test-request-id")
                .internal(false)
                .build();

        SecurityContextHolder.setContext(context);
    }

    private void setServiceContext() {
        SecurityContext context = SecurityContext.builder()
                .role(UserRole.SERVICE)
                .ipAddress("10.0.0.1")
                .requestId("test-request-id")
                .internal(true)
                .build();

        SecurityContextHolder.setContext(context);
    }

    @Service
    static class TestSecurityService {

        @Public
        public String publicMethod() {
            return "public-ok";
        }

        @RequireUser
        public String userMethod() {
            return "user-ok";
        }

        @RequireAdmin
        public String adminMethod() {
            return "admin-ok";
        }

        @RequireService
        public String serviceMethod() {
            return "service-ok";
        }

        @RequireAnyRole({UserRole.USER, UserRole.ADMIN})
        public String anyRoleMethod() {
            return "any-role-ok";
        }

        @RequireUser(checkOwnership = true)
        public String ownershipMethod(TestRequest request) {
            return "ownership-ok";
        }
    }

    @Setter
    @Getter
    static class TestRequest {
        private Long userId;

    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        public TestSecurityService testSecurityService() {
            return new TestSecurityService();
        }
    }
  
}