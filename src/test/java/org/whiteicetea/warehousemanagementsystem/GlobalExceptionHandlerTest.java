package org.whiteicetea.warehousemanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.whiteicetea.warehousemanagementsystem.common.ApiResponse;
import org.whiteicetea.warehousemanagementsystem.exception.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {
    @Test
    void unexpectedExceptionReturnsSafe500() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleException(new RuntimeException("内部细节"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertEquals("服务器内部错误", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
