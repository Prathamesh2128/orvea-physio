package com.hms.measurement.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.measurement.client.PatientClient;
import com.hms.measurement.dto.RecordDtos.AngleReading;
import com.hms.measurement.dto.RecordDtos.CompareEnd;
import com.hms.measurement.dto.RecordDtos.CompareItem;
import com.hms.measurement.dto.RecordDtos.CompareResponse;
import com.hms.measurement.dto.RecordDtos.HistoryPoint;
import com.hms.measurement.dto.RecordDtos.LandmarkReading;
import com.hms.measurement.dto.RecordDtos.PointReading;
import com.hms.measurement.dto.RecordDtos.RecordMeasurementRequest;
import com.hms.measurement.dto.RecordDtos.RecordResponse;
import com.hms.measurement.engine.DifferenceCalculator;
import com.hms.measurement.engine.DueCalculator;
import com.hms.measurement.engine.FaceProtocol;
import com.hms.measurement.engine.ProtocolRules;
import com.hms.measurement.engine.ProtocolSnapshot;
import com.hms.measurement.engine.Results;
import com.hms.measurement.engine.TrendCalculator;
import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.entity.MeasurementRecord;
import com.hms.measurement.entity.MeasurementSettings;
import com.hms.measurement.enums.AngleMethod;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.PlanEventType;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.enums.RecordStatus;
import com.hms.measurement.enums.Side;
import com.hms.measurement.repository.PlanRepository;
import com.hms.measurement.repository.RecordRepository;
import com.hms.measurement.storage.ImageStorage;
import com.hms.measurement.storage.ImageValidator;
import com.hms.measurement.util.Json;

@Service
public class MeasurementService {

	static final int MAX_BACKDATE_DAYS = 60;
	private static final BigDecimal MIN_CIRC = new BigDecimal("3.00");
	private static final BigDecimal MAX_CIRC = new BigDecimal("250.00");
	private static final BigDecimal MIN_FACE = new BigDecimal("1.00");
	private static final BigDecimal MAX_FACE = new BigDecimal("50.00");

	public record PhotoData(byte[] bytes, String contentType) {
	}

	private final PlanRepository plans;
	private final RecordRepository records;
	private final PlanEvents events;
	private final SettingsService settingsService;
	private final TrendService trendService;
	private final PatientClient patientClient;
	private final ImageStorage imageStorage;
	private final AuditPublisher audit;
	private final Clock clock;
	private final boolean requireCameraConsent;
	private final long maxImageBytes;

	public MeasurementService(PlanRepository plans, RecordRepository records, PlanEvents events,
			SettingsService settingsService, TrendService trendService, PatientClient patientClient,
			ImageStorage imageStorage, AuditPublisher audit, Clock clock,
			@Value("${physio.measurement.require-camera-consent:true}") boolean requireCameraConsent,
			@Value("${physio.measurement.max-image-bytes:5242880}") long maxImageBytes) {
		this.plans = plans;
		this.records = records;
		this.events = events;
		this.settingsService = settingsService;
		this.trendService = trendService;
		this.patientClient = patientClient;
		this.imageStorage = imageStorage;
		this.audit = audit;
		this.clock = clock;
		this.requireCameraConsent = requireCameraConsent;
		this.maxImageBytes = maxImageBytes;
	}

