package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:mobile-count;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.main.web-application-type=none")
class MobileStockCountRepositoryTest {
    @Autowired private StockCountRepository stockCountRepository;
    @Autowired private StockCountLineRepository stockCountLineRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private RakRepository rakRepository;
    @Autowired private BarangRepository barangRepository;

    @Test
    void mobileCountQueriesReturnOnlyAssignedOpenLinesAndDoNotExposeExpectedQuantity() {
        User firstPicker = saveUser("first@example.com", "First Picker");
        User secondPicker = saveUser("second@example.com", "Second Picker");
        Section section = sectionRepository.save(new Section(null, "SEC-API", "API Test", 12));
        Rak rack = rakRepository.save(new Rak(null, section, "R-API", 100, 0));
        Barang firstItem = saveItem("SKU-API-1");
        Barang secondItem = saveItem("SKU-API-2");
        Barang thirdItem = saveItem("SKU-API-3");
        StockCount count = new StockCount("SC-API-COUNT", rack, firstPicker, LocalDateTime.now());
        count.setCycle("DC-20261019", StockCountCycleType.DAILY);
        count.addLine(assignedLine(firstItem, firstPicker));
        count.addLine(assignedLine(secondItem, firstPicker));
        count.addLine(assignedLine(thirdItem, secondPicker));
        stockCountRepository.saveAndFlush(count);

        var firstPickerCounts = stockCountRepository.findMobileActiveCounts(firstPicker.getId(),
                List.of(StockCountStatus.SCHEDULED, StockCountStatus.COUNTING),
                StockCountStatus.SCHEDULED, PageRequest.of(0, 25));
        var secondPickerCounts = stockCountRepository.findMobileActiveCounts(secondPicker.getId(),
                List.of(StockCountStatus.SCHEDULED, StockCountStatus.COUNTING),
                StockCountStatus.SCHEDULED, PageRequest.of(0, 25));
        var firstPickerLines = stockCountLineRepository.findOpenLinesForUser(
                count.getId(), firstPicker.getId(), PageRequest.of(0, 100));
        var secondPickerLines = stockCountLineRepository.findOpenLinesForUser(
                count.getId(), secondPicker.getId(), PageRequest.of(0, 100));

        assertEquals(1, firstPickerCounts.getTotalElements());
        assertEquals(1, secondPickerCounts.getTotalElements());
        assertEquals(2, firstPickerLines.getTotalElements());
        assertEquals("SKU-API-1", firstPickerLines.getContent().getFirst().itemCode());
        assertEquals(List.of("SKU-API-3"), secondPickerLines.getContent().stream()
                .map(StockCountLineProjection::itemCode).toList());
    }

    private User saveUser(String email, String name) {
        return userRepository.save(User.builder().fullName(name).email(email)
                .passwordHash("test-hash").role("user").build());
    }

    private Barang saveItem(String sku) {
        return barangRepository.save(Barang.builder().kodeBarang(sku).namaBarang("Test item")
                .satuan("pcs").stok(0).stokMin(0).build());
    }

    private StockCountLine assignedLine(Barang item, User user) {
        StockCountLine line = new StockCountLine(item, 12);
        line.setAssignedUser(user);
        return line;
    }
}
