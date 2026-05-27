package com.unishare.api.modules.service.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.service.dto.MentorDto;
import com.unishare.api.modules.service.dto.request.CreateServicePackageRequest;
import com.unishare.api.modules.service.dto.request.CreateServicePackageVersionRequest;
import com.unishare.api.modules.service.dto.request.UpdateServicePackageRequest;
import com.unishare.api.modules.service.entity.PackageCurriculum;
import com.unishare.api.modules.service.entity.ServicePackage;
import com.unishare.api.modules.service.entity.ServicePackageVersion;
import com.unishare.api.modules.service.exception.ServiceErrorCode;
import com.unishare.api.modules.service.repository.PackageCurriculumRepository;
import com.unishare.api.modules.service.repository.ServicePackageRepository;
import com.unishare.api.modules.service.repository.ServicePackageVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

        @Mock
        private ServicePackageRepository servicePackageRepository;
        @Mock
        private ServicePackageVersionRepository servicePackageVersionRepository;
        @Mock
        private PackageCurriculumRepository packageCurriculumRepository;

        @InjectMocks
        private CatalogServiceImpl catalogService;

        private UUID mentorId;
        private UUID packageId;
        private UUID versionId;

        @BeforeEach
        void setUp() {
                mentorId = UUID.randomUUID();
                packageId = UUID.randomUUID();
                versionId = UUID.randomUUID();
        }

        @Test
        void createPackage_whenValidRequest_shouldCreatePackageDefaultVersionAndCurriculums() {
                CreateServicePackageRequest request = validRequest();

                when(servicePackageRepository.save(any(ServicePackage.class))).thenAnswer(invocation -> {
                        ServicePackage servicePackage = invocation.getArgument(0);
                        servicePackage.setId(packageId);
                        return servicePackage;
                });
                when(servicePackageVersionRepository.save(any(ServicePackageVersion.class))).thenAnswer(invocation -> {
                        ServicePackageVersion version = invocation.getArgument(0);
                        version.setId(versionId);
                        return version;
                });
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageResponse response = catalogService.createPackage(mentorId, request);

                ArgumentCaptor<ServicePackageVersion> versionCaptor = ArgumentCaptor
                                .forClass(ServicePackageVersion.class);
                verify(servicePackageVersionRepository).save(versionCaptor.capture());
                assertTrue(versionCaptor.getValue().getIsDefault());
                assertEquals(packageId, versionCaptor.getValue().getPackageId());

                ArgumentCaptor<List<PackageCurriculum>> curriculumCaptor = ArgumentCaptor.forClass(List.class);
                verify(packageCurriculumRepository).saveAll(curriculumCaptor.capture());
                assertEquals(2, curriculumCaptor.getValue().size());
                assertTrue(curriculumCaptor.getValue().stream()
                                .allMatch(item -> versionId.equals(item.getPackageVersionId())));

                assertEquals(packageId, response.getId());
                assertEquals(1, response.getVersions().size());
                assertTrue(response.getVersions().get(0).getIsDefault());
                assertEquals(2, response.getVersions().get(0).getCurriculums().size());
                assertEquals(List.of(1, 2), response.getVersions().get(0).getCurriculums().stream()
                                .map(MentorDto.CurriculumItemResponse::getOrderIndex)
                                .toList());
        }

        @Test
        void createPackage_whenCurriculumOrderIndexDuplicated_shouldThrowBusinessException() {
                CreateServicePackageRequest request = validRequest();
                request.getCurriculums().get(1).setOrderIndex(1);

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.createPackage(mentorId, request));

                assertSame(ServiceErrorCode.DUPLICATE_CURRICULUM_ORDER_INDEX, exception.getExceptionCode());
        }

        @Test
        void createPackage_shouldPersistExactlyOneDefaultVersion() {
                when(servicePackageRepository.save(any(ServicePackage.class))).thenAnswer(invocation -> {
                        ServicePackage servicePackage = invocation.getArgument(0);
                        servicePackage.setId(packageId);
                        return servicePackage;
                });
                when(servicePackageVersionRepository.save(any(ServicePackageVersion.class))).thenAnswer(invocation -> {
                        ServicePackageVersion version = invocation.getArgument(0);
                        version.setId(versionId);
                        return version;
                });
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                catalogService.createPackage(mentorId, validRequest());

                ArgumentCaptor<ServicePackageVersion> versionCaptor = ArgumentCaptor
                                .forClass(ServicePackageVersion.class);
                verify(servicePackageVersionRepository).save(versionCaptor.capture());
                assertNotNull(versionCaptor.getValue());
                assertTrue(versionCaptor.getValue().getIsDefault());
                assertFalse(Boolean.FALSE.equals(versionCaptor.getValue().getIsDefault()));
        }

        @Test
        void getMentorPackages_whenPaged_shouldReturnOnlyMappedActivePackages() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(true);

                PageRequest pageable = PageRequest.of(0, 5);
                when(servicePackageRepository.findByMentorIdAndIsActiveTrueAndDeletedAtIsNull(eq(mentorId), eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(servicePackage), pageable, 1));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                Page<MentorDto.ServicePackageResponse> response = catalogService.getMentorPackages(mentorId, null,
                                pageable);

                assertEquals(1, response.getTotalElements());
                assertEquals("Career Planning", response.getContent().get(0).getName());
                assertEquals(1, response.getContent().get(0).getVersions().size());
                assertTrue(response.getContent().get(0).getVersions().get(0).getIsDefault());
        }

        @Test
        void getMyPackages_whenPaged_shouldReturnBothActiveAndInactivePackages() {
                ServicePackage activePackage = new ServicePackage();
                activePackage.setId(packageId);
                activePackage.setMentorId(mentorId);
                activePackage.setName("Career Planning");
                activePackage.setDescription("Active package");
                activePackage.setIsActive(true);

                UUID inactivePackageId = UUID.randomUUID();
                UUID inactiveVersionId = UUID.randomUUID();
                ServicePackage inactivePackage = new ServicePackage();
                inactivePackage.setId(inactivePackageId);
                inactivePackage.setMentorId(mentorId);
                inactivePackage.setName("CV Review");
                inactivePackage.setDescription("Inactive package");
                inactivePackage.setIsActive(false);

                PageRequest pageable = PageRequest.of(0, 5);
                when(servicePackageRepository.findByMentorId(eq(mentorId), eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(activePackage, inactivePackage), pageable, 2));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());
                when(servicePackageVersionRepository.findByPackageId(inactivePackageId))
                                .thenReturn(List.of(savedVersionFor(inactivePackageId, inactiveVersionId, false)));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(inactiveVersionId))
                                .thenReturn(List.of());

                Page<MentorDto.ServicePackageResponse> response = catalogService.getMyPackages(mentorId, null,
                                pageable);

                assertEquals(2, response.getTotalElements());
                assertEquals(List.of("Career Planning", "CV Review"),
                                response.getContent().stream().map(MentorDto.ServicePackageResponse::getName).toList());
                assertEquals(List.of(true, false),
                                response.getContent().stream().map(MentorDto.ServicePackageResponse::getIsActive)
                                                .toList());
        }

        @Test
        void getMyPackage_whenOwnedPackageExists_shouldReturnFullPackageDetail() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(false);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageResponse response = catalogService.getMyPackage(mentorId, packageId);

                assertEquals(packageId, response.getId());
                assertEquals("Career Planning", response.getName());
                assertEquals(false, response.getIsActive());
                assertEquals(1, response.getVersions().size());
        }

        @Test
        void getPackageVersions_whenOwnedPackageExists_shouldReturnPagedVersions() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);

                PageRequest pageable = PageRequest.of(0, 5);
                UUID secondVersionId = UUID.randomUUID();
                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findByPackageId(packageId, pageable))
                                .thenReturn(new PageImpl<>(
                                                List.of(savedVersion(),
                                                                savedVersionFor(packageId, secondVersionId, false)),
                                                pageable,
                                                2));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(secondVersionId))
                                .thenReturn(List.of());

                Page<MentorDto.ServicePackageVersionResponse> response = catalogService.getPackageVersions(mentorId,
                                packageId, pageable);

                assertEquals(2, response.getTotalElements());
                assertEquals(2, response.getContent().size());
                assertTrue(response.getContent().get(0).getIsDefault());
                assertEquals(false, response.getContent().get(1).getIsDefault());
        }

        @Test
        void getPackageVersion_whenOwnedVersionExists_shouldReturnVersionDetail() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                ServicePackageVersion version = savedVersion();

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageVersionResponse response = catalogService.getPackageVersion(mentorId, packageId,
                                versionId);

                assertEquals(versionId, response.getId());
                assertEquals(2, response.getCurriculums().size());
                assertTrue(response.getIsDefault());
        }

        @Test
        void getActivePackages_whenPaged_shouldReturnOnlyActiveCatalogPackages() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(true);

                PageRequest pageable = PageRequest.of(0, 5);
                when(servicePackageRepository.findByIsActiveTrueAndDeletedAtIsNull(eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(servicePackage), pageable, 1));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                Page<MentorDto.ServicePackageResponse> response = catalogService.getActivePackages(null, null,
                                pageable);

                assertEquals(1, response.getTotalElements());
                assertEquals("Career Planning", response.getContent().get(0).getName());
                assertEquals(1, response.getContent().get(0).getVersions().size());
                assertTrue(response.getContent().get(0).getVersions().get(0).getIsDefault());
        }

        @Test
        void getActivePackage_whenPackageExists_shouldReturnCatalogPackageResponse() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(true);

                when(servicePackageRepository.findActiveById(packageId))
                                .thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageResponse response = catalogService.getActivePackage(packageId);

                assertEquals(packageId, response.getId());
                assertEquals("Career Planning", response.getName());
                assertEquals(1, response.getVersions().size());
        }

        @Test
        void getActivePackage_whenPackageMissing_shouldThrowPackageNotFound() {
                when(servicePackageRepository.findActiveById(packageId)).thenReturn(Optional.empty());

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.getActivePackage(packageId));

                assertSame(ServiceErrorCode.PACKAGE_NOT_FOUND, exception.getExceptionCode());
        }

        @Test
        void createPackageVersion_whenOwnedPackageExists_shouldCreateNewDefaultVersionAndCloneCurriculums() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(true);

                ServicePackageVersion oldDefaultVersion = savedVersion();
                oldDefaultVersion.setIsDefault(true);

                UUID newVersionId = UUID.randomUUID();
                ServicePackageVersion newVersion = new ServicePackageVersion();
                newVersion.setId(newVersionId);
                newVersion.setPackageId(packageId);
                newVersion.setPrice(new BigDecimal("150.00"));
                newVersion.setDuration(4);
                newVersion.setDeliveryType("ONLINE");
                newVersion.setIsDefault(true);

                CreateServicePackageVersionRequest request = new CreateServicePackageVersionRequest();
                request.setPrice(new BigDecimal("150.00"));
                request.setDuration(4);
                request.setDeliveryType("ONLINE");

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findByPackageId(packageId))
                                .thenReturn(List.of(oldDefaultVersion))
                                .thenReturn(List.of(oldDefaultVersion, newVersion));
                when(servicePackageVersionRepository.saveAll(any()))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(servicePackageVersionRepository.save(any(ServicePackageVersion.class))).thenAnswer(invocation -> {
                        ServicePackageVersion version = invocation.getArgument(0);
                        version.setId(newVersionId);
                        return version;
                });
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(newVersionId))
                                .thenReturn(savedClonedCurriculums(newVersionId));

                MentorDto.ServicePackageResponse response = catalogService.createPackageVersion(mentorId, packageId,
                                request);

                ArgumentCaptor<List<ServicePackageVersion>> versionsCaptor = ArgumentCaptor.forClass(List.class);
                verify(servicePackageVersionRepository).saveAll(versionsCaptor.capture());
                assertFalse(versionsCaptor.getValue().get(0).getIsDefault());

                ArgumentCaptor<ServicePackageVersion> newVersionCaptor = ArgumentCaptor
                                .forClass(ServicePackageVersion.class);
                verify(servicePackageVersionRepository).save(newVersionCaptor.capture());
                assertTrue(newVersionCaptor.getValue().getIsDefault());
                assertEquals(new BigDecimal("150.00"), newVersionCaptor.getValue().getPrice());

                ArgumentCaptor<List<PackageCurriculum>> curriculumCaptor = ArgumentCaptor.forClass(List.class);
                verify(packageCurriculumRepository).saveAll(curriculumCaptor.capture());
                assertEquals(2, curriculumCaptor.getValue().size());
                assertTrue(curriculumCaptor.getValue().stream()
                                .allMatch(item -> newVersionId.equals(item.getPackageVersionId())));

                assertEquals(2, response.getVersions().size());
                assertEquals(1, response.getVersions().stream()
                                .filter(MentorDto.ServicePackageVersionResponse::getIsDefault).count());
        }

        @Test
        void createPackageVersion_whenNoDefaultVersionExists_shouldThrowVersionNotFound() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);

                CreateServicePackageVersionRequest request = new CreateServicePackageVersionRequest();
                request.setPrice(new BigDecimal("150.00"));
                request.setDuration(4);
                request.setDeliveryType("ONLINE");

                ServicePackageVersion nonDefaultVersion = savedVersion();
                nonDefaultVersion.setIsDefault(false);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(nonDefaultVersion));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.createPackageVersion(mentorId, packageId, request));

                assertSame(ServiceErrorCode.SERVICE_VERSION_NOT_FOUND, exception.getExceptionCode());
                verify(servicePackageVersionRepository, never()).save(any(ServicePackageVersion.class));
        }

        @Test
        void createPackageVersion_whenRequesterIsNotOwner_shouldThrowPackageNotFound() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(UUID.randomUUID());

                CreateServicePackageVersionRequest request = new CreateServicePackageVersionRequest();
                request.setPrice(new BigDecimal("150.00"));
                request.setDuration(4);
                request.setDeliveryType("ONLINE");

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.createPackageVersion(mentorId, packageId, request));

                assertSame(ServiceErrorCode.PACKAGE_NOT_FOUND, exception.getExceptionCode());
                verify(servicePackageVersionRepository, never()).findByPackageId(packageId);
        }

        @Test
        void updatePackage_whenOwnedPackageExists_shouldUpdateMetadata() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Old package");
                servicePackage.setDescription("Old description");
                servicePackage.setIsActive(true);

                UpdateServicePackageRequest request = new UpdateServicePackageRequest();
                request.setName("Updated package");
                request.setDescription("Updated description");

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageRepository.save(any(ServicePackage.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageResponse response = catalogService.updatePackage(mentorId, packageId, request);

                assertEquals("Updated package", response.getName());
                assertEquals("Updated description", response.getDescription());
                ArgumentCaptor<ServicePackage> packageCaptor = ArgumentCaptor.forClass(ServicePackage.class);
                verify(servicePackageRepository).save(packageCaptor.capture());
                assertEquals("Updated package", packageCaptor.getValue().getName());
                assertEquals("Updated description", packageCaptor.getValue().getDescription());
        }

        @Test
        void updatePackage_whenRequesterIsNotOwner_shouldThrowPackageNotFound() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(UUID.randomUUID());

                UpdateServicePackageRequest request = new UpdateServicePackageRequest();
                request.setName("Updated package");
                request.setDescription("Updated description");

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.updatePackage(mentorId, packageId, request));

                assertSame(ServiceErrorCode.PACKAGE_NOT_FOUND, exception.getExceptionCode());
                verify(servicePackageRepository, never()).save(any(ServicePackage.class));
        }

        @Test
        void togglePackage_whenOwnedActivePackageExists_shouldDisablePackage() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(true);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageRepository.save(any(ServicePackage.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageResponse response = catalogService.togglePackage(mentorId, packageId);

                assertFalse(response.getIsActive());
                ArgumentCaptor<ServicePackage> packageCaptor = ArgumentCaptor.forClass(ServicePackage.class);
                verify(servicePackageRepository).save(packageCaptor.capture());
                assertFalse(packageCaptor.getValue().getIsActive());
        }

        @Test
        void togglePackage_whenOwnedInactivePackageExists_shouldEnablePackage() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setName("Career Planning");
                servicePackage.setDescription("Package description");
                servicePackage.setIsActive(false);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageRepository.save(any(ServicePackage.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                MentorDto.ServicePackageResponse response = catalogService.togglePackage(mentorId, packageId);

                assertTrue(response.getIsActive());
        }

        @Test
        void togglePackage_whenRequesterIsNotOwner_shouldThrowPackageNotFound() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(UUID.randomUUID());

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.togglePackage(mentorId, packageId));

                assertSame(ServiceErrorCode.PACKAGE_NOT_FOUND, exception.getExceptionCode());
                verify(servicePackageRepository, never()).save(any(ServicePackage.class));
        }

        @Test
        void deletePackage_whenOwnedPackageExists_shouldArchiveAndDisablePackage() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setIsActive(true);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageRepository.save(any(ServicePackage.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                catalogService.deletePackage(mentorId, packageId);

                ArgumentCaptor<ServicePackage> packageCaptor = ArgumentCaptor.forClass(ServicePackage.class);
                verify(servicePackageRepository).save(packageCaptor.capture());
                assertFalse(packageCaptor.getValue().getIsActive());
                assertNotNull(packageCaptor.getValue().getDeletedAt());
                verify(servicePackageRepository, never()).delete(any(ServicePackage.class));
                verify(servicePackageVersionRepository, never()).delete(any(ServicePackageVersion.class));
        }

        @Test
        void deletePackage_whenRequesterIsNotOwner_shouldThrowPackageNotFound() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(UUID.randomUUID());

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.deletePackage(mentorId, packageId));

                assertSame(ServiceErrorCode.PACKAGE_NOT_FOUND, exception.getExceptionCode());
                verify(servicePackageRepository, never()).save(any(ServicePackage.class));
        }

        @Test
        void deletePackage_whenAlreadyArchived_shouldThrowConflict() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                servicePackage.setIsActive(false);
                servicePackage.setDeletedAt(Instant.now());

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.deletePackage(mentorId, packageId));

                assertSame(ServiceErrorCode.PACKAGE_ALREADY_ARCHIVED, exception.getExceptionCode());
                verify(servicePackageRepository, never()).save(any(ServicePackage.class));
        }

        @Test
        void getMyPackages_whenPackageArchived_shouldReturnArchiveFlag() {
                ServicePackage archivedPackage = new ServicePackage();
                archivedPackage.setId(packageId);
                archivedPackage.setMentorId(mentorId);
                archivedPackage.setName("Archived package");
                archivedPackage.setDescription("Archived package");
                archivedPackage.setIsActive(false);
                archivedPackage.setDeletedAt(Instant.now());

                PageRequest pageable = PageRequest.of(0, 5);
                when(servicePackageRepository.findByMentorId(eq(mentorId), eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(archivedPackage), pageable, 1));
                when(servicePackageVersionRepository.findByPackageId(packageId)).thenReturn(List.of(savedVersion()));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId))
                                .thenReturn(savedCurriculums());

                Page<MentorDto.ServicePackageResponse> response = catalogService.getMyPackages(mentorId, null,
                                pageable);

                assertEquals(1, response.getTotalElements());
                assertTrue(response.getContent().get(0).getIsArchived());
                assertFalse(response.getContent().get(0).getIsActive());
        }

        @Test
        void updateCurriculumItem_whenOwnedCurriculumExists_shouldUpdateCurriculum() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);

                ServicePackageVersion version = savedVersion();
                UUID curriculumId = UUID.randomUUID();
                PackageCurriculum curriculum = new PackageCurriculum();
                curriculum.setId(curriculumId);
                curriculum.setPackageVersionId(versionId);
                curriculum.setTitle("Session 1");
                curriculum.setDescription("Intro");
                curriculum.setOrderIndex(1);
                curriculum.setDuration(60);

                MentorDto.CurriculumItemRequest request = new MentorDto.CurriculumItemRequest();
                request.setTitle("Updated Session 1");
                request.setDescription("Updated intro");
                request.setOrderIndex(2);
                request.setDuration(75);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.findById(curriculumId)).thenReturn(Optional.of(curriculum));
                when(packageCurriculumRepository.existsByPackageVersionIdAndOrderIndexAndIdNot(versionId, 2,
                                curriculumId)).thenReturn(false);
                when(packageCurriculumRepository.save(any(PackageCurriculum.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                MentorDto.CurriculumItemResponse response = catalogService.updateCurriculumItem(mentorId, packageId,
                                versionId, curriculumId, request);

                assertEquals("Updated Session 1", response.getTitle());
                assertEquals(2, response.getOrderIndex());
                ArgumentCaptor<PackageCurriculum> curriculumCaptor = ArgumentCaptor.forClass(PackageCurriculum.class);
                verify(packageCurriculumRepository).save(curriculumCaptor.capture());
                assertEquals("Updated Session 1", curriculumCaptor.getValue().getTitle());
                assertEquals(75, curriculumCaptor.getValue().getDuration());
        }

        @Test
        void updateCurriculumItem_whenOrderIndexDuplicated_shouldThrowBusinessException() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);

                ServicePackageVersion version = savedVersion();
                UUID curriculumId = UUID.randomUUID();
                PackageCurriculum curriculum = new PackageCurriculum();
                curriculum.setId(curriculumId);
                curriculum.setPackageVersionId(versionId);

                MentorDto.CurriculumItemRequest request = new MentorDto.CurriculumItemRequest();
                request.setTitle("Updated Session 1");
                request.setDescription("Updated intro");
                request.setOrderIndex(2);
                request.setDuration(75);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.findById(curriculumId)).thenReturn(Optional.of(curriculum));
                when(packageCurriculumRepository.existsByPackageVersionIdAndOrderIndexAndIdNot(versionId, 2,
                                curriculumId)).thenReturn(true);

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.updateCurriculumItem(mentorId, packageId, versionId, curriculumId,
                                                request));

                assertSame(ServiceErrorCode.DUPLICATE_CURRICULUM_ORDER_INDEX, exception.getExceptionCode());
                verify(packageCurriculumRepository, never()).save(any(PackageCurriculum.class));
        }

        @Test
        void updateCurriculumItem_whenCurriculumDoesNotBelongToVersion_shouldThrowCurriculumNotFound() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);

                ServicePackageVersion version = savedVersion();
                UUID curriculumId = UUID.randomUUID();
                PackageCurriculum curriculum = new PackageCurriculum();
                curriculum.setId(curriculumId);
                curriculum.setPackageVersionId(UUID.randomUUID());

                MentorDto.CurriculumItemRequest request = new MentorDto.CurriculumItemRequest();
                request.setTitle("Updated Session 1");
                request.setDescription("Updated intro");
                request.setOrderIndex(2);
                request.setDuration(75);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.findById(curriculumId)).thenReturn(Optional.of(curriculum));

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.updateCurriculumItem(mentorId, packageId, versionId, curriculumId,
                                                request));

                assertSame(ServiceErrorCode.CURRICULUM_NOT_FOUND, exception.getExceptionCode());
                verify(packageCurriculumRepository, never()).save(any(PackageCurriculum.class));
        }

        @Test
        void addCurriculumItem_whenOrderIndexDuplicatedInVersion_shouldThrowBusinessException() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);

                ServicePackageVersion version = savedVersion();
                MentorDto.CurriculumItemRequest request = new MentorDto.CurriculumItemRequest();
                request.setTitle("Session 3");
                request.setDescription("Wrap up");
                request.setOrderIndex(1);
                request.setDuration(45);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.existsByPackageVersionIdAndOrderIndex(versionId, 1)).thenReturn(true);

                AppException exception = assertThrows(AppException.class,
                                () -> catalogService.addCurriculumItem(mentorId, packageId, versionId, request));

                assertSame(ServiceErrorCode.DUPLICATE_CURRICULUM_ORDER_INDEX, exception.getExceptionCode());
        }

        @Test
        void listCurriculum_whenPaged_shouldReturnOrderedPage() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                ServicePackageVersion version = savedVersion();
                PageRequest pageable = PageRequest.of(0, 10);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(versionId, pageable))
                                .thenReturn(new PageImpl<>(savedCurriculums(), pageable, 2));

                Page<MentorDto.CurriculumItemResponse> response = catalogService.listCurriculum(mentorId, packageId,
                                versionId, pageable);

                assertEquals(2, response.getTotalElements());
                assertEquals(List.of(1, 2), response.getContent().stream()
                                .map(MentorDto.CurriculumItemResponse::getOrderIndex)
                                .toList());
        }

        @Test
        void deleteCurriculumItemByTree_whenOwnedCurriculumExists_shouldDeleteCurriculum() {
                ServicePackage servicePackage = new ServicePackage();
                servicePackage.setId(packageId);
                servicePackage.setMentorId(mentorId);
                ServicePackageVersion version = savedVersion();
                UUID curriculumId = UUID.randomUUID();
                PackageCurriculum curriculum = new PackageCurriculum();
                curriculum.setId(curriculumId);
                curriculum.setPackageVersionId(versionId);

                when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
                when(servicePackageVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
                when(packageCurriculumRepository.findById(curriculumId)).thenReturn(Optional.of(curriculum));

                catalogService.deleteCurriculumItem(mentorId, packageId, versionId, curriculumId);

                verify(packageCurriculumRepository).delete(curriculum);
        }

        private CreateServicePackageRequest validRequest() {
                CreateServicePackageRequest request = new CreateServicePackageRequest();
                request.setName("Career Planning");
                request.setDescription("Package description");
                request.setPrice(new BigDecimal("120.00"));
                request.setDuration(3);
                request.setDeliveryType("ONLINE");

                CreateServicePackageRequest.CurriculumRequest first = new CreateServicePackageRequest.CurriculumRequest();
                first.setTitle("Session 1");
                first.setDescription("Intro");
                first.setOrderIndex(1);
                first.setDuration(60);

                CreateServicePackageRequest.CurriculumRequest second = new CreateServicePackageRequest.CurriculumRequest();
                second.setTitle("Session 2");
                second.setDescription("Deep dive");
                second.setOrderIndex(2);
                second.setDuration(90);

                request.setCurriculums(List.of(first, second));
                return request;
        }

        private ServicePackageVersion savedVersion() {
                ServicePackageVersion version = new ServicePackageVersion();
                version.setId(versionId);
                version.setPackageId(packageId);
                version.setPrice(new BigDecimal("120.00"));
                version.setDuration(3);
                version.setDeliveryType("ONLINE");
                version.setIsDefault(true);
                return version;
        }

        private ServicePackageVersion savedVersionFor(UUID ownedPackageId, UUID ownedVersionId, boolean isDefault) {
                ServicePackageVersion version = new ServicePackageVersion();
                version.setId(ownedVersionId);
                version.setPackageId(ownedPackageId);
                version.setPrice(new BigDecimal("80.00"));
                version.setDuration(2);
                version.setDeliveryType("OFFLINE");
                version.setIsDefault(isDefault);
                return version;
        }

        private List<PackageCurriculum> savedCurriculums() {
                PackageCurriculum first = new PackageCurriculum();
                first.setId(UUID.randomUUID());
                first.setPackageVersionId(versionId);
                first.setTitle("Session 1");
                first.setDescription("Intro");
                first.setOrderIndex(1);
                first.setDuration(60);

                PackageCurriculum second = new PackageCurriculum();
                second.setId(UUID.randomUUID());
                second.setPackageVersionId(versionId);
                second.setTitle("Session 2");
                second.setDescription("Deep dive");
                second.setOrderIndex(2);
                second.setDuration(90);

                return List.of(first, second);
        }

        private List<PackageCurriculum> savedClonedCurriculums(UUID clonedVersionId) {
                PackageCurriculum first = new PackageCurriculum();
                first.setId(UUID.randomUUID());
                first.setPackageVersionId(clonedVersionId);
                first.setTitle("Session 1");
                first.setDescription("Intro");
                first.setOrderIndex(1);
                first.setDuration(60);

                PackageCurriculum second = new PackageCurriculum();
                second.setId(UUID.randomUUID());
                second.setPackageVersionId(clonedVersionId);
                second.setTitle("Session 2");
                second.setDescription("Deep dive");
                second.setOrderIndex(2);
                second.setDuration(90);

                return List.of(first, second);
        }
}
