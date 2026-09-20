package com.ruth.inventio.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ruth.inventio.dto.MovimientoInventarioRequest;
import com.ruth.inventio.dto.DetalleComercialRequest;
import com.ruth.inventio.dto.VentaRequest;
import com.ruth.inventio.dto.ReciboRequest;
import com.ruth.inventio.dto.ProformaRequest;
import com.ruth.inventio.entity.Cliente;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.model.NombreRol;
import com.ruth.inventio.model.EstadoVenta;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import java.util.List;
import static org.mockito.Mockito.*;
import com.ruth.inventio.entity.Producto;
import com.ruth.inventio.exception.StockInsuficienteException;
import com.ruth.inventio.repository.MovimientoInventarioRepository;
import com.ruth.inventio.repository.ProductoRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** PostgreSQL real opcional. Crea y elimina exclusivamente su propio esquema aleatorio. */
@EnabledIfEnvironmentVariable(named = "INVENTIO_TEST_DB_URL", matches = ".+")
class InventoryPostgresTests {

    private static DriverManagerDataSource dataSource;
    private static LocalContainerEntityManagerFactoryBean factory;
    private static ValidatorFactory validation;
    private static TransactionTemplate transaction;
    private static ProductoRepository productos;
    private static InventoryService service;
    private static SalesService sales;
    private static ReceiptService receipts;
    private static QuoteService quotes;
    private static Long customerId;
    private static VentaDetalleRepository saleDetails;
    private static String schema;
    private static boolean schemaCreated;

