package com.my.craft.security.service.initial;

import java.util.List;

import com.my.craft.security.service.OrganizationNotFoundException;
import com.my.craft.security.service.OrganizationService;
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
        Organization saved = organizationRepository.save(new Organization(request.name()));
        return OrganizationResponse.from(saved);
    }

    @Override
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization organization = findOrThrow(id);
        organization.setName(request.name());
        Organization saved = organizationRepository.save(organization);
        return OrganizationResponse.from(saved);
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
