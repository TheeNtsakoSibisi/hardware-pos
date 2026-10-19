package za.co.ntsumo.pos.sales;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, DocumentSequence.Key> {

    /**
     * Locks the row for the duration of the transaction. Two sales on the same
     * till cannot take the same number, even under concurrent requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DocumentSequence d WHERE d.tillId = :tillId AND d.docType = :docType")
    Optional<DocumentSequence> lock(@Param("tillId") String tillId,
                                    @Param("docType") DocumentSequence.DocType docType);
}