	// ================================================= record
	@Transactional
	public RecordResponse record(UUID planId, RecordMeasurementRequest r) {
		MeasurementPlan plan = loadPlan(planId);
		if (plan.getStatus() == PlanStatus.ENDED)
			throw new BusinessException("This plan has ended");
		LocalDate today = LocalDate.now(clock);
		LocalDate on = r.measuredOn() == null ? today : r.measuredOn();
		if (on.isAfter(today))
			throw new BusinessException("The measurement date cannot be in the future");
		if (on.isBefore(today.minusDays(MAX_BACKDATE_DAYS))) {
			throw new BusinessException("Measurements can be entered up to " + MAX_BACKDATE_DAYS + " days late");
		}
		MeasurementSettings s = settingsService.effective(plan.getClinicId());
		Side side = r.affectedSide() != null ? r.affectedSide() : plan.getAffectedSide(); // therapist may pick another
																							// side
		ProtocolSnapshot snapshot = snapshotOf(plan);
		Results results = plan.getKind() == MeasurementKind.LIMB ? limbResults(snapshot, r, side)
				: faceResults(plan, r, side);

		MeasurementRecord rec = new MeasurementRecord();
		rec.setClinicId(plan.getClinicId());
		rec.setPlanId(plan.getId());
		rec.setPatientId(plan.getPatientId());
		rec.setCaseId(plan.getCaseId());
		rec.setKind(plan.getKind());
		rec.setMeasuredOn(on);
		rec.setMeasuredTime(r.measuredTime());
		rec.setProtocolRevision(plan.getProtocolRevision());
		rec.setProtocolSnapshot(Json.write(snapshot));
		rec.setResults(Json.write(results));
		rec.setAffectedSide(side);
		Results.Summary sum = results.summary();
		rec.setMeanDiffCm(sum.meanDiffCm());
		rec.setMaxDiffCm(sum.maxDiffCm());
		rec.setMeanDiffPercent(sum.meanDiffPercent());
		rec.setMaxDiffPercent(sum.maxDiffPercent());
		rec.setFlagged(isFlagged(s, sum));
		rec.setNotes(r.notes());
		rec.setMeasuredBy(SecurityUtils.currentUsername());
		MeasurementRecord saved = records.save(rec);

		// a back-dated entry must not move the schedule backwards
		if (plan.getLastMeasuredOn() == null || !on.isBefore(plan.getLastMeasuredOn())) {
			plan.setLastMeasuredOn(on);
			if (plan.getStatus() == PlanStatus.ACTIVE) {
				plan.setNextDueDate(DueCalculator.afterMeasurement(plan.getDueMode(), plan.getNextDueDate(), on,
						plan.getRepeatDays()));
			}
		}
		plans.save(plan);
		events.log(plan, PlanEventType.MEASURED, on, null, Map.of("recordId", String.valueOf(saved.getId())));
		audit.record("MEASUREMENT_RECORDED", "MeasurementRecord", saved.getId(),
				Map.of("planId", planId.toString(), "kind", plan.getKind().name()));
		return toResponse(saved, plan.getNextDueDate(), s);
	}

	private Results limbResults(ProtocolSnapshot snap, RecordMeasurementRequest r, Side side) {
		if ((r.landmarks() != null && !r.landmarks().isEmpty()) || (r.angles() != null && !r.angles().isEmpty())) {
			throw new BusinessException("Limb measurements contain points only");
		}
		List<PointReading> pts = r.points() == null ? List.of() : new ArrayList<>(r.points());
		if (pts.size() != snap.pointCount()) {
			throw new BusinessException(
					"This protocol has " + snap.pointCount() + " points. Send a reading for each one.");
		}
		pts.sort(Comparator.comparing(p -> p == null || p.point() == null ? Integer.MAX_VALUE : p.point()));
		List<Results.Item> items = new ArrayList<>();
		List<DifferenceCalculator.Diff> diffs = new ArrayList<>();
		for (int i = 0; i < pts.size(); i++) {
			PointReading p = pts.get(i);
			if (p == null || p.point() == null || p.point() != i + 1) {
				throw new BusinessException(
						"Points must be numbered 1 to " + snap.pointCount() + " without gaps or repeats");
			}
			if (p.actualDistanceCm() == null)
				throw new BusinessException("Point " + p.point() + ": the actual distance is required");
			BigDecimal actual = DifferenceCalculator.round2(p.actualDistanceCm());
			if (actual.signum() < 0 || actual.compareTo(new BigDecimal("200")) > 0) {
				throw new BusinessException(
						"Point " + p.point() + ": the actual distance must be between 0 and 200 cm");
			}
			BigDecimal left = circumference(p.leftCm(), "Point " + p.point() + " left", MIN_CIRC, MAX_CIRC);
			BigDecimal right = circumference(p.rightCm(), "Point " + p.point() + " right", MIN_CIRC, MAX_CIRC);
			if (left == null && right == null)
				throw new BusinessException("Point " + p.point() + ": measure at least one side");
			BigDecimal planned = snap.plannedDistances().get(i);
			DifferenceCalculator.Diff d = DifferenceCalculator.compute(side, left, right);
			diffs.add(d);
			items.add(
					new Results.Item(String.valueOf(p.point()), ProtocolRules.pointLabel(snap.startLandmark(), planned),
							p.point(), planned, actual, left, right, d.affectedCm(), d.unaffectedCm(), d.diffCm(),
							d.diffPercent(), d.leftMinusRightCm(), actual.subtract(planned)));
		}
		Results.Summary summary = DifferenceCalculator.summarize(diffs);
		return new Results(items, summary, List.of(), side, side != null && summary.meanDiffPercent() != null);
	}

