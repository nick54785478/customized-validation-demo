package com.example.demo.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.policy.aggregate.ValidationPolicy;
import java.util.List;


@Repository
public interface ValidationPolicyRepository extends JpaRepository<ValidationPolicy, Long> {

	List<ValidationPolicy> findByTemplateName(String templateName);
}
