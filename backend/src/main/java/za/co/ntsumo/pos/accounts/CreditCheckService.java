package za.co.ntsumo.pos.accounts;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.ntsumo.pos.money.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether a sale may go onto an account, and says why not in words the
 * cashier can read to the customer.
 *
 * <p>Four rules, checked in the order a person would explain them:
 *
 * <ol>
 *   <li>the account is on hold;
 *   <li>something on it is more than 90 days old;
 *   <li>this sale would take the balance past the credit limit;
 *   <li>the person collecting is over their own per-sale limit.
 * </ol>
 *
 * <p>A refusal is never the last word. A manager can approve the sale anyway,
 * and the reason is written onto the invoice, so the owner sees later what was
 * waved through and why.
 */
@Service
public class CreditCheckService {

    private final AgeingService ageing;
    private final PasswordEncoder passwordEncoder;

    public CreditCheckService(AgeingService ageing, PasswordEncoder passwordEncoder) {
        this.ageing = ageing;
        this.passwordEncoder = passwordEncoder;
    }

    /** Why a sale was refused, in the cashier's words. */
    public enum Reason {
        ACCOUNT_ON_HOLD,
        OVERDUE_90_DAYS,
        OVER_CREDIT_LIMIT,
        OVER_BUYER_LIMIT
    }

    public record Refusal(Reason reason, String message) {
    }

    public record Decision(boolean approved, List<Refusal> refusals,
                           AgeingService.AccountPosition position) {

        public boolean requiresOverride() {
            return !approved;
        }
    }

    public Decision check(ContractorAccount account, AuthorisedBuyer buyer, BigDecimal saleTotal) {
        AgeingService.AccountPosition position = ageing.positionOf(account);
        List<Refusal> refusals = new ArrayList<>();

        if (account.getStatus() != ContractorAccount.Status.ACTIVE) {
            refusals.add(new Refusal(Reason.ACCOUNT_ON_HOLD,
                    "%s is %s.".formatted(account.getName(),
                            account.getStatus() == ContractorAccount.Status.ON_HOLD ? "on hold" : "closed")));
        }

        BigDecimal overdue = position.ageing().days90Plus();
        if (Money.isPositive(overdue)) {
            refusals.add(new Refusal(Reason.OVERDUE_90_DAYS,
                    "R%s is more than 90 days old.".formatted(overdue.toPlainString())));
        }

        BigDecimal projected = Money.scaled(position.balance().add(saleTotal));
        if (projected.compareTo(account.getCreditLimit()) > 0) {
            BigDecimal excess = Money.scaled(projected.subtract(account.getCreditLimit()));
            refusals.add(new Refusal(Reason.OVER_CREDIT_LIMIT,
                    "This sale takes the balance to R%s, over the R%s limit by R%s."
                            .formatted(projected.toPlainString(),
                                    account.getCreditLimit().toPlainString(),
                                    excess.toPlainString())));
        }

        BigDecimal buyerLimit = buyer.getPerSaleLimit();
        if (buyerLimit != null && saleTotal.compareTo(buyerLimit) > 0) {
            refusals.add(new Refusal(Reason.OVER_BUYER_LIMIT,
                    "%s may only charge up to R%s per sale."
                            .formatted(buyer.getName(), buyerLimit.toPlainString())));
        }

        return new Decision(refusals.isEmpty(), List.copyOf(refusals), position);
    }

    /**
     * Confirms the person collecting is who the account says they are.
     *
     * <p>PINs are BCrypt hashed. They are weak secrets by nature, four digits at
     * a counter, so they identify the collector on the invoice rather than
     * securing anything on their own.
     */
    public boolean pinMatches(AuthorisedBuyer buyer, String pin) {
        return buyer.isActive() && pin != null && passwordEncoder.matches(pin, buyer.getPinHash());
    }
}
