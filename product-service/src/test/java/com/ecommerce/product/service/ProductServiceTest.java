package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;
    private ProductDTO sampleProductDTO;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Wireless Bluetooth Headphones");
        sampleProduct.setSku("ELEC-WBH-001");
        sampleProduct.setPrice(new BigDecimal("79.99"));
        sampleProduct.setCategory("Electronics");
        sampleProduct.setStock(150);
        sampleProduct.setActive(true);

        sampleProductDTO = new ProductDTO();
        sampleProductDTO.setName("Wireless Bluetooth Headphones");
        sampleProductDTO.setPrice(new BigDecimal("79.99"));
        sampleProductDTO.setCategory("Electronics");
        sampleProductDTO.setStock(150);
    }

    @Nested
    @DisplayName("Create Product")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully with valid data")
        void shouldCreateProductSuccessfully() {
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

            ProductDTO result = productService.createProduct(sampleProductDTO);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Wireless Bluetooth Headphones");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("79.99"));
            assertThat(result.getCategory()).isEqualTo("Electronics");
            assertThat(result.getStock()).isEqualTo(150);

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should reject product with negative price")
        void shouldRejectProductWithNegativePrice() {
            sampleProductDTO.setPrice(new BigDecimal("-10.00"));

            assertThatThrownBy(() -> productService.createProduct(sampleProductDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be positive");
        }

        @Test
        @DisplayName("should reject product with negative stock")
        void shouldRejectProductWithNegativeStock() {
            sampleProductDTO.setStock(-5);

            assertThatThrownBy(() -> productService.createProduct(sampleProductDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stock cannot be negative");
        }
    }

    @Nested
    @DisplayName("Get Product")
    class GetProduct {

        @Test
        @DisplayName("should return product by ID when it exists")
        void shouldReturnProductById() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

            ProductDTO result = productService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Wireless Bluetooth Headphones");
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw ProductNotFoundException when product does not exist")
        void shouldThrowExceptionWhenNotFound() {
            when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 999");
        }

        @Test
        @DisplayName("should return products filtered by category")
        void shouldReturnProductsByCategory() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(Arrays.asList(sampleProduct), pageable, 1);

            when(productRepository.findByCategory("Electronics", pageable)).thenReturn(productPage);
    // Ensure thread safety for concurrent access

            Page<ProductDTO> result = productService.getProductsByCategory("Electronics", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("should search products by name keyword")
        void shouldSearchProductsByName() {
            when(productRepository.findByNameContainingIgnoreCase("Bluetooth"))
                    .thenReturn(Arrays.asList(sampleProduct));

            var results = productService.searchProducts("Bluetooth");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).contains("Bluetooth");
        }
    }

    @Nested
    @DisplayName("Update Product")
    class UpdateProduct {

        @Test
        @DisplayName("should update product price successfully")
        void shouldUpdateProductPrice() {
            ProductDTO updateDTO = new ProductDTO();
            updateDTO.setName("Wireless Bluetooth Headphones");
            updateDTO.setPrice(new BigDecimal("89.99"));
            updateDTO.setCategory("Electronics");
            updateDTO.setStock(150);

            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductDTO result = productService.updateProduct(1L, updateDTO);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("89.99"));
        }

        @Test
        @DisplayName("should throw exception when updating non-existent product")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(999L, sampleProductDTO))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Stock Management")
    class StockManagement {

        @Test
        @DisplayName("should reduce stock when sufficient quantity available")
        void shouldReduceStockWhenSufficient() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            productService.reduceStock(1L, 10);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getStock()).isEqualTo(140);
        }

        @Test
        @DisplayName("should throw InsufficientStockException when stock is low")
        void shouldThrowExceptionWhenInsufficientStock() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> productService.reduceStock(1L, 200))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("should increase stock successfully")
        void shouldIncreaseStock() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            productService.addStock(1L, 50);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getStock()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Delete Product")
    class DeleteProduct {

        @Test
        @DisplayName("should soft-delete product by deactivating it")
        void shouldSoftDeleteProduct() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            productService.deleteProduct(1L);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }
    }

    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    // TODO: add proper error handling here
    }


    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