	private Results faceResults(MeasurementPlan plan, RecordMeasurementRequest r, Side side) {
		if (r.points() != null && !r.points().isEmpty())
			throw new BusinessException("Face measurements contain landmarks and angles only");
		Map<String, LandmarkReading> byKey = new LinkedHashMap<>();
		for (LandmarkReading l : r.landmarks() == null ? List.<LandmarkReading>of() : r.landmarks()) {
			if (l == null || FaceProtocol.landmarkLabel(l.key()) == null)
				throw new BusinessException("Unknown face landmark: " + (l == null ? null : l.key()));
			if (byKey.put(l.key(), l) != null)
				throw new BusinessException("Face landmark sent twice: " + l.key());
		}
		List<Results.Item> items = new ArrayList<>();
		List<DifferenceCalculator.Diff> diffs = new ArrayList<>();
		for (FaceProtocol.Item fixed : FaceProtocol.LANDMARKS) {
			LandmarkReading l = byKey.get(fixed.key());
			if (l == null)
				throw new BusinessException("The face measurement needs: " + fixed.label());
			BigDecimal left = circumference(l.leftCm(), fixed.label() + " left", MIN_FACE, MAX_FACE);
			BigDecimal right = circumference(l.rightCm(), fixed.label() + " right", MIN_FACE, MAX_FACE);
			if (left == null && right == null)
				throw new BusinessException(fixed.label() + ": measure at least one side");
			DifferenceCalculator.Diff d = DifferenceCalculator.compute(side, left, right);
			diffs.add(d);
			items.add(new Results.Item(fixed.key(), fixed.label(), null, null, null, left, right, d.affectedCm(),
					d.unaffectedCm(), d.diffCm(), d.diffPercent(), d.leftMinusRightCm(), null));
		}
		List<Results.Angle> angles = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		boolean camera = false;
		for (AngleReading a : r.angles() == null ? List.<AngleReading>of() : r.angles()) {
			if (a == null || FaceProtocol.angleLabel(a.key()) == null)
				throw new BusinessException("Unknown face angle: " + (a == null ? null : a.key()));
			if (!seen.add(a.key()))
				throw new BusinessException("Face angle sent twice: " + a.key());
			AngleMethod method = a.method() == null ? AngleMethod.MANUAL : a.method();
			BigDecimal degrees = angle(a.degrees(), a.key());
			BigDecimal suggested = null;
			if (method == AngleMethod.CAMERA_CONFIRMED) {
				if (a.suggestedDegrees() == null)
					throw new BusinessException(
							a.key() + ": send the camera's suggested value as well as the final one");
				suggested = angle(a.suggestedDegrees(), a.key());
				camera = true;
			} else if (a.suggestedDegrees() != null) {
				throw new BusinessException(a.key() + ": a manual angle cannot carry a camera suggestion");
			}
			angles.add(new Results.Angle(a.key(), FaceProtocol.angleLabel(a.key()), degrees, method, suggested));
		}
		if (camera && requireCameraConsent && !patientClient.hasConsent(plan.getPatientId(), "CAMERA_POSE_ANALYSIS")) {
			throw new BusinessException("The patient has not consented to camera-based analysis");
		}
		Results.Summary summary = DifferenceCalculator.summarize(diffs);
		return new Results(items, summary, angles, side, side != null && summary.meanDiffPercent() != null);
	}

