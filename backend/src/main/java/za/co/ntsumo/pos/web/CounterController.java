package za.co.ntsumo.pos.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.ntsumo.pos.accounts.AuthorisedBuyerRepository;
import za.co.ntsumo.pos.accounts.ContractorAccountRepository;
import za.co.ntsumo.pos.accounts.JobSiteRepository;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.catalogue.ProductRepository;
import za.co.ntsumo.pos.catalogue.StockMovementRepository;
import za.co.ntsumo.pos.sales.Sale;
import za.co.ntsumo.pos.sales.SaleRepository;
import za.co.ntsumo.pos.sales.SaleService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** The counter: find products, take payment, charge to an account. */
@RestController
@RequestMapping("/api")
public class CounterController {

    private final ProductRepository products;
    private final StockMovementRepository stock;
    private final SaleService saleService;
    private final SaleRepository sales;
    private final ContractorAccountRepository accounts;
    private final AuthorisedBuyerRepository buyers;
    private final JobSiteRepository jobs;

    public CounterController(ProductRepository products, StockMovementRepository stock,
                             SaleService saleService, SaleRepository sales,
                             ContractorAccountRepository accounts, AuthorisedBuyerRepository buyers,
                             JobSiteRepository jobs) {
        this.products = products;
        this.stock = stock;
        this.saleService = saleService;
        this.sales = sales;
        this.accounts = accounts;
        this.buyers = buyers;
        this.jobs = jobs;
    }

    /** Resolves the names an account invoice prints. */
    private SaleViews.SaleView view(Sale sale) {
        if (sale.getSaleType() != za.co.ntsumo.pos.sales.SaleType.ACCOUNT) {
            return SaleViews.of(sale, null, null, null);
        }
        String accountName = accounts.findById(sale.getAccountId())
                .map(a -> a.getName()).orElse(null);
        String buyerName = buyers.findById(sale.getBuyerId()).map(b -> b.getName()).orElse(null);
        String job = sale.getJobId() == null ? null
                : jobs.findById(sale.getJobId()).map(j -> j.getDescription()).orElse(null);
        return SaleViews.of(sale, accountName, buyerName, job);
    }

    public record ProductView(String sku, String barcode, String name, String category, String unit,
                              boolean decimalQty, BigDecimal price, BigDecimal stockOnHand,
                              BigDecimal reorderLevel) {
    }

    @GetMapping("/products")
    public List<ProductView> search(@RequestParam(required = false) String term,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(defaultValue = "40") int limit) {
        String cleanedTerm = (term == null || term.isBlank()) ? null : term.trim();
        String cleanedCategory = (category == null || category.isBlank() || "All".equals(category))
                ? null : category;
        List<Product> found = products.search(cleanedTerm, cleanedCategory,
                PageRequest.of(0, Math.min(limit, 200)));

        Map<Long, BigDecimal> levels = stock.allStockLevels().stream()
                .collect(Collectors.toMap(StockMovementRepository.StockLevel::getProductId,
                        StockMovementRepository.StockLevel::getOnHand));

        return found.stream()
                .map(p -> new ProductView(p.getSku(), p.getBarcode(), p.getName(), p.getCategory(),
                        p.getUnit(), p.isDecimalQty(), p.getRetailInclVat(),
                        levels.getOrDefault(p.getId(), BigDecimal.ZERO), p.getReorderLevel()))
                .toList();
    }

    @GetMapping("/products/categories")
    public List<String> categories() {
        return products.findCategories();
    }

    @GetMapping("/products/barcode/{barcode}")
    public ResponseEntity<ProductView> byBarcode(@PathVariable String barcode) {
        return products.findByBarcode(barcode)
                .map(p -> ResponseEntity.ok(new ProductView(p.getSku(), p.getBarcode(), p.getName(),
                        p.getCategory(), p.getUnit(), p.isDecimalQty(), p.getRetailInclVat(),
                        stock.stockOnHand(p.getId()), p.getReorderLevel())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sales/walk-in")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER')")
    public SaleViews.SaleView walkIn(@Valid @RequestBody SaleRequests.WalkInSale request,
                                     @AuthenticationPrincipal UserDetails user) {
        return view(saleService.captureWalkIn(request, user.getUsername()));
    }

    @PostMapping("/sales/account")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER')")
    public SaleViews.SaleView onAccount(@Valid @RequestBody SaleRequests.AccountSale request,
                                        @AuthenticationPrincipal UserDetails user) {
        return view(saleService.chargeToAccount(request, user.getUsername()));
    }

    @GetMapping("/sales/{documentNo}")
    public SaleViews.SaleView one(@PathVariable String documentNo) {
        Sale sale = sales.findByDocumentNo(documentNo)
                .orElseThrow(() -> new NoSuchElementException("No document " + documentNo));
        return view(sale);
    }

    /**
     * Receives sales a till queued while offline.
     *
     * <p>Each sale carries the UUID the till generated, so replaying the queue
     * after a partial failure is safe: already-stored sales come back as they
     * were, and the till clears them from its queue.
     */
    @PostMapping("/sync/sales")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER')")
    public SyncResult sync(@Valid @RequestBody SaleRequests.SyncBatch batch,
                           @AuthenticationPrincipal UserDetails user) {
        List<SyncOutcome> outcomes = new ArrayList<>();
        if (batch.walkInSales() != null) {
            for (SaleRequests.WalkInSale sale : batch.walkInSales()) {
                outcomes.add(attempt(sale.clientUuid(), () -> saleService.captureWalkIn(sale, user.getUsername())));
            }
        }
        if (batch.accountSales() != null) {
            for (SaleRequests.AccountSale sale : batch.accountSales()) {
                outcomes.add(attempt(sale.clientUuid(), () -> saleService.chargeToAccount(sale, user.getUsername())));
            }
        }
        long accepted = outcomes.stream().filter(SyncOutcome::accepted).count();
        return new SyncResult(outcomes.size(), accepted, outcomes.size() - accepted, outcomes);
    }

    private SyncOutcome attempt(UUID clientUuid, java.util.function.Supplier<Sale> capture) {
        try {
            Sale sale = capture.get();
            return new SyncOutcome(clientUuid, true, sale.getDocumentNo(), List.of());
        } catch (SaleService.SaleRejected ex) {
            // One bad sale must not block the rest of the queue: it is reported
            // back so the till can flag it for the cashier and move on.
            return new SyncOutcome(clientUuid, false, null, ex.getReasons());
        }
    }

    public record SyncOutcome(UUID clientUuid, boolean accepted, String documentNo, List<String> reasons) {
    }

    public record SyncResult(int received, long accepted, long rejected, List<SyncOutcome> outcomes) {
    }
}
