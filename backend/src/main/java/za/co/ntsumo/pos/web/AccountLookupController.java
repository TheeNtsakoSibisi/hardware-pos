package za.co.ntsumo.pos.web;

import org.springframework.web.bind.annotation.*;
import za.co.ntsumo.pos.accounts.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/** What the till needs when the cashier picks an account: buyers and jobs. */
@RestController
@RequestMapping("/api/accounts/{code}")
public class AccountLookupController {

    private final ContractorAccountRepository accounts;
    private final AuthorisedBuyerRepository buyers;
    private final JobSiteRepository jobs;

    public AccountLookupController(ContractorAccountRepository accounts,
                                   AuthorisedBuyerRepository buyers, JobSiteRepository jobs) {
        this.accounts = accounts;
        this.buyers = buyers;
        this.jobs = jobs;
    }

    public record BuyerOption(Long id, String name, String roleTitle, BigDecimal perSaleLimit) {
    }

    public record JobOption(String code, String description) {
    }

    @GetMapping("/buyers")
    public List<BuyerOption> buyers(@PathVariable String code) {
        return buyers.findByAccountIdAndActiveTrueOrderByNameAsc(account(code).getId()).stream()
                .map(b -> new BuyerOption(b.getId(), b.getName(), b.getRoleTitle(), b.getPerSaleLimit()))
                .toList();
    }

    @GetMapping("/jobs")
    public List<JobOption> jobs(@PathVariable String code) {
        return jobs.findByAccountIdAndActiveTrueOrderByCodeAsc(account(code).getId()).stream()
                .map(j -> new JobOption(j.getCode(), j.getDescription()))
                .toList();
    }

    private ContractorAccount account(String code) {
        return accounts.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("No account " + code));
    }
}