	private static BigDecimal circumference(BigDecimal v, String what, BigDecimal min, BigDecimal max) {
		if (v == null)
			return null;
		BigDecimal x = DifferenceCalculator.round2(v);
		if (x.compareTo(min) < 0 || x.compareTo(max) > 0) {
			throw new BusinessException(what + " must be between " + min.stripTrailingZeros().toPlainString() + " and "
					+ max.stripTrailingZeros().toPlainString() + " cm");
		}
		return x;
	}

	private static BigDecimal angle(BigDecimal v, String key) {
		BigDecimal x = DifferenceCalculator.round2(v);
		if (x == null || x.compareTo(FaceProtocol.MIN_ANGLE) < 0 || x.compareTo(FaceProtocol.MAX_ANGLE) > 0) {
			throw new BusinessException(key + " must be between -90 and 90 degrees");
		}
		return x;
	}

	/** Flags only when the clinic has set a level. */
	private static boolean isFlagged(MeasurementSettings s, Results.Summary sum) {
		if (s.getFlagThresholdPercent() == null)
			return false;
		BigDecimal m = TrendCalculator.metric(s.getTrendMetric(), sum.meanDiffPercent(), sum.maxDiffPercent());
		return m != null && m.compareTo(s.getFlagThresholdPercent()) >= 0;
	}

	private static ProtocolSnapshot snapshotOf(MeasurementPlan p) {
		boolean limb = p.getKind() == MeasurementKind.LIMB;
		return new ProtocolSnapshot(p.getKind(), p.getLimbType(), p.getStepCm(), p.getStartLandmark(),
				p.getPointCount(), p.getProtocolRevision(),
				limb ? ProtocolRules.plannedDistances(p.getStepCm(), p.getPointCount()) : List.of());
	}

	// ================================================= void
	/**
	 * Wrong entry: it is voided (kept, with the reason) and the measurement is
	 * taken again.
	 */
	@Transactional
	public RecordResponse voidRecord(UUID recordId, String reason) {
		MeasurementRecord rec = loadRecord(recordId);
		if (rec.getStatus() == RecordStatus.VOIDED)
			throw new BusinessException("This measurement is already voided");
		if (reason == null || reason.isBlank())
			throw new BusinessException("A reason is required to void a measurement");
		MeasurementPlan plan = loadPlan(rec.getPlanId());
		rec.setStatus(RecordStatus.VOIDED);
		rec.setVoidReason(reason.trim());
		records.save(rec);
		List<MeasurementRecord> latest = records.findRecent(plan.getId(), RecordStatus.ACTIVE, PageRequest.of(0, 1));
		plan.setLastMeasuredOn(latest.isEmpty() ? null : latest.get(0).getMeasuredOn()); // due date is left as it was
		plans.save(plan);
		events.log(plan, PlanEventType.MEASUREMENT_VOIDED, LocalDate.now(clock), reason.trim(),
				Map.of("recordId", recordId.toString()));
		audit.record("MEASUREMENT_VOIDED", "MeasurementRecord", recordId, Map.of("planId", plan.getId().toString()));
		return toResponse(rec, plan.getNextDueDate(), settingsService.effective(plan.getClinicId()));
	}

	// ================================================= read
	@Transactional
	public RecordResponse get(UUID recordId) {
		MeasurementRecord rec = loadRecord(recordId);
		MeasurementPlan plan = loadPlan(rec.getPlanId());
		return toResponse(rec, plan.getNextDueDate(), settingsService.effective(plan.getClinicId()));
	}

	/** Oldest first, valid readings only: ready for a chart. */
	@Transactional(readOnly = true)
	public List<HistoryPoint> history(UUID planId) {
		loadPlan(planId);
		return records.findByPlanIdAndStatusOrderByMeasuredOnAscCreatedAtAsc(planId, RecordStatus.ACTIVE).stream()
				.map(r -> new HistoryPoint(r.getId(), r.getMeasuredOn(), r.getProtocolRevision(),
						r.getMeanDiffPercent(), r.getMaxDiffPercent(), r.getMeanDiffCm(), r.getMaxDiffCm(),
						r.isFlagged(), Json.read(r.getResults(), Results.class).items()))
				.toList();
	}

