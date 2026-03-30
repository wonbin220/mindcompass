// 감정 모델 registry 컨트롤러의 쿼리 파라미터 바인딩을 검증하는 테스트다.
package com.mindcompass.aiapi.registry.controller;

import com.mindcompass.aiapi.registry.service.EmotionModelRegistryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EmotionModelRegistryAdminControllerTest {

    @Mock
    private EmotionModelRegistryService emotionModelRegistryService;

    @InjectMocks
    private EmotionModelRegistryAdminController controller;

    @Test
    void getAllForwardsAllOptionalFilters() {
        controller.getAll("SHADOW", false, true, "manual_seed_v2", true);

        then(emotionModelRegistryService).should().getAll("SHADOW", false, true, "manual_seed_v2", true);
    }

    @Test
    void getAllAllowsMissingFilters() {
        controller.getAll(null, null, null, null, null);

        then(emotionModelRegistryService).should().getAll(null, null, null, null, null);
        then(emotionModelRegistryService).should(never()).getAll("SHADOW", false, true, "manual_seed_v2", true);
    }

    @Test
    void getAllForwardsCurrentShadowOnlyFilterSeparately() {
        controller.getAll(null, null, true, null, false);

        then(emotionModelRegistryService).should().getAll(null, null, true, null, false);
    }

    @Test
    void getSummaryDelegatesToService() {
        controller.getSummary();

        then(emotionModelRegistryService).should().getSummary();
    }

    @Test
    void getAvailableTransitionsDelegatesToService() {
        controller.getAvailableTransitions(2L);

        then(emotionModelRegistryService).should().getAvailableTransitions(2L);
    }

    @Test
    void getActiveRuntimeAlignmentDelegatesToService() {
        controller.getActiveRuntimeAlignment();

        then(emotionModelRegistryService).should().getActiveRuntimeAlignment();
    }

    @Test
    void getArtifactHealthDelegatesToService() {
        controller.getArtifactHealth(1L);

        then(emotionModelRegistryService).should().getArtifactHealth(1L);
    }

    @Test
    void getArtifactJsonCheckDelegatesToService() {
        controller.getArtifactJsonCheck(1L);

        then(emotionModelRegistryService).should().getArtifactJsonCheck(1L);
    }

    @Test
    void getPromotionChecklistDelegatesToService() {
        controller.getPromotionChecklist(2L);

        then(emotionModelRegistryService).should().getPromotionChecklist(2L);
    }
}
