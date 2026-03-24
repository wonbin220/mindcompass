// 상담 답변 생성 내부 엔드포인트를 제공하는 컨트롤러입니다.
package com.mindcompass.aiapi.controller;

import com.mindcompass.aiapi.dto.GenerateReplyRequest;
import com.mindcompass.aiapi.dto.GenerateReplyResponse;
import com.mindcompass.aiapi.service.ReplyGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class ChatAiController {

    private final ReplyGenerationService replyGenerationService;

    public ChatAiController(ReplyGenerationService replyGenerationService) {
        this.replyGenerationService = replyGenerationService;
    }

    @PostMapping("/generate-reply")
    public GenerateReplyResponse generateReply(@Valid @RequestBody GenerateReplyRequest request) {
        return replyGenerationService.generate(request);
    }
}
