package com.hms.assessment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.assessment.client.PatientClient;
import com.hms.assessment.dto.CaptureRequest;
import com.hms.assessment.dto.CaptureResponse;
import com.hms.assessment.dto.ConfirmCaptureRequest;
import com.hms.assessment.entity.Assessment;
import com.hms.assessment.entity.PoseCapture;
import com.hms.assessment.enums.CaptureStatus;
import com.hms.assessment.repository.PoseCaptureRepository;
import com.hms.assessment.storage.ImageStorage;
import com.hms.assessment.storage.ImageValidator;
import com.hms.assessment.util.Json;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.SecurityUtils;

/**
 * Assistive camera measurements: suggestions until a clinician confirms them.
 */
@Service
public class PoseCaptureService {

	private static final Pattern ANGLE_KEY = Pattern.compile("^[a-z][a-z0-9_]{0,59}$");

	public record ImageData(byte[] bytes, String contentType) {
	}

	private final PoseCaptureRepository captureRepository;
	private final AssessmentService assessmentService;
	private final PatientClient patientClient;
	private final ImageStorage imageStorage;
	private final AuditPublisher audit;
	private final Clock clock;
	private final boolean requireConsent;
	private final long maxImageBytes;

	public PoseCaptureService(PoseCaptureRepository captureRepository, AssessmentService assessmentService,
			PatientClient patientClient, ImageStorage imageStorage, AuditPublisher audit, Clock clock,
			@Value("${physio.assessment.require-pose-consent:true}") boolean requireConsent,
			@Value("${physio.assessment.max-image-bytes:5242880}") long maxImageBytes) {
		this.captureRepository = captureRepository;
		this.assessmentService = assessmentService;
		this.patientClient = patientClient;
		this.imageStorage = imageStorage;
		this.audit = audit;
		this.clock = clock;
		this.requireConsent = requireConsent;
		this.maxImageBytes = maxImageBytes;
	}