	/**
	 * Before vs after. Defaults: baseline = the first reading of the current
	 * protocol revision, target = the latest. Only readings of the same revision
	 * and the same reference side are compared.
	 */
	@Transactional(readOnly = true)
	public CompareResponse compare(UUID planId, UUID baselineId, UUID targetId) {
		loadPlan(planId);
		List<MeasurementRecord> all = records.findByPlanIdAndStatusOrderByMeasuredOnAscCreatedAtAsc(planId,
				RecordStatus.ACTIVE);
		if (all.size() < 2)
			throw new BusinessException("At least two valid measurements are needed to compare");
		MeasurementRecord target = targetId == null ? all.get(all.size() - 1) : pick(all, targetId);
		MeasurementRecord baseline = baselineId == null
				? all.stream().filter(r -> r.getProtocolRevision() == target.getProtocolRevision()).findFirst()
						.orElseThrow()
				: pick(all, baselineId);
		if (baseline.getId().equals(target.getId()))
			throw new BusinessException("Choose two different measurements to compare");
		if (all.indexOf(baseline) > all.indexOf(target))
			throw new BusinessException("The baseline must be earlier than the target");
		if (baseline.getProtocolRevision() != target.getProtocolRevision()) {
			throw new BusinessException(
					"These measurements use different protocols (revision " + baseline.getProtocolRevision() + " and "
							+ target.getProtocolRevision() + ") and cannot be compared point by point");
		}
		if (baseline.getAffectedSide() != target.getAffectedSide()) {
			throw new BusinessException(
					"The two measurements used a different reference side, so their differences cannot be compared");
		}
		Results b = Json.read(baseline.getResults(), Results.class);
		Results t = Json.read(target.getResults(), Results.class);
		Map<String, Results.Item> baseItems = new LinkedHashMap<>();
		b.items().forEach(i -> baseItems.put(i.key(), i));
		List<CompareItem> items = new ArrayList<>();
		for (Results.Item ti : t.items()) {
			Results.Item bi = baseItems.get(ti.key());
			if (bi == null)
				continue;
			items.add(new CompareItem(ti.key(), ti.label(), bi.affectedCm(), ti.affectedCm(),
					minus(ti.affectedCm(), bi.affectedCm()), bi.unaffectedCm(), ti.unaffectedCm(), bi.diffPercent(),
					ti.diffPercent(), minus(ti.diffPercent(), bi.diffPercent())));
		}
		return new CompareResponse(planId,
				new CompareEnd(baseline.getId(), baseline.getMeasuredOn(), baseline.getMeanDiffPercent(),
						baseline.getMaxDiffPercent()),
				new CompareEnd(target.getId(), target.getMeasuredOn(), target.getMeanDiffPercent(),
						target.getMaxDiffPercent()),
				java.time.temporal.ChronoUnit.DAYS.between(baseline.getMeasuredOn(), target.getMeasuredOn()),
				target.getAffectedSide(), items, minus(target.getMeanDiffPercent(), baseline.getMeanDiffPercent()),
				minus(target.getMaxDiffPercent(), baseline.getMaxDiffPercent()),
				minus(target.getMeanDiffCm(), baseline.getMeanDiffCm()));
	}

	private MeasurementRecord pick(List<MeasurementRecord> all, UUID id) {
		return all.stream().filter(r -> r.getId().equals(id)).findFirst()
				.orElseThrow(() -> new BusinessException("Measurement " + id + " is not a valid reading of this plan"));
	}

	private static BigDecimal minus(BigDecimal a, BigDecimal b) {
		return a == null || b == null ? null : a.subtract(b);
	}