    @BeforeAll
    static void prepareIsolatedSchema() throws Exception {
        dataSource = new DriverManagerDataSource(System.getenv("INVENTIO_TEST_DB_URL"),
                System.getenv("INVENTIO_TEST_DB_USERNAME"), System.getenv("INVENTIO_TEST_DB_PASSWORD"));
        schema = "inventio_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            schemaCreated = true;
        }
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.ruth.inventio.entity");
        factory.setJpaPropertyMap(Map.of(
                "hibernate.default_schema", schema,
                "jakarta.persistence.schema-generation.database.action", "create",
                "hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD"));
        factory.afterPropertiesSet();
        var entityManagerFactory = factory.getObject();
        JpaTransactionManager manager = new JpaTransactionManager(entityManagerFactory);
        // Rechaza futuras llamadas desde una transaccion de aislamiento incompatible.
        manager.setValidateExistingTransaction(true);
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var repositories = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
        productos = repositories.getRepository(ProductoRepository.class);
        validation = Validation.buildDefaultValidatorFactory();
        InventoryService target = new InventoryService(productos,
                repositories.getRepository(MovimientoInventarioRepository.class),
                repositories.getRepository(UsuarioRepository.class), validation.getValidator());
        TransactionInterceptor advice = new TransactionInterceptor();
        advice.setTransactionManager(manager);
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        advice.afterPropertiesSet();
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(advice);
        service = (InventoryService) proxy.getProxy();
        var customers = repositories.getRepository(ClienteRepository.class);
        var users = repositories.getRepository(UsuarioRepository.class);
        var roles = repositories.getRepository(RolRepository.class);
        var actor = transaction.execute(status -> {
            Rol role = new Rol(); role.setNombre(NombreRol.ADMIN); roles.save(role);
            Usuario user = new Usuario(); user.setNombre("Prueba"); user.setEmail("admin@inventio.test");
            user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4).encode("prueba-segura-123"));
            user.getRoles().add(role);
            return users.save(user);
        });
        customerId = transaction.execute(status -> {
            Cliente customer = new Cliente(); customer.setNombre("Cliente prueba"); customer.setIdentificacion("001");
            return customers.save(customer).getId();
        });
        CurrentUser current = mock(CurrentUser.class);
        when(current.usuario()).thenReturn(actor); when(current.id()).thenReturn(actor.getId());
        var salesRepo = repositories.getRepository(VentaRepository.class);
        saleDetails = repositories.getRepository(VentaDetalleRepository.class);
        var quotesRepo = repositories.getRepository(ProformaRepository.class);
        var receiptsRepo = repositories.getRepository(ReciboRepository.class);
        var calculator = new DocumentoCalculator(productos, validation.getValidator());
        sales = transactional(new SalesService(salesRepo, saleDetails, customers, quotesRepo, receiptsRepo,
                service, calculator, current), advice);
        receipts = transactional(new ReceiptService(receiptsRepo, salesRepo, current, validation.getValidator()), advice);
        quotes = transactional(new QuoteService(quotesRepo, repositories.getRepository(ProformaDetalleRepository.class),
                customers, salesRepo, calculator, current), advice);
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target, TransactionInterceptor advice) {
        ProxyFactory proxy = new ProxyFactory(target); proxy.addAdvice(advice);
        return (T) proxy.getProxy();
    }

    private VentaRequest saleRequest(Long productId, String quantity, String price) {
        return new VentaRequest(customerId, null, List.of(new DetalleComercialRequest(productId,
                new BigDecimal(quantity), new BigDecimal(price))));
    }

    @Test
    void saleDeductsStockAndReceiptsTransitionToPartialAndPaid() {
        Long id = product(); service.registrarEntrada(request(id, "10"));
        var sale = sales.crear(saleRequest(id, "2", "10"));
        assertEquals(EstadoVenta.PENDIENTE, sale.estado());
        assertEquals(0, new BigDecimal("8").compareTo(service.obtenerStockProducto(id).stockActual()));
        assertEquals(sale.numero(), service.obtenerKardexProducto(id).movimientos().getLast().documentoOrigen());
        receipts.crear(new ReciboRequest(sale.id(), new BigDecimal("5"), null));
        assertEquals(EstadoVenta.PARCIAL, sales.obtener(sale.id()).estado());
        assertEquals(0, new BigDecimal("15").compareTo(sales.obtener(sale.id()).saldo()));
        assertThrows(ReglaNegocioException.class, () -> receipts.crear(new ReciboRequest(sale.id(), new BigDecimal("16"), null)));
        receipts.crear(new ReciboRequest(sale.id(), new BigDecimal("15"), null));
        assertEquals(EstadoVenta.PAGADA, sales.obtener(sale.id()).estado());
        assertEquals(0, sales.obtener(sale.id()).saldo().signum());
        assertThrows(ReglaNegocioException.class, () -> sales.anular(sale.id()));
    }

    @Test
    void insufficientSecondProductRollsBackHeaderDetailsAndFirstExit() {
        Long first = product(); Long second = product(); service.registrarEntrada(request(first, "10"));
        int headers = sales.listar().size();
        int details = transaction.execute(status -> saleDetails.findAll().size());
        var request = new VentaRequest(customerId, null, List.of(
                new DetalleComercialRequest(first, BigDecimal.ONE, BigDecimal.TEN),
                new DetalleComercialRequest(second, BigDecimal.ONE, BigDecimal.TEN)));
        assertThrows(StockInsuficienteException.class, () -> sales.crear(request));
        assertEquals(headers, sales.listar().size());
        int remainingDetails = transaction.execute(status -> saleDetails.findAll().size());
        assertEquals(details, remainingDetails);
        assertEquals(0, new BigDecimal("10").compareTo(service.obtenerStockProducto(first).stockActual()));
        assertEquals(1, service.obtenerKardexProducto(first).movimientos().size());
        assertEquals(0, service.obtenerKardexProducto(second).movimientos().size());
    }

    @Test
    void cancellingSaleCompensatesExactlyOnceAndKeepsOriginalExit() {
        Long id = product(); service.registrarEntrada(request(id, "10"));
        var sale = sales.crear(saleRequest(id, "3", "5"));
        assertEquals(EstadoVenta.ANULADA, sales.anular(sale.id()).estado());
        assertEquals(0, new BigDecimal("10").compareTo(service.obtenerStockProducto(id).stockActual()));
        assertEquals(3, service.obtenerKardexProducto(id).movimientos().size());
        assertThrows(ReglaNegocioException.class, () -> sales.anular(sale.id()));
        assertEquals(3, service.obtenerKardexProducto(id).movimientos().size());
    }

    @Test
    void quoteDoesNotReserveInventoryAndConversionCopiesServerDetails() {
        Long id = product();
        var quote = quotes.crear(new ProformaRequest(customerId, saleRequest(id, "2", "5").detalles(), null));
        assertEquals(0, service.obtenerKardexProducto(id).movimientos().size());
        service.registrarEntrada(request(id, "2"));
        var sale = sales.crear(new VentaRequest(null, quote.id(), null));
        assertEquals(quote.id(), sale.proformaId());
        assertEquals(quote.total(), sale.total());
        assertEquals(0, service.obtenerStockProducto(id).stockActual().signum());
        assertThrows(ReglaNegocioException.class, () -> sales.crear(new VentaRequest(null, quote.id(), null)));
    }

    @Test
    void concurrentSalesCannotOversellAndFailedSaleLeavesNoHeader() throws Exception {
        Long id = product(); service.registrarEntrada(request(id, "10"));
        int count = sales.listar().size();
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transaction.execute(status -> {
                var sale = sales.crear(saleRequest(id, "7", "1")); inserted.countDown();
                awaitCommit(commit); return sale;
            }));
            try {
                assertTrue(inserted.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> sales.crear(saleRequest(id, "7", "1")));
                try { assertThrows(TimeoutException.class, () -> second.get(250, TimeUnit.MILLISECONDS)); }
                finally { commit.countDown(); }
                first.get(10, TimeUnit.SECONDS);
                assertInstanceOf(StockInsuficienteException.class,
                        assertThrows(ExecutionException.class, () -> second.get(10, TimeUnit.SECONDS)).getCause());
            } finally { commit.countDown(); }
        }
        assertEquals(count + 1, sales.listar().size());
        assertEquals(0, new BigDecimal("3").compareTo(service.obtenerStockProducto(id).stockActual()));
    }

    @Test
    void concurrentReceiptsCannotOverpay() throws Exception {
        Long id = product(); service.registrarEntrada(request(id, "1"));
        var sale = sales.crear(saleRequest(id, "1", "10"));
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transaction.execute(status -> {
                var receipt = receipts.crear(new ReciboRequest(sale.id(), new BigDecimal("7"), null));
                inserted.countDown(); awaitCommit(commit); return receipt;
            }));
            try {
                assertTrue(inserted.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> receipts.crear(new ReciboRequest(sale.id(), new BigDecimal("7"), null)));
                try { assertThrows(TimeoutException.class, () -> second.get(250, TimeUnit.MILLISECONDS)); }
                finally { commit.countDown(); }
                first.get(10, TimeUnit.SECONDS);
                assertInstanceOf(ReglaNegocioException.class,
                        assertThrows(ExecutionException.class, () -> second.get(10, TimeUnit.SECONDS)).getCause());
            } finally { commit.countDown(); }
        }
        assertEquals(0, new BigDecimal("3").compareTo(sales.obtener(sale.id()).saldo()));
    }

    private void awaitCommit(CountDownLatch commit) {
        try {
            if (!commit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de prueba");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
        }
    }

    @AfterAll
    static void removeOnlyOwnedSchema() throws Exception {
        if (factory != null) factory.destroy();
        if (validation != null) validation.close();
        if (schemaCreated && schema.matches("inventio_test_[0-9a-f]{32}")) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private Long product() {
        return transaction.execute(status -> {
            Producto producto = new Producto();
            producto.setCodigo(UUID.randomUUID().toString());
            producto.setNombre("Prueba de inventario");
            producto.setCostoUnitario(new BigDecimal("2.50"));
            return productos.save(producto).getId();
        });
    }

    private MovimientoInventarioRequest request(Long id, String cantidad) {
        return new MovimientoInventarioRequest(id, new BigDecimal(cantidad), null, null, null);
    }

    @Test
    void aggregatesZeroAndAllMovementTypesAndReturnsRunningBalance() {
        Long id = product();
        assertEquals(0, service.obtenerStockProducto(id).stockActual().signum());
        assertTrue(service.obtenerStockActual().stream().anyMatch(stock -> stock.idProducto().equals(id)));
        service.registrarEntrada(request(id, "10"));
        service.registrarAjusteEntrada(request(id, "2"));
        service.registrarSalida(request(id, "3"));
        service.registrarAjusteSalida(request(id, "1"));
        assertEquals(0, new BigDecimal("8").compareTo(service.obtenerStockProducto(id).stockActual()));
        assertEquals(0, new BigDecimal("20").compareTo(service.obtenerStockProducto(id).valorInventario()));
        var kardex = service.obtenerKardexProducto(id);
        assertEquals(4, kardex.movimientos().size());
        assertEquals(0, new BigDecimal("8").compareTo(kardex.stockActual()));
        assertTrue(kardex.movimientos().stream().allMatch(m -> m.saldoAcumulado().signum() >= 0));
    }

    @Test
    void concurrentWithdrawalsWaitForCommitAndCannotOverdraw() throws Exception {
        Long id = product();
        service.registrarEntrada(request(id, "10"));
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transaction.execute(status -> {
                var result = service.registrarSalida(request(id, "7"));
                inserted.countDown();
                try {
                    if (!commit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de prueba");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return result;
            }));
            try {
                assertTrue(inserted.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> {
                    secondStarted.countDown();
                    return service.registrarAjusteSalida(request(id, "7"));
                });
                try {
                    assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> second.get(250, TimeUnit.MILLISECONDS));
                } finally {
                    commit.countDown();
                }
                first.get(10, TimeUnit.SECONDS);
                var failure = assertThrows(ExecutionException.class, () -> second.get(10, TimeUnit.SECONDS));
                assertInstanceOf(StockInsuficienteException.class, failure.getCause());
            } finally {
                commit.countDown();
            }
        }
        assertEquals(0, new BigDecimal("3").compareTo(service.obtenerStockProducto(id).stockActual()));
        assertEquals(2, service.obtenerKardexProducto(id).movimientos().size());
    }
}
