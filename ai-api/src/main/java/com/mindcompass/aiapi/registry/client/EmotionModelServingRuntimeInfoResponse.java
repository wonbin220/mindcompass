// FastAPI emotion model serving runtime 조회 결과를 담는 내부 응답 객체입니다.
package com.mindcompass.aiapi.registry.client;

public record EmotionModelServingRuntimeInfoResponse(
        String modelDirConfigured,
        String modelDirResolved,
        boolean modelDirExists,
        String labelMapPathConfigured,
        String labelMapPathResolved,
        boolean labelMapPathExists,
        String modelName,
        String modelLoadSource,
        int maxLength
) {
}
