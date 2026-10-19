package za.co.ntsumo.pos.sales;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Two tills that have been offline all morning must not come back with the same
 * receipt numbers, which is why the till id is part of the number.
 */
@DisplayName("Document numbering")
class DocumentNumberServiceTest {

    private final Map<String, DocumentSequence> rows = new HashMap<>();
    private final DocumentSequenceRepository repository = mock(DocumentSequenceRepository.class);
    private final DocumentNumberService service = new DocumentNumberService(repository);

    DocumentNumberServiceTest() {
        when(repository.lock(anyString(), any())).thenAnswer(call -> {
            String key = call.getArgument(0) + ":" + call.getArgument(1);
            return Optional.ofNullable(rows.get(key));
        });
        when(repository.save(any(DocumentSequence.class))).thenAnswer(call -> {
            DocumentSequence sequence = call.getArgument(0);
            rows.put(sequence.getTillId() + ":" + sequence.getDocType(), sequence);
            return sequence;
        });
    }

    @Test
    @DisplayName("numbers sales in sequence, padded, per till")
    void sequentialSaleNumbers() {
        assertThat(service.nextSaleNumber("T1")).isEqualTo("T1-000001");
        assertThat(service.nextSaleNumber("T1")).isEqualTo("T1-000002");
    }

    @Test
    @DisplayName("each till has its own run of numbers")
    void perTillSequences() {
        service.nextSaleNumber("T1");
        service.nextSaleNumber("T1");

        assertThat(service.nextSaleNumber("T2")).isEqualTo("T2-000001");
        assertThat(service.nextSaleNumber("T1")).isEqualTo("T1-000003");
    }

    @Test
    @DisplayName("account invoices are numbered apart from till slips")
    void invoiceNumbersAreSeparate() {
        service.nextSaleNumber("T1");

        assertThat(service.nextInvoiceNumber("T1")).isEqualTo("T1-A00001");
        assertThat(service.nextSaleNumber("T1")).isEqualTo("T1-000002");
    }

    @Test
    @DisplayName("payment receipts have their own run")
    void paymentNumbers() {
        assertThat(service.nextPaymentNumber("T1")).isEqualTo("RCPT-00001");
        assertThat(service.nextPaymentNumber("T1")).isEqualTo("RCPT-00002");
    }
}
