package com.gw.share.common.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void handleExceptionReturnsInternalServerErrorResponse() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"));
    }

    @Test
    void handleBusinessExceptionReturnsErrorCodeMessage() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/error")
        public ApiResponse<Void> throwError() {
            throw new IllegalStateException("test exception");
        }

        @GetMapping("/test/business-error")
        public ApiResponse<Void> throwBusinessError() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "상세 메시지");
        }
    }
}
