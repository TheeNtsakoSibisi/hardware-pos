package za.co.ntsumo.pos.sales;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/** The next document number for one till and document type. */
@Entity
@Table(name = "document_sequence")
@IdClass(DocumentSequence.Key.class)
public class DocumentSequence {

    public enum DocType { SALE, INVOICE, PAYMENT }

    @Id
    @Column(name = "till_id")
    private String tillId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private DocType docType;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1;

    protected DocumentSequence() {
    }

    public DocumentSequence(String tillId, DocType docType, long nextValue) {
        this.tillId = tillId;
        this.docType = docType;
        this.nextValue = nextValue;
    }

    public long takeNext() {
        return nextValue++;
    }

    public String getTillId() {
        return tillId;
    }

    public DocType getDocType() {
        return docType;
    }

    public long getNextValue() {
        return nextValue;
    }

    public static class Key implements Serializable {
        private String tillId;
        private DocType docType;

        public Key() {
        }

        public Key(String tillId, DocType docType) {
            this.tillId = tillId;
            this.docType = docType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(tillId, key.tillId) && docType == key.docType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tillId, docType);
        }
    }
}
