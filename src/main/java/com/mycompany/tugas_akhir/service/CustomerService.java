package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomer() {
        return customerRepository.findAllByOrderByNamaCustomerAsc();
    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }
}
