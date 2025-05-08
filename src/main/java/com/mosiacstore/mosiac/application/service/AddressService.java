package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.AddressRequest;
import com.mosiacstore.mosiac.application.dto.response.AddressResponse;
import com.mosiacstore.mosiac.application.dto.response.DistrictResponse;
import com.mosiacstore.mosiac.application.dto.response.ProvinceResponse;
import com.mosiacstore.mosiac.application.dto.response.WardResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {
    List<ProvinceResponse> getAllProvinces();
    List<DistrictResponse> getDistrictsByProvince(String provinceCode);
    List<WardResponse> getWardsByDistrict(String districtCode);

    List<AddressResponse> getUserAddresses(UUID userId);
    AddressResponse getAddressById(UUID addressId, UUID userId);
    AddressResponse createAddress(AddressRequest request, UUID userId);
    AddressResponse updateAddress(UUID addressId, AddressRequest request, UUID userId);
    void deleteAddress(UUID addressId, UUID userId);
    AddressResponse setDefaultAddress(UUID addressId, UUID userId);
}