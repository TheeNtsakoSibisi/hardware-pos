package za.co.ntsumo.pos.sales;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues receipt, invoice and payment numbers.
 *
 * <p>Every number carries its till's prefix: T1-000123, T2-000123. Two tills
 * that have been offline all morning can both capture sales and still sync
 * without a collision, which a single shared counter could not survive.
 *
 * <p>Numbers are issued in their own transaction so a sale that later fails
 * validation does not silently reuse a number another sale has already printed.
 */
@Service
public class DocumentNumberService {

    private final DocumentSequenceRepository sequences;

    public DocumentNumberService(DocumentSequenceRepository sequences) {
        this.sequences = sequences;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextSaleNumber(String tillId) {
        return "%s-%06d".formatted(tillId, next(tillId, DocumentSequence.DocType.SALE));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextInvoiceNumber(String tillId) {
        return "%s-A%05d".formatted(tillId, next(tillId, DocumentSequence.DocType.INVOICE));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextPaymentNumber(String tillId) {
        return "RCPT-%05d".formatted(next(tillId, DocumentSequence.DocType.PAYMENT));
    }

    private long next(String tillId, DocumentSequence.DocType type) {
        DocumentSequence sequence = sequences.lock(tillId, type)
                .orElseGet(() -> sequences.save(new DocumentSequence(tillId, type, 1)));
        long value = sequence.takeNext();
        sequences.save(sequence);
        return value;
    }
}
