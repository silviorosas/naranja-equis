package com.naranjax.transaction.repository;

import com.naranjax.transaction.entity.TransactionAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionAuditRepository extends MongoRepository<TransactionAudit, String> {
}
