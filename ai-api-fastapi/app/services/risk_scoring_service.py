# 위험 신호를 규칙 기반으로 분류하는 MVP 스코어링 서비스다.
from __future__ import annotations

import re

from app.schemas.risk_score import RiskScoreRequest, RiskScoreResponse


class RiskScoringService:
    HIGH_RISK_PATTERNS = {
        r"죽고\s*싶": "SELF_HARM_IMPLICIT",
        r"끝내고\s*싶": "SELF_HARM_IMPLICIT",
        r"사라지고\s*싶": "HOPELESSNESS",
        r"자해": "SELF_HARM_EXPLICIT",
        r"극단적\s*선택": "SELF_HARM_EXPLICIT",
    }

    MEDIUM_RISK_PATTERNS = {
        r"아무도\s*없": "ISOLATION",
        r"혼자(?:다|인 것 같|라고 느껴)": "ISOLATION",
        r"버티기\s*힘들": "DISTRESS_ESCALATION",
        r"너무\s*힘들": "DISTRESS_ESCALATION",
        r"절망": "HOPELESSNESS",
    }

    def score_risk(self, request: RiskScoreRequest) -> RiskScoreResponse:
        text = request.text.strip().lower()
        if not text:
            return RiskScoreResponse(
                riskLevel="LOW",
                riskScore=0.05,
                signals=[],
                recommendedAction="NORMAL_RESPONSE",
            )

        high_signals = self._collect_signals(text, self.HIGH_RISK_PATTERNS)
        if high_signals:
            return RiskScoreResponse(
                riskLevel="HIGH",
                riskScore=0.95,
                signals=high_signals,
                recommendedAction="SAFETY_RESPONSE",
            )

        medium_signals = self._collect_signals(text, self.MEDIUM_RISK_PATTERNS)
        if medium_signals:
            return RiskScoreResponse(
                riskLevel="MEDIUM",
                riskScore=0.65,
                signals=medium_signals,
                recommendedAction="SUPPORTIVE_RESPONSE",
            )

        return RiskScoreResponse(
            riskLevel="LOW",
            riskScore=0.10,
            signals=[],
            recommendedAction="NORMAL_RESPONSE",
        )

    def _collect_signals(self, text: str, patterns: dict[str, str]) -> list[str]:
        signals = [
            signal
            for pattern, signal in patterns.items()
            if re.search(pattern, text)
        ]
        return sorted(set(signals))