	// ================================================= photo (optional)
	@Transactional
	public RecordResponse uploadPhoto(UUID recordId, byte[] data) {
		MeasurementRecord rec = loadRecord(recordId);
		if (rec.getStatus() == RecordStatus.VOIDED)
			throw new BusinessException("A voided measurement cannot have a photo");
		if (data == null || data.length == 0)
			throw new BusinessException("The photo is empty");
		if (data.length > maxImageBytes)
			throw new BusinessException("The photo is larger than " + (maxImageBytes / 1024 / 1024) + " MB");
		ImageValidator.Detected type = ImageValidator.detect(data);
		if (type == null)
			throw new BusinessException("Only JPEG, PNG or WebP images are accepted");
		if (!patientClient.hasConsent(rec.getPatientId(), "PHOTO_VIDEO")) {
			throw new BusinessException("The patient has not consented to photos or video");
		}
		String ref = (rec.getClinicId() == null ? "default" : rec.getClinicId().toString()) + "/" + rec.getPatientId()
				+ "/" + LocalDateTime.now(clock).getYear() + "/" + UUID.randomUUID() + "." + type.extension();
		imageStorage.save(ref, data);
		String old = rec.getPhotoRef();
		rec.setPhotoRef(ref);
		rec.setPhotoContentType(type.contentType());
		rec.setPhotoSizeBytes((long) data.length);
		records.save(rec);
		if (old != null)
			imageStorage.delete(old);
		audit.record("MEASUREMENT_PHOTO_STORED", "MeasurementRecord", recordId, Map.of());
		MeasurementPlan plan = loadPlan(rec.getPlanId());
		return toResponse(rec, plan.getNextDueDate(), settingsService.effective(plan.getClinicId()));
	}

	@Transactional(readOnly = true)
	public PhotoData getPhoto(UUID recordId) {
		MeasurementRecord rec = loadRecord(recordId);
		if (rec.getPhotoRef() == null)
			throw new ResourceNotFoundException("Photo", recordId);
		return new PhotoData(imageStorage.load(rec.getPhotoRef()), rec.getPhotoContentType());
	}

	/**
	 * Allowed at any time, for example when the patient withdraws photo consent.
	 */
	@Transactional
	public RecordResponse deletePhoto(UUID recordId) {
		MeasurementRecord rec = loadRecord(recordId);
		if (rec.getPhotoRef() == null)
			throw new ResourceNotFoundException("Photo", recordId);
		imageStorage.delete(rec.getPhotoRef());
		rec.setPhotoRef(null);
		rec.setPhotoContentType(null);
		rec.setPhotoSizeBytes(null);
		records.save(rec);
		audit.record("MEASUREMENT_PHOTO_DELETED", "MeasurementRecord", recordId, Map.of());
		MeasurementPlan plan = loadPlan(rec.getPlanId());
		return toResponse(rec, plan.getNextDueDate(), settingsService.effective(plan.getClinicId()));
	}

	// ================================================= helpers
	private MeasurementPlan loadPlan(UUID id) {
		MeasurementPlan p = plans.findById(id).orElseThrow(() -> new ResourceNotFoundException("Measurement plan", id));
		ClinicScope.assertSameClinic(p.getClinicId(), "Measurement plan", id);
		return p;
	}

	private MeasurementRecord loadRecord(UUID id) {
		MeasurementRecord r = records.findById(id).orElseThrow(() -> new ResourceNotFoundException("Measurement", id));
		ClinicScope.assertSameClinic(r.getClinicId(), "Measurement", id);
		return r;
	}

	private RecordResponse toResponse(MeasurementRecord r, LocalDate nextDue, MeasurementSettings s) {
		return new RecordResponse(r.getId(), r.getPlanId(), r.getPatientId(), r.getCaseId(), r.getKind(),
				r.getMeasuredOn(), r.getMeasuredTime(), r.getProtocolRevision(),
				Json.read(r.getProtocolSnapshot(), ProtocolSnapshot.class), Json.read(r.getResults(), Results.class),
				r.isFlagged(), trendService.trendFor(r, s), r.getStatus(), r.getVoidReason(), r.getNotes(),
				r.getPhotoRef() != null, r.getMeasuredBy(), nextDue, r.getCreatedAt());
	}
}