package com.whatsapp.commonlib;

import com.whatsapp.common.config.CommonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies the common-lib auto-configuration loads correctly.
 */
@SpringBootTest(classes = {CommonLibApplication.class, CommonConfig.class})
class CommonLibApplicationTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertThat(objectMapper).isNotNull();
    }

}
