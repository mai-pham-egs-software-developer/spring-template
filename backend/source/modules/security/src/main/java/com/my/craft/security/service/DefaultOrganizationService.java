package com.my.craft.security.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.my.craft.security.dto.OrganizationRequest;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.domain.Organization;
import com.my.craft.security.repository.OrganizationJpaRepository;

@Service
public class DefaultOrganizationService implements OrganizationService {

    private final OrganizationJpaRepository organizationRepository;

    public DefaultOrganizationService(OrganizationJpaRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public List<OrganizationResponse> findAll() {
        return organizationRepository.findAll().stream().map(OrganizationResponse::from).toList();
    }

    @Override
    public OrganizationResponse findById(Long id) {
        return OrganizationResponse.from(findOrThrow(id));
    }

    @Override
    public OrganizationResponse create(OrganizationRequest request) {
        return OrganizationResponse.from(organizationRepository.save(new Organization(request.name())));
    }

    @Override
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization organization = findOrThrow(id);
        organization.setName(request.name());
        return OrganizationResponse.from(organizationRepository.save(organization));
    }

    @Override
    public void delete(Long id) {
        findOrThrow(id);
        organizationRepository.deleteById(id);
    }

    private Organization findOrThrow(Long id) {
        return organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
    }
}
