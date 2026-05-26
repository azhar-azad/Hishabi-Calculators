package dev.azhar.calculators.platform.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestThrowController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestThrowController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void notFoundExceptionReturns404WithApiErrorShape() throws Exception {
        mockMvc.perform(get("/__test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("widget 42 missing"))
                .andExpect(jsonPath("$.path").value("/__test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void genericExceptionReturns500WithoutLeakingInternals() throws Exception {
        mockMvc.perform(get("/__test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void validationErrorReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/__test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void unmappedPathReturns404FromSpringDefault() throws Exception {
        mockMvc.perform(get("/__test/totally-unmapped-path")).andExpect(status().isNotFound());
    }

    @RestController
    static class TestThrowController {

        @GetMapping("/__test/not-found")
        String triggerNotFound() {
            throw new NotFoundException("widget 42 missing");
        }

        @GetMapping("/__test/boom")
        String triggerGeneric() {
            throw new RuntimeException("internal kaboom - must not leak to client");
        }

        @PostMapping("/__test/validate")
        String validate(@Valid @RequestBody TestPayload payload) {
            return "ok";
        }

        record TestPayload(@NotBlank String name) {}
    }
}
