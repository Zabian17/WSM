package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    List<Customer> findAllByOrderByNamaCustomerAsc();
}
