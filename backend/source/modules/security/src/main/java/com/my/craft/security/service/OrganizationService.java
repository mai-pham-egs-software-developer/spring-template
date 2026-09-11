package com.my.craft.security.service;

import java.util.List;

import com.my.craft.security.dto.OrganizationRequest;
import com.my.craft.security.dto.OrganizationResponse;

public interface OrganizationService {

    List<OrganizationResponse> findAll();

    /** @throws OrganizationNotFoundException if {@code id} doesn't exist. */
    OrganizationResponse findById(Long id);

    OrganizationResponse create(OrganizationRequest request);

    /** @throws OrganizationNotFoundException if {@code id} doesn't exist. */
    OrganizationResponse update(Long id, OrganizationRequest request);

    /** @throws OrganizationNotFoundException if {@code id} doesn't exist. */
    void delete(Long id);
}
