package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.request.AddressRequest;
import com.mosiacstore.mosiac.application.dto.response.AddressResponse;
import com.mosiacstore.mosiac.application.dto.response.DistrictResponse;
import com.mosiacstore.mosiac.application.dto.response.ProvinceResponse;
import com.mosiacstore.mosiac.application.dto.response.WardResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.service.AddressService;
import com.mosiacstore.mosiac.domain.address.Address;
import com.mosiacstore.mosiac.domain.address.District;
import com.mosiacstore.mosiac.domain.address.Province;
import com.mosiacstore.mosiac.domain.address.Ward;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.infrastructure.repository.AddressRepository;
import com.mosiacstore.mosiac.infrastructure.repository.DistrictRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ProvinceRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import com.mosiacstore.mosiac.infrastructure.repository.WardRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final UserRepository userRepository;

    @Override
    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAll().stream()
                .map(this::mapToProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictResponse> getDistrictsByProvince(String provinceCode) {
        return districtRepository.findByProvinceCode(provinceCode).stream()
                .map(this::mapToDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getWardsByDistrict(String districtCode) {
        return wardRepository.findByDistrictCode(districtCode).stream()
                .map(this::mapToWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getUserAddresses(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddressById(UUID addressId, UUID userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Address does not belong to the current user");
        }

        return mapToAddressResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(AddressRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new EntityNotFoundException("Province not found with code: " + request.getProvinceCode()));

        District district = districtRepository.findById(request.getDistrictCode())
                .orElseThrow(() -> new EntityNotFoundException("District not found with code: " + request.getDistrictCode()));

        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new EntityNotFoundException("Ward not found with code: " + request.getWardCode()));

        // Validate district belongs to province
        if (!district.getProvince().getCode().equals(province.getCode())) {
            throw new InvalidOperationException("District does not belong to the selected province");
        }

        // Validate ward belongs to district
        if (!ward.getDistrict().getCode().equals(district.getCode())) {
            throw new InvalidOperationException("Ward does not belong to the selected district");
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address.setStreetAddress(request.getStreetAddress());
        address.setIsDefault(request.getIsDefault());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            // Set all other addresses as non-default
            addressRepository.updateAllUserAddressesToNonDefault(userId);
        }

        Address savedAddress = addressRepository.save(address);
        return mapToAddressResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID addressId, AddressRequest request, UUID userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Address does not belong to the current user");
        }

        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new EntityNotFoundException("Province not found with code: " + request.getProvinceCode()));

        District district = districtRepository.findById(request.getDistrictCode())
                .orElseThrow(() -> new EntityNotFoundException("District not found with code: " + request.getDistrictCode()));

        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new EntityNotFoundException("Ward not found with code: " + request.getWardCode()));

        // Validate district belongs to province
        if (!district.getProvince().getCode().equals(province.getCode())) {
            throw new InvalidOperationException("District does not belong to the selected province");
        }

        // Validate ward belongs to district
        if (!ward.getDistrict().getCode().equals(district.getCode())) {
            throw new InvalidOperationException("Ward does not belong to the selected district");
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address.setStreetAddress(request.getStreetAddress());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            // Set all other addresses as non-default
            addressRepository.updateAllUserAddressesToNonDefault(userId);
            address.setIsDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        return mapToAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID addressId, UUID userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Address does not belong to the current user");
        }

        addressRepository.delete(address);

        // If this was the default address, make another address the default
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findFirstByUserIdAndIdNot(userId, addressId)
                    .ifPresent(newDefault -> {
                        newDefault.setIsDefault(true);
                        addressRepository.save(newDefault);
                    });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID addressId, UUID userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Address does not belong to the current user");
        }

        // Set all addresses as non-default
        addressRepository.updateAllUserAddressesToNonDefault(userId);

        // Set this address as default
        address.setIsDefault(true);
        Address updatedAddress = addressRepository.save(address);

        return mapToAddressResponse(updatedAddress);
    }

    // Helper mapping methods
    private ProvinceResponse mapToProvinceResponse(Province province) {
        return ProvinceResponse.builder()
                .code(province.getCode())
                .name(province.getName())
                .nameEn(province.getNameEn())
                .fullName(province.getFullName())
                .fullNameEn(province.getFullNameEn())
                .build();
    }

    private DistrictResponse mapToDistrictResponse(District district) {
        return DistrictResponse.builder()
                .code(district.getCode())
                .name(district.getName())
                .nameEn(district.getNameEn())
                .fullName(district.getFullName())
                .fullNameEn(district.getFullNameEn())
                .provinceCode(district.getProvince().getCode())
                .build();
    }

    private WardResponse mapToWardResponse(Ward ward) {
        return WardResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .nameEn(ward.getNameEn())
                .fullName(ward.getFullName())
                .fullNameEn(ward.getFullNameEn())
                .districtCode(ward.getDistrict().getCode())
                .build();
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId().toString())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .province(mapToProvinceResponse(address.getProvince()))
                .district(mapToDistrictResponse(address.getDistrict()))
                .ward(mapToWardResponse(address.getWard()))
                .streetAddress(address.getStreetAddress())
                .isDefault(address.getIsDefault())
                .build();
    }
}