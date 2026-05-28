package com.qrcode.repository;

import com.qrcode.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, String> {

	List<QrCode> findByStatusOrderByCreatedAtDesc(String status);

	List<QrCode> findAllByOrderByCreatedAtDesc();

	@Query("SELECT q FROM QrCode q WHERE q.expiredAt < ?1 AND q.status = 'VALID'")
	List<QrCode> findExpiredQrCodes(LocalDateTime now);

}
