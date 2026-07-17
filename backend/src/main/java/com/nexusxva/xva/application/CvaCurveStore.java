package com.nexusxva.xva.application;

import com.nexusxva.xva.domain.CreditCurve;
import com.nexusxva.xva.domain.DiscountCurve;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvaCurveStore {
    CreditCurve createCreditCurve(SaveCreditCurveCommand command);
    CreditCurve updateCreditCurve(UUID curveId, SaveCreditCurveCommand command);
    List<CreditCurve> listCreditCurves(UUID counterpartyId, boolean includeInactive);
    Optional<CreditCurve> findCreditCurve(UUID curveId);

    CreditCurve approveCreditCurve(UUID curveId, UUID approvedByUserId);

    CreditCurve rejectCreditCurve(UUID curveId, String reason);

    DiscountCurve createDiscountCurve(SaveDiscountCurveCommand command);
    DiscountCurve updateDiscountCurve(UUID curveId, SaveDiscountCurveCommand command);
    List<DiscountCurve> listDiscountCurves(String currency, boolean includeInactive);
    Optional<DiscountCurve> findDiscountCurve(UUID curveId);

    DiscountCurve approveDiscountCurve(UUID curveId, UUID approvedByUserId);

    DiscountCurve rejectDiscountCurve(UUID curveId, String reason);
}