	@Transactional
	public CaptureResponse create(UUID assessmentId, CaptureRequest r) {
		Assessment a = assessmentService.requireDraft(assessmentId);
		if (requireConsent && !patientClient.hasConsent(a.getPatientId(), "CAMERA_POSE_ANALYSIS")) {
			throw new BusinessException("The patient has not consented to camera-based analysis");
		}
		Map<String, Double> angles = checkAngles(r.angles());
		if (r.questionRef() != null && !r.questionRef().isBlank()) {
			assertAngleQuestion(a, r.questionRef());
		}
		PoseCapture c = new PoseCapture();
		c.setClinicId(a.getClinicId());
		c.setAssessmentId(a.getId());
		c.setPatientId(a.getPatientId());
		c.setKind(r.kind());
		c.setViewType(r.view());
		c.setBodyRegion(r.bodyRegion());
		c.setLaterality(r.laterality());
		c.setAngles(Json.write(angles));
		c.setQualityScore(r.qualityScore());
		c.setToolName(r.toolName());
		c.setToolVersion(r.toolVersion());
		c.setQuestionRef(r.questionRef() == null || r.questionRef().isBlank() ? null : r.questionRef().trim());
		c.setNote(r.note());
		c.setStatus(CaptureStatus.SUGGESTED);
		PoseCapture saved = captureRepository.save(c);
		audit.record("POSE_CAPTURE_RECORDED", "PoseCapture", saved.getId(),
				Map.of("assessmentId", assessmentId.toString(), "kind", r.kind().name()));
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<CaptureResponse> list(UUID assessmentId) {
		assessmentService.load(assessmentId);
		return captureRepository.findByAssessmentIdOrderByCreatedAtAsc(assessmentId).stream().map(this::toResponse)
				.toList();
	}

	/**
	 * The clinician accepts (optionally adjusting) the angles. Only now can they
	 * fill a form answer.
	 */
	@Transactional
	public CaptureResponse confirm(UUID assessmentId, UUID captureId, ConfirmCaptureRequest r) {
		Assessment a = assessmentService.requireDraft(assessmentId);
		PoseCapture c = find(assessmentId, captureId);
		if (c.getStatus() != CaptureStatus.SUGGESTED)
			throw new BusinessException("This capture is already " + c.getStatus());

		Map<String, Object> suggested = Json.readMap(c.getAngles());
		Map<String, Double> finalAngles = new LinkedHashMap<>();
		suggested.forEach((k, v) -> finalAngles.put(k, ((Number) v).doubleValue()));
		if (r != null && r.confirmedAngles() != null) {
			Map<String, Double> adjusted = checkAngles(r.confirmedAngles());
			for (String key : adjusted.keySet()) {
				if (!suggested.containsKey(key))
					throw new BusinessException("'" + key + "' was not measured in this capture");
			}
			finalAngles.putAll(adjusted);
		}
		c.setConfirmedAngles(Json.write(finalAngles));
		c.setStatus(CaptureStatus.CONFIRMED);
		c.setConfirmedBy(SecurityUtils.currentUsername());
		c.setConfirmedAt(LocalDateTime.now(clock));
		if (r != null && r.note() != null)
			c.setNote(r.note());

		if (c.getQuestionRef() != null) {
			String key = chooseAngleKey(a, c, finalAngles, r == null ? null : r.angleKey());
			assessmentService.setCameraAnswer(a, c.getQuestionRef(), finalAngles.get(key), c.getId());
		}
		audit.record("POSE_CAPTURE_CONFIRMED", "PoseCapture", c.getId(),
				Map.of("assessmentId", assessmentId.toString()));
		return toResponse(captureRepository.save(c));
	}

	@Transactional
	public CaptureResponse reject(UUID assessmentId, UUID captureId, String reason) {
		assessmentService.requireDraft(assessmentId);
		PoseCapture c = find(assessmentId, captureId);
		if (c.getStatus() != CaptureStatus.SUGGESTED)
			throw new BusinessException("This capture is already " + c.getStatus());
		c.setStatus(CaptureStatus.REJECTED);
		c.setNote(reason);
		audit.record("POSE_CAPTURE_REJECTED", "PoseCapture", c.getId(),
				Map.of("assessmentId", assessmentId.toString()));
		return toResponse(captureRepository.save(c));
	}

	// ---------------------------------------------------- image (optional)
	@Transactional
	public CaptureResponse uploadImage(UUID assessmentId, UUID captureId, byte[] data) {
		Assessment a = assessmentService.requireDraft(assessmentId);
		PoseCapture c = find(assessmentId, captureId);
		if (c.getStatus() == CaptureStatus.REJECTED)
			throw new BusinessException("A rejected capture cannot have an image");
		if (data == null || data.length == 0)
			throw new BusinessException("The image is empty");
		if (data.length > maxImageBytes)
			throw new BusinessException("The image is larger than " + (maxImageBytes / 1024 / 1024) + " MB");
		ImageValidator.Detected type = ImageValidator.detect(data);
		if (type == null)
			throw new BusinessException("Only JPEG, PNG or WebP images are accepted");
		if (!patientClient.hasConsent(a.getPatientId(), "PHOTO_VIDEO")) {
			throw new BusinessException("The patient has not consented to photos or video");
		}
		String ref = (a.getClinicId() == null ? "default" : a.getClinicId().toString()) + "/" + a.getPatientId() + "/"
				+ LocalDateTime.now(clock).getYear() + "/" + UUID.randomUUID() + "." + type.extension();
		imageStorage.save(ref, data);
		String old = c.getImageRef();
		c.setImageRef(ref);
		c.setImageContentType(type.contentType());
		c.setImageSizeBytes((long) data.length);
		captureRepository.save(c);
		if (old != null)
			imageStorage.delete(old);
		audit.record("POSE_IMAGE_STORED", "PoseCapture", c.getId(), Map.of("assessmentId", assessmentId.toString()));
		return toResponse(c);
	}

	@Transactional(readOnly = true)
	public ImageData getImage(UUID assessmentId, UUID captureId) {
		assessmentService.load(assessmentId);
		PoseCapture c = find(assessmentId, captureId);
		if (c.getImageRef() == null)
			throw new ResourceNotFoundException("Image", captureId);
		return new ImageData(imageStorage.load(c.getImageRef()), c.getImageContentType());
	}

	/**
	 * Allowed even after completion (for example when the patient withdraws photo
	 * consent).
	 */
	@Transactional
	public CaptureResponse deleteImage(UUID assessmentId, UUID captureId) {
		assessmentService.load(assessmentId);
		PoseCapture c = find(assessmentId, captureId);
		if (c.getImageRef() == null)
			throw new ResourceNotFoundException("Image", captureId);
		imageStorage.delete(c.getImageRef());
		c.setImageRef(null);
		c.setImageContentType(null);
		c.setImageSizeBytes(null);
		audit.record("POSE_IMAGE_DELETED", "PoseCapture", c.getId(), Map.of("assessmentId", assessmentId.toString()));
		return toResponse(captureRepository.save(c));
	}

	// ------------------------------------------------------ helpers
	private PoseCapture find(UUID assessmentId, UUID captureId) {
		return captureRepository.findByIdAndAssessmentId(captureId, assessmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Capture", captureId));
	}

	static Map<String, Double> checkAngles(Map<String, Double> angles) {
		if (angles == null || angles.isEmpty())
			throw new BusinessException("At least one angle is required");
		if (angles.size() > 40)
			throw new BusinessException("At most 40 angles per capture");
		Map<String, Double> out = new LinkedHashMap<>();
		angles.forEach((k, v) -> {
			if (k == null || !ANGLE_KEY.matcher(k).matches()) {
				throw new BusinessException("Angle names must be lowercase letters, digits or underscore: " + k);
			}
			if (v == null || !Double.isFinite(v) || v < -360 || v > 360) {
				throw new BusinessException("Angle '" + k + "' must be a number between -360 and 360 degrees");
			}
			out.put(k, v);
		});
		return out;
	}

	private void assertAngleQuestion(Assessment a, String ref) {
		String[] parts = ref.trim().split("\\.", 2);
		boolean ok = parts.length == 2 && assessmentService.snapshot(a).templates().stream()
				.filter(t -> t.code().equals(parts[0])).flatMap(t -> t.sections().stream())
				.flatMap(s -> s.questions().stream())
				.anyMatch(q -> q.key().equals(parts[1]) && q.type() == com.hms.assessment.engine.QuestionType.ANGLE);
		if (!ok)
			throw new BusinessException(
					"questionRef must point to an angle question of this assessment (TEMPLATE.question)");
	}

	private String chooseAngleKey(Assessment a, PoseCapture c, Map<String, Double> angles, String requested) {
		if (requested != null && !requested.isBlank()) {
			if (!angles.containsKey(requested))
				throw new BusinessException("'" + requested + "' was not measured in this capture");
			return requested;
		}
		String[] parts = c.getQuestionRef().split("\\.", 2);
		String hint = assessmentService.snapshot(a).templates().stream().filter(t -> t.code().equals(parts[0]))
				.flatMap(t -> t.sections().stream()).flatMap(s -> s.questions().stream())
				.filter(q -> q.key().equals(parts[1]) && q.capture() != null).map(q -> q.capture().angleKey())
				.findFirst().orElse(null);
		if (hint != null && angles.containsKey(hint))
			return hint;
		if (angles.size() == 1)
			return angles.keySet().iterator().next();
		throw new BusinessException("This capture has several angles. Say which one to use (angleKey).");
	}

	private CaptureResponse toResponse(PoseCapture c) {
		return new CaptureResponse(c.getId(), c.getKind(), c.getViewType(), c.getBodyRegion(), c.getLaterality(),
				Json.readMap(c.getAngles()),
				c.getConfirmedAngles() == null ? null : Json.readMap(c.getConfirmedAngles()), c.getQualityScore(),
				c.getToolName(), c.getToolVersion(), c.getQuestionRef(), c.getStatus(), c.getNote(),
				c.getImageRef() != null, c.getConfirmedBy(), c.getConfirmedAt(), c.getCreatedAt());
	}
}