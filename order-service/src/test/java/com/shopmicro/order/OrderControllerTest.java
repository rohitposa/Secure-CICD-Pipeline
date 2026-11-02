package com.shopmicro.order;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    private static final String ORDERS_URL = "/api/orders";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductClient productClient;

    @Test
    void createOrderCalculatesTotal() throws Exception {
        when(productClient.findById(1L)).thenReturn(Optional.of(new ProductDto(1, "Laptop", 55000)));

        mvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.total").value(110000.0));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        when(productClient.findById(99L)).thenReturn(Optional.empty());
    }
