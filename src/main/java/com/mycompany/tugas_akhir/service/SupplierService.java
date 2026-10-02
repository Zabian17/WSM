package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAllSupplier() {
        return supplierRepository.findAllByOrderByNamaSupplierAsc();
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Integer id) {
        supplierRepository.deleteById(id);
    }
}
