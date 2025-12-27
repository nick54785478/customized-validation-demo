package com.example.demo.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.application.domain.policy.aggregate.ValidationPolicy;


@Repository
public interface ValidationPolicyRepository extends JpaRepository<ValidationPolicy, Long> {

	List<ValidationPolicy> findByTemplateName(String templateName);
}
