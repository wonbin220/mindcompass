// 감정분류 모델 registry 상태 값을 정의하는 enum이다.
package com.mindcompass.aiapi.registry.domain;

public enum EmotionModelRegistryStatus {
    TRAINED,
    APPROVED,
    REJECTED,
    SHADOW,
    ACTIVE,
    ARCHIVED
}
