package com.gw.api.service.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.convert.work.WeeklyReportConvert;
import com.gw.api.dto.work.WeeklyReportAiDraftResponse;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
    private static final String SYSTEM_PROMPT_PATH = "prompts/weekly-report-system-prompt.txt";
    private static final String EXAMPLE_PROMPT_PATH = "prompts/weekly-report-example.txt";

    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiPrimaryModel;
    private final String openAiFallbackModel;
    private final HttpClient httpClient;
    private final String systemPromptTemplate;
    private final String examplePrompt;

    public WeeklyReportDraftService(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String openAiApiKey,
            @Value("${openai.primary-model}") String openAiPrimaryModel,
            @Value("${openai.fallback-model}") String openAiFallbackModel
    ) {
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiPrimaryModel = openAiPrimaryModel;
        this.openAiFallbackModel = openAiFallbackModel;
        this.httpClient = HttpClient.newHttpClient();
        this.systemPromptTemplate = readPromptTemplate(SYSTEM_PROMPT_PATH);
        this.examplePrompt = readPromptTemplate(EXAMPLE_PROMPT_PATH);
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
            return requestOpenAiDraft(weekStartDate, weekEndDate, dailyReports, additionalPrompt, openAiPrimaryModel);
        } catch (Exception primaryException) {
            log.warn("OpenAI 기본 모델 초안 생성에 실패했습니다. fallback 모델로 재시도합니다. model={}", openAiPrimaryModel, primaryException);

            try {
                return requestOpenAiDraft(weekStartDate, weekEndDate, dailyReports, additionalPrompt, openAiFallbackModel);
            } catch (Exception fallbackException) {
                log.warn(
                        "OpenAI fallback 모델 초안 생성에도 실패해 규칙 기반 초안으로 대체합니다. primaryModel={}, fallbackModel={}",
                        openAiPrimaryModel,
                        openAiFallbackModel,
                        fallbackException
                );
            }

            return buildRuleBasedDraft(weekStartDate, weekEndDate, dailyReports, additionalPrompt);
        }
    }

    private WeeklyReportAiDraftResponse requestOpenAiDraft(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyReportVo> dailyReports,
            String additionalPrompt,
            String modelName
    ) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("temperature", 0.4);
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", systemPromptTemplate
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

        return WeeklyReportConvert.toAiDraftResponse(title, body, WorkPolicy.GENERATION_TYPE_OPENAI, dailyReports.size(), modelName);
    }

    private WeeklyReportAiDraftResponse buildRuleBasedDraft(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyReportVo> dailyReports,
            String additionalPrompt
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("## 주요 작업 요약\n");

        for (DailyReportVo dailyReport : dailyReports) {
            builder.append("- ")
                    .append(dailyReport.getRptDt().format(DATE_FORMATTER))
                    .append(" ")
                    .append(resolveWorkSummary(dailyReport))
                    .append('\n');
        }

        builder.append('\n')
                .append("## 상세 작업\n");

        for (DailyReportVo dailyReport : dailyReports) {
            builder.append("### ")
                    .append(dailyReport.getRptDt().format(DATE_FORMATTER))
                    .append(" ")
                    .append(resolveWorkSummary(dailyReport))
                    .append('\n');

            appendContentLines(builder, dailyReport.getCntn());

            if (dailyReport.getSpclNote() != null && !dailyReport.getSpclNote().isBlank()) {
                builder.append("- 특이사항: ").append(dailyReport.getSpclNote().trim()).append('\n');
            }

            builder.append('\n');
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
        builder.append("- 기간: ").append(weekStartDate).append(" ~ ").append(weekEndDate).append('\n');

        if (additionalPrompt != null && !additionalPrompt.isBlank()) {
            builder.append("- 추가 지시: ").append(additionalPrompt.trim()).append('\n');
        }

        builder.append('\n')
                .append(examplePrompt)
                .append('\n')
                .append('\n');

        builder.append("일일보고 원본:\n");

        for (DailyReportVo dailyReport : dailyReports) {
            builder.append("### 날짜: ").append(dailyReport.getRptDt()).append('\n')
                    .append("- 업무 분류: ").append(resolveWorkSummary(dailyReport)).append('\n')
                    .append("- 오늘 수행 내용:\n");
            appendContentBlock(builder, dailyReport.getCntn());
            builder.append('\n');
        }

        return builder.toString();
    }

    private void appendContentBlock(StringBuilder builder, String content) {
        if (content == null || content.isBlank()) {
            builder.append("  - 작성된 내용 없음\n");
            return;
        }

        for (String line : content.strip().split("\\R")) {
            if (line.isBlank()) {
                continue;
            }

            builder.append("  ").append(line.strip()).append('\n');
        }
    }

    private void appendContentLines(StringBuilder builder, String content) {
        if (content == null || content.isBlank()) {
            builder.append("- 작성된 내용 없음\n");
            return;
        }

        for (String line : content.strip().split("\\R")) {
            if (line.isBlank()) {
                continue;
            }

            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("-")) {
                builder.append(trimmedLine).append('\n');
                continue;
            }

            builder.append("- ").append(trimmedLine).append('\n');
        }
    }

    private String readPromptTemplate(String path) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("프롬프트 리소스를 찾을 수 없습니다. path=" + path);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("프롬프트 리소스를 읽지 못했습니다. path=" + path, exception);
        }
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
