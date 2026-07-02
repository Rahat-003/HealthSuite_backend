package com.healthsuite.marketplace.service;

import com.healthsuite.auth.entity.Role;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.RoleRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.marketplace.dto.request.CreateConsultationRequest;
import com.healthsuite.marketplace.dto.request.DoctorRegistrationRequest;
import com.healthsuite.marketplace.dto.response.AdminMarketplaceStatsResponse;
import com.healthsuite.marketplace.dto.response.ConsultationMediaResponse;
import com.healthsuite.marketplace.dto.response.ConsultationRequestResponse;
import com.healthsuite.marketplace.dto.response.DoctorDocumentResponse;
import com.healthsuite.marketplace.dto.response.DoctorProfileResponse;
import com.healthsuite.marketplace.entity.ConsultationMedia;
import com.healthsuite.marketplace.entity.ConsultationOffer;
import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.entity.DoctorDocument;
import com.healthsuite.marketplace.entity.DoctorProfile;
import com.healthsuite.marketplace.enums.DoctorDocumentType;
import com.healthsuite.marketplace.repository.DoctorDocumentRepository;
import com.healthsuite.phr.service.FileStorageService;
import com.healthsuite.marketplace.enums.ConsultationMediaType;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.DoctorStatus;
import com.healthsuite.marketplace.enums.OfferStatus;
import com.healthsuite.marketplace.repository.ConsultationMediaRepository;
import com.healthsuite.marketplace.repository.ConsultationOfferRepository;
import com.healthsuite.marketplace.repository.ConsultationRequestRepository;
import com.healthsuite.marketplace.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationOfferRepository consultationOfferRepository;
    private final ConsultationMediaRepository consultationMediaRepository;
    private final ConsultationMediaStorageService mediaStorageService;
    private final DoctorDocumentRepository doctorDocumentRepository;
    private final DoctorDocumentStorageService doctorDocumentStorageService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // ── Doctor registration ────────────────────────────────────────────────

    @Transactional
    public DoctorProfileResponse registerDoctor(DoctorRegistrationRequest req, Long userId) {
        if (doctorProfileRepository.existsByUserId(userId)) {
            throw new ConflictException("You already have a doctor profile. Contact support to update it.");
        }
        if (doctorProfileRepository.existsByLicenseNumber(req.licenseNumber())) {
            throw new ConflictException("A profile with this license number already exists.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        DoctorProfile profile = DoctorProfile.builder()
            .userId(userId)
            .fullName(user.getFullName())
            .specialty(req.specialty())
            .qualifications(req.qualifications())
            .experienceYears(req.experienceYears())
            .licenseNumber(req.licenseNumber())
            .bio(req.bio())
            .hospitalAffiliation(req.hospitalAffiliation())
            .availabilityNote(req.availabilityNote())
            .consultationFeeBdt(req.consultationFeeBdt())
            .build();

        return DoctorProfileResponse.from(doctorProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getMyDoctorProfile(Long userId) {
        return doctorProfileRepository.findByUserId(userId)
            .map(DoctorProfileResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile for user", userId));
    }

    // ── Public doctor browsing ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<DoctorProfileResponse> listDoctors(String specialty, String search, Pageable pageable) {
        // empty string (not null) — null params inside LOWER()/CONCAT() break Postgres binding
        Page<DoctorProfile> page = doctorProfileRepository.searchActive(
            specialty != null && !specialty.isBlank() ? specialty.trim() : "",
            search != null && !search.isBlank() ? search.trim() : "",
            pageable
        );
        return PagedResponse.from(page.map(DoctorProfileResponse::from));
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getDoctorById(Long id) {
        DoctorProfile profile = doctorProfileRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile", id));
        if (profile.getStatus() != DoctorStatus.ACTIVE) {
            throw new ResourceNotFoundException("DoctorProfile", id);
        }
        return DoctorProfileResponse.from(profile);
    }

    // ── Consultation request ───────────────────────────────────────────────

    @Transactional
    public ConsultationRequestResponse createConsultation(CreateConsultationRequest req, Long patientId) {
        List<DoctorProfile> doctors = doctorProfileRepository.findAllById(req.doctorProfileIds());

        if (doctors.size() != req.doctorProfileIds().size()) {
            throw new ResourceNotFoundException("One or more selected doctors not found");
        }
        doctors.forEach(d -> {
            if (d.getStatus() != DoctorStatus.ACTIVE) {
                throw new ConflictException("Doctor " + d.getFullName() + " is not currently accepting consultations.");
            }
        });

        LocalDateTime now = LocalDateTime.now();
        ConsultationRequest request = ConsultationRequest.builder()
            .patientId(patientId)
            .problemText(req.problemText())
            .refundWindowHours(req.refundWindowHours())
            .expiresAt(now.plusHours(req.refundWindowHours()))
            .build();

        List<ConsultationOffer> offers = doctors.stream()
            .map(d -> ConsultationOffer.builder().request(request).doctorProfile(d).build())
            .toList();
        request.getOffers().addAll(offers);

        return ConsultationRequestResponse.from(consultationRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ConsultationRequestResponse> getMyConsultations(Long patientId) {
        return consultationRequestRepository.findByPatientId(patientId)
            .stream()
            .map(r -> ConsultationRequestResponse.from(r, consultationMediaRepository.findByConsultationId(r.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationRequestResponse getConsultation(Long id, Long patientId) {
        ConsultationRequest req = consultationRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationRequest", id));
        if (!req.getPatientId().equals(patientId)) {
            throw new AccessDeniedException("Access denied");
        }
        return ConsultationRequestResponse.from(req, consultationMediaRepository.findByConsultationId(id));
    }

    @Transactional
    public ConsultationMediaResponse uploadMedia(Long consultationId, Long userId, MultipartFile file, ConsultationMediaType type) {
        ConsultationRequest req = consultationRequestRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationRequest", consultationId));
        if (!req.getPatientId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ConsultationMediaStorageService.StoredFile stored = mediaStorageService.store(file);

        ConsultationMedia media = ConsultationMedia.builder()
            .consultationId(consultationId)
            .userUuid(user.getUserUuid())
            .fileName(stored.fileName())
            .filePath(stored.filePath())
            .mediaType(type)
            .build();

        return ConsultationMediaResponse.from(consultationMediaRepository.save(media));
    }

    // ── Doctor-side queue (first response wins) ─────────────────────────────

    private DoctorProfile requireActiveDoctor(Long userId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile for user", userId));
        if (profile.getStatus() != DoctorStatus.ACTIVE) {
            throw new AccessDeniedException("Your doctor profile is not active yet.");
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public List<ConsultationRequestResponse> getDoctorQueue(Long userId) {
        DoctorProfile profile = requireActiveDoctor(userId);
        return consultationOfferRepository
            .findPendingQueueForDoctor(profile.getId(), LocalDateTime.now())
            .stream()
            .map(o -> ConsultationRequestResponse.from(
                o.getRequest(),
                consultationMediaRepository.findByConsultationId(o.getRequest().getId())
            ))
            .toList();
    }

    @Transactional
    public ConsultationRequestResponse acceptCase(Long consultationId, Long userId) {
        DoctorProfile profile = requireActiveDoctor(userId);

        ConsultationOffer offer = consultationOfferRepository
            .findByRequestIdAndDoctorProfileId(consultationId, profile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationOffer for consultation", consultationId));

        ConsultationRequest request = offer.getRequest();
        if (request.getStatus() != ConsultationStatus.QUEUED) {
            throw new ConflictException("This case has already been claimed or is no longer available.");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ConflictException("Your offer on this case is no longer pending.");
        }
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("This case has expired.");
        }

        LocalDateTime now = LocalDateTime.now();
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(now);
        request.getOffers().stream()
            .filter(o -> !o.getId().equals(offer.getId()) && o.getStatus() == OfferStatus.PENDING)
            .forEach(o -> o.setStatus(OfferStatus.INVALIDATED));
        request.setStatus(ConsultationStatus.ACTIVE);

        log.info("Doctor {} accepted consultation {}", profile.getId(), consultationId);
        return ConsultationRequestResponse.from(
            consultationRequestRepository.save(request),
            consultationMediaRepository.findByConsultationId(consultationId)
        );
    }

    @Transactional
    public void passCase(Long consultationId, Long userId) {
        DoctorProfile profile = requireActiveDoctor(userId);

        ConsultationOffer offer = consultationOfferRepository
            .findByRequestIdAndDoctorProfileId(consultationId, profile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationOffer for consultation", consultationId));

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ConflictException("Your offer on this case is no longer pending.");
        }
        offer.setStatus(OfferStatus.INVALIDATED);
        offer.setRespondedAt(LocalDateTime.now());
        consultationOfferRepository.save(offer);
        log.info("Doctor {} passed consultation {}", profile.getId(), consultationId);
    }

    @Transactional(readOnly = true)
    public List<ConsultationRequestResponse> getDoctorActiveCases(Long userId) {
        DoctorProfile profile = requireActiveDoctor(userId);
        return consultationOfferRepository
            .findByDoctorProfileIdAndStatus(profile.getId(), OfferStatus.ACCEPTED)
            .stream()
            .map(o -> ConsultationRequestResponse.from(
                o.getRequest(),
                consultationMediaRepository.findByConsultationId(o.getRequest().getId())
            ))
            .toList();
    }

    // ── Doctor credential documents ─────────────────────────────────────────

    @Transactional
    public DoctorDocumentResponse uploadDoctorDocument(Long userId, MultipartFile file, DoctorDocumentType type) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile for user", userId));

        String fileName;
        String filePath;
        if (type == DoctorDocumentType.HEADSHOT) {
            // headshots go to public storage so the marketplace can show the profile photo;
            // filePath holds the public /files/... URL
            String publicUrl = fileStorageService.uploadFile(file, "doctor_photos");
            profile.setProfilePhotoUrl(publicUrl);
            doctorProfileRepository.save(profile);
            fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "headshot";
            filePath = publicUrl;
        } else {
            // certificates stay in private storage, streamed via the admin download endpoint
            DoctorDocumentStorageService.StoredFile stored = doctorDocumentStorageService.store(file);
            fileName = stored.fileName();
            filePath = stored.filePath();
        }

        DoctorDocument doc = DoctorDocument.builder()
            .doctorProfileId(profile.getId())
            .docType(type)
            .fileName(fileName)
            .filePath(filePath)
            .build();
        return DoctorDocumentResponse.from(doctorDocumentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<DoctorDocumentResponse> adminListDoctorDocuments(Long profileId) {
        if (!doctorProfileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("DoctorProfile", profileId);
        }
        return doctorDocumentRepository.findByDoctorProfileId(profileId)
            .stream().map(DoctorDocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorDocument adminGetDoctorDocument(Long profileId, Long docId) {
        DoctorDocument doc = doctorDocumentRepository.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorDocument", docId));
        if (!doc.getDoctorProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("DoctorDocument", docId);
        }
        return doc;
    }

    // ── Media download (patient owner or offered doctor) ───────────────────

    @Transactional(readOnly = true)
    public ConsultationMedia getMediaForDownload(Long consultationId, Long mediaId, Long userId) {
        ConsultationRequest request = consultationRequestRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationRequest", consultationId));

        boolean isPatient = request.getPatientId().equals(userId);
        boolean isOfferedDoctor = doctorProfileRepository.findByUserId(userId)
            .map(p -> consultationOfferRepository.existsByRequestIdAndDoctorProfileId(consultationId, p.getId()))
            .orElse(false);
        if (!isPatient && !isOfferedDoctor) {
            throw new AccessDeniedException("Access denied");
        }

        ConsultationMedia media = consultationMediaRepository.findById(mediaId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationMedia", mediaId));
        if (!media.getConsultationId().equals(consultationId)) {
            throw new ResourceNotFoundException("ConsultationMedia", mediaId);
        }
        return media;
    }

    // ── Admin operations ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminMarketplaceStatsResponse adminGetStats() {
        return new AdminMarketplaceStatsResponse(
            doctorProfileRepository.countByStatus(DoctorStatus.PENDING),
            doctorProfileRepository.countByStatus(DoctorStatus.ACTIVE),
            consultationRequestRepository.sumEscrowHeld()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<DoctorProfileResponse> adminListDoctors(DoctorStatus status, Pageable pageable) {
        return PagedResponse.from(
            doctorProfileRepository.findByStatus(status, pageable).map(DoctorProfileResponse::from)
        );
    }

    @Transactional
    public DoctorProfileResponse approveDoctor(Long profileId) {
        DoctorProfile profile = doctorProfileRepository.findById(profileId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile", profileId));

        profile.setStatus(DoctorStatus.ACTIVE);

        User user = userRepository.findById(profile.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", profile.getUserId()));
        Role doctorRole = roleRepository.findByName(RoleName.ROLE_DOCTOR)
            .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_DOCTOR not seeded"));
        user.getRoles().add(doctorRole);
        userRepository.save(user);

        return DoctorProfileResponse.from(doctorProfileRepository.save(profile));
    }

    @Transactional
    public DoctorProfileResponse rejectDoctor(Long profileId, String reason) {
        DoctorProfile profile = doctorProfileRepository.findById(profileId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile", profileId));
        profile.setStatus(DoctorStatus.SUSPENDED);
        profile.setRejectionReason(reason);
        return DoctorProfileResponse.from(doctorProfileRepository.save(profile));
    }
}
