package com.gw.api.service.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.convert.work.WeeklyReportConvert;
import com.gw.api.dto.work.WeeklyReportAiDraftResponse;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WeeklyReportDraftService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");

    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiModel;
    private final HttpClient httpClient;

    public WeeklyReportDraftService(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4.1-mini}") String openAiModel
    ) {
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.httpClient = HttpClient.newHttpClient();
    }

    // 주차 범위와 일일보고를 바탕으로 주간보고 초안을 생성한다.
    public WeeklyReportAiDraftResponse generateDraft(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyReportVo> dailyReports,
            String additionalPrompt
    ) {
        if (dailyReports.isEmpty()) {
            return buildRuleBasedDraft(weekStartDate, weekEndDate, dailyReports, "해당 주차의 일일보고가 없습니다.");
        }

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return buildRuleBasedDraft(weekStartDate, weekEndDate, dailyReports, additionalPrompt);
        }

        try {
            return requestOpenAiDraft(weekStartDate, weekEndDate, dailyReports, additionalPrompt);
        } catch (Exception exception) {
            log.warn("OpenAI 초안 생성에 실패해 규칙 기반 초안으로 대체합니다.", exception);
            return buildRuleBasedDraft(weekStartDate, weekEndDate, dailyReports, additionalPrompt);
        }
    }

    private WeeklyReportAiDraftResponse requestOpenAiDraft(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyReportVo> dailyReports,
            String additionalPrompt
    ) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", openAiModel);
        requestBody.put("temperature", 0.4);
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "당신은 주간보고 초안을 작성하는 비서입니다. 반드시 JSON 문자열만 반환하세요. keys: title, content"
                ),
                Map.of(
                        "role", "user",
                        "content", buildPrompt(weekStartDate, weekEndDate, dailyReports, additionalPrompt)
                )
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("OpenAI API 호출 실패: " + response.statusCode() + " / " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        String content = rootNode.path("choices").path(0).path("message").path("content").asText();

        if (content == null || content.isBlank()) {
            throw new IOException("OpenAI 응답 본문이 비어 있습니다.");
        }

        JsonNode jsonContent = objectMapper.readTree(content);
        String title = jsonContent.path("title").asText(buildTitle(weekStartDate, weekEndDate));
        String body = jsonContent.path("content").asText();

        if (body == null || body.isBlank()) {
            throw new IOException("OpenAI 초안 내용이 비어 있습니다.");
        }

        return WeeklyReportConvert.toAiDraftResponse(title, body, WorkPolicy.GENERATION_TYPE_OPENAI, dailyReports.size(), openAiModel);
    }

    private WeeklyReportAiDraftResponse buildRuleBasedDraft(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyReportVo> dailyReports,
            String additionalPrompt
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("## 이번 주 주요 업무\n");

        for (DailyReportVo dailyReport : dailyReports) {
            builder.append("- ")
                    .append(dailyReport.getRptDt().format(DATE_FORMATTER))
                    .append(" ")
                    .append(resolveWorkSummary(dailyReport))
                    .append('\n');

            if (dailyReport.getSpclNote() != null && !dailyReport.getSpclNote().isBlank()) {
                builder.append("  - 특이사항: ").append(dailyReport.getSpclNote()).append('\n');
            }
        }

        builder.append('\n')
                .append("## 다음 주 계획\n")
                .append("- 다음 주 우선순위 업무를 정리해주세요.\n")
                .append("- 공유가 필요한 이슈와 지원 요청이 있으면 보강해주세요.\n");

        if (additionalPrompt != null && !additionalPrompt.isBlank()) {
            builder.append('\n')
                    .append("## 추가 메모\n")
                    .append(additionalPrompt.trim())
                    .append('\n');
        }

        return WeeklyReportConvert.toAiDraftResponse(
                buildTitle(weekStartDate, weekEndDate),
                builder.toString().trim(),
                WorkPolicy.GENERATION_TYPE_RULE_BASED,
                dailyReports.size(),
                "local-rule-based"
        );
    }

    private String buildPrompt(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyReportVo> dailyReports,
            String additionalPrompt
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("주간보고 초안을 작성하세요.\n")
                .append("- 기간: ").append(weekStartDate).append(" ~ ").append(weekEndDate).append('\n')
                .append("- 출력 형식: JSON 문자열, markdown fence 금지\n")
                .append("- title: 주간보고 제목\n")
                .append("- content: markdown 형식의 주간보고 본문\n")
                .append("- 본문은 이번 주 주요 업무, 이슈/특이사항, 다음 주 계획 순서로 작성\n");

        if (additionalPrompt != null && !additionalPrompt.isBlank()) {
            builder.append("- 추가 지시: ").append(additionalPrompt.trim()).append('\n');
        }

        builder.append("일일보고 원본:\n");

        for (DailyReportVo dailyReport : dailyReports) {
            builder.append("* 날짜: ").append(dailyReport.getRptDt())
                    .append(", 업무내용: ").append(resolveWorkSummary(dailyReport));

            if (dailyReport.getSpclNote() != null && !dailyReport.getSpclNote().isBlank()) {
                builder.append(", 특이사항: ").append(dailyReport.getSpclNote());
            }

            builder.append('\n');
        }

        return builder.toString();
    }

    private String buildTitle(LocalDate weekStartDate, LocalDate weekEndDate) {
        return weekStartDate.format(DATE_FORMATTER) + " - " + weekEndDate.format(DATE_FORMATTER) + " 주간보고";
    }

    private String resolveWorkSummary(DailyReportVo dailyReport) {
        List<WorkUnitVo> workUnits = dailyReport.getWorkUnits();

        if (workUnits == null || workUnits.isEmpty()) {
            return "선택된 업무가 없습니다.";
        }

        return workUnits.stream()
                .map(WorkUnitVo::getTtl)
                .filter(title -> title != null && !title.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("선택된 업무가 없습니다.");
    }
}
