package za.co.ntsumo.pos.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.ntsumo.pos.accounts.AgeingService;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.accounts.ContractorAccountRepository;
import za.co.ntsumo.pos.catalogue.StockMovementRepository;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.sales.SaleRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** The numbers the owner looks at: takings, debtors and what to reorder. */
@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class OwnerController {

    private static final ZoneId STORE_ZONE = ZoneId.of("Africa/Johannesburg");

    private final SaleRepository sales;
    private final StockMovementRepository stock;
    private final ContractorAccountRepository accounts;
    private final AgeingService ageing;
    private final Clock clock;

    public OwnerController(SaleRepository sales, StockMovementRepository stock,
                           ContractorAccountRepository accounts, AgeingService ageing, Clock clock) {
        this.sales = sales;
        this.stock = stock;
        this.accounts = accounts;
        this.ageing = ageing;
        this.clock = clock;
    }

    public record DayTakings(LocalDate date, long transactions, BigDecimal total,
                             BigDecimal cash, BigDecimal card, BigDecimal eft) {
    }

    public record LowStockItem(String sku, String name, String unit, BigDecimal onHand,
                               BigDecimal reorderLevel) {
    }

    public record Dashboard(BigDecimal takingsThisMonth, BigDecimal owedByContractors,
                            BigDecimal owedOver60Days, int accountsNeedingAttention,
                            int itemsBelowReorderLevel, List<DayTakings> dailyTakings,
                            List<LowStockItem> reorderSoon) {
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(defaultValue = "30") int days) {
        LocalDate today = LocalDate.now(clock.withZone(STORE_ZONE));
        List<DayTakings> daily = sales.walkInTakingsSince(today.minusDays(days)).stream()
                .map(r -> new DayTakings(r.getSaleDate().toLocalDate(), r.getTransactions(),
                        r.getTotal(), r.getCash(), r.getCard(), r.getEft()))
                .toList();

        BigDecimal monthToDate = daily.stream()
                .filter(d -> d.date().getMonth() == today.getMonth()
                        && d.date().getYear() == today.getYear())
                .map(DayTakings::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal owed = Money.ZERO;
        BigDecimal over60 = Money.ZERO;
        int attention = 0;
        for (ContractorAccount account : accounts.findAllByOrderByNameAsc()) {
            AgeingService.AccountPosition position = ageing.positionOf(account);
            owed = owed.add(position.balance());
            over60 = over60.add(position.ageing().over60());
            if (account.getStatus() != ContractorAccount.Status.ACTIVE
                    || position.balance().compareTo(account.getCreditLimit()) > 0
                    || Money.isPositive(position.ageing().days90Plus())) {
                attention++;
            }
        }

        List<LowStockItem> low = stock.findLowStock().stream()
                .map(r -> new LowStockItem(r.getSku(), r.getName(), r.getUnit(), r.getOnHand(),
                        r.getReorderLevel()))
                .toList();

        return new Dashboard(Money.scaled(monthToDate), Money.scaled(owed), Money.scaled(over60),
                attention, low.size(), daily, low.stream().limit(10).toList());
    }
}
