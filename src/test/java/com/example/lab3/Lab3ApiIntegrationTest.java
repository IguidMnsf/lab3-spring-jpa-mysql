package com.example.lab3;

import com.example.lab3.entity.Category;
import com.example.lab3.entity.Product;
import com.example.lab3.repository.CategoryRepository;
import com.example.lab3.repository.CustomerOrderRepository;
import com.example.lab3.repository.OrderItemRepository;
import com.example.lab3.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Lab3ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Long categoryId;
    private Long productId;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        Category category = categoryRepository.save(new Category("Électronique"));
        categoryId = category.getId();
        productId = productRepository.save(new Product(
                "Laptop de test",
                new BigDecimal("1200.50"),
                category
        )).getId();
    }

    @Test
    void createsCategoryAndProductWithPersistentRelations() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Livres"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Livres"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Téléphone", "699.99", categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Électronique"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1200.50))
                .andExpect(jsonPath("$.category.id").doesNotExist());
    }

    @Test
    void listsSearchesAndPaginatesProducts() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "5").param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/products/search").param("name", "LAPTOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(productId));

        mockMvc.perform(get("/api/categories/{id}/products", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updatesProductAndRejectsDuplicateName() throws Exception {
        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Laptop de test", "1200.50", categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop de test"));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Laptop Pro", "1300.00", categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(1300.00));

        productRepository.save(new Product("Autre produit", new BigDecimal("25.00"),
                categoryRepository.findById(categoryId).orElseThrow()));
        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Autre produit", "30.00", categoryId)))
                .andExpect(status().isConflict());
    }

    @Test
    void createsAnOrderAtomicallyAndReadsItsItems() throws Exception {
        var result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":2}]}
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop de test"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.total").value(2401.00))
                .andReturn();

        JsonNode order = objectMapper.readTree(result.getResponse().getContentAsString());
        mockMvc.perform(get("/api/orders/{id}", order.get("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(productId));

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("commande")));
    }

    @Test
    void returnsClearValidationAndNotFoundErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","price":-2,"categoryId":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.price").exists());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Produit sans catégorie", "10.00", 999999L)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String productJson(String name, String price, Long category) throws Exception {
        return objectMapper.writeValueAsString(new ProductPayload(name, new BigDecimal(price), category));
    }

    private record ProductPayload(String name, BigDecimal price, Long categoryId) {
    }
}
