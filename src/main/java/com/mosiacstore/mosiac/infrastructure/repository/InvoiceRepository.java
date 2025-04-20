package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.order.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

}
