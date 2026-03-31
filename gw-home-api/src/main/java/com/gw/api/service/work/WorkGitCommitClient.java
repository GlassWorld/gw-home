package com.gw.api.service.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.dto.work.WorkGitConnectionTestResponse;
import com.gw.api.dto.work.WorkUnitGitCommitResponse;
import com.gw.api.util.work.WorkGitAccessTokenEncryptor;
import com.gw.share.vo.work.WorkGitPrjVo;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkGitCommitClient {

    private static final Pattern SSH_REPOSITORY_PATTERN = Pattern.compile("^git@([^:]+):(.+?)(?:\\.git)?/?$");
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;
    private final WorkGitAccessTokenEncryptor workGitAccessTokenEncryptor;
    private final HttpClient httpClient;

    public WorkGitCommitClient(
            ObjectMapper objectMapper,
            WorkGitAccessTokenEncryptor workGitAccessTokenEncryptor
    ) {
        this.objectMapper = objectMapper;
        this.workGitAccessTokenEncryptor = workGitAccessTokenEncryptor;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<WorkUnitGitCommitResponse> fetchCommits(List<WorkGitPrjVo> gitProjects, LocalDate reportDate) {
        if (gitProjects == null || gitProjects.isEmpty()) {
            return List.of();
        }

        List<WorkUnitGitCommitResponse> commits = new ArrayList<>();

        for (WorkGitPrjVo gitProject : gitProjects) {
            try {
                commits.addAll(fetchCommits(gitProject, reportDate));
            } catch (Exception exception) {
                log.warn(
                        "Git commit 조회 일부 실패 - gitProjectUuid: {}, provider: {}, repositoryUrl: {}",
                        gitProject.getUuid(),
                        gitProject.getPrvdCd(),
                        gitProject.getRepoUrl(),
                        exception
                );
            }
        }

        return commits.stream()
                .filter(commit -> commit.authoredAt() != null)
                .sorted(Comparator.comparing(WorkUnitGitCommitResponse::authoredAt).reversed())
                .toList();
    }

    public WorkGitConnectionTestResponse testProjectConnection(WorkGitPrjVo gitProject) {
        try {
            if (!"GITLAB".equals(gitProject.getPrvdCd())) {
                return new WorkGitConnectionTestResponse(
                        gitProject.getUuid(),
                        gitProject.getPrvdCd(),
                        gitProject.getPrjNm(),
                        gitProject.getRepoUrl(),
                        false,
                        "GitLab 프로젝트만 연결 테스트를 지원합니다.",
                        OffsetDateTime.now(KOREA_ZONE_ID)
                );
            }

            return testGitLabConnection(gitProject);
        } catch (Exception exception) {
            log.warn(
                    "Git 연결 테스트 실패 - gitProjectUuid: {}, provider: {}, repositoryUrl: {}",
                    gitProject.getUuid(),
                    gitProject.getPrvdCd(),
                    gitProject.getRepoUrl(),
                    exception
            );
            return new WorkGitConnectionTestResponse(
                    gitProject.getUuid(),
                    gitProject.getPrvdCd(),
                    gitProject.getPrjNm(),
                    gitProject.getRepoUrl(),
                    false,
                    exception.getMessage(),
                    OffsetDateTime.now(KOREA_ZONE_ID)
            );
        }
    }

    private List<WorkUnitGitCommitResponse> fetchCommits(WorkGitPrjVo gitProject, LocalDate reportDate)
            throws IOException, InterruptedException {
        if (!"GITLAB".equals(gitProject.getPrvdCd())) {
            return List.of();
        }

        return fetchGitLabCommits(gitProject, reportDate);
    }

    private List<WorkUnitGitCommitResponse> fetchGitLabCommits(WorkGitPrjVo gitProject, LocalDate reportDate)
            throws IOException, InterruptedException {
        RepositoryCoordinates repositoryCoordinates = parseRepositoryCoordinates(gitProject.getRepoUrl());
        String projectPath = encode(repositoryCoordinates.projectPath());
        OffsetDateTime since = reportDate.atStartOfDay(KOREA_ZONE_ID).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime until = reportDate.plusDays(1).atStartOfDay(KOREA_ZONE_ID)
                .toOffsetDateTime()
                .minusSeconds(1)
                .withOffsetSameInstant(ZoneOffset.UTC);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(
                        repositoryCoordinates.scheme()
                                + "://"
                                + repositoryCoordinates.host()
                                + "/api/v4/projects/"
                                + projectPath
                                + "/repository/commits?since="
                                + encode(since.toString())
                                + "&until="
                                + encode(until.toString())
                                + "&author="
                                + encode(gitProject.getAuthNm())
                                + "&per_page=100"
                ))
                .header("Accept", "application/json")
                .GET();

        applyAuthorizationHeader(requestBuilder, gitProject);

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("GitLab API 호출 실패: " + response.statusCode());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        List<WorkUnitGitCommitResponse> commits = new ArrayList<>();

        for (JsonNode commitNode : rootNode) {
            String authorName = commitNode.path("author_name").asText("");
            if (!isSameAuthor(authorName, gitProject.getAuthNm())) {
                continue;
            }

            String authoredAt = commitNode.path("authored_date").asText(commitNode.path("created_at").asText(""));
            String message = commitNode.path("message").asText(commitNode.path("title").asText(""));
            commits.add(new WorkUnitGitCommitResponse(
                    gitProject.getUuid(),
                    gitProject.getPrvdCd(),
                    gitProject.getRepoUrl(),
                    gitProject.getPrjNm(),
                    commitNode.path("id").asText(""),
                    message,
                    authorName,
                    parseOffsetDateTime(authoredAt),
                    commitNode.path("web_url").asText("")
            ));
        }

        return commits;
    }

    private WorkGitConnectionTestResponse testGitLabConnection(WorkGitPrjVo gitProject)
            throws IOException, InterruptedException {
        RepositoryCoordinates repositoryCoordinates = parseRepositoryCoordinates(gitProject.getRepoUrl());
        String projectPath = encode(repositoryCoordinates.projectPath());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(
                        repositoryCoordinates.scheme()
                                + "://"
                                + repositoryCoordinates.host()
                                + "/api/v4/projects/"
                                + projectPath
                ))
                .header("Accept", "application/json")
                .GET();

        applyAuthorizationHeader(requestBuilder, gitProject);

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("GitLab 저장소 접근 실패: " + response.statusCode());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        String fullPath = rootNode.path("path_with_namespace").asText(gitProject.getPrjNm());
        return new WorkGitConnectionTestResponse(
                gitProject.getUuid(),
                gitProject.getPrvdCd(),
                gitProject.getPrjNm(),
                gitProject.getRepoUrl(),
                true,
                "연결 성공: " + fullPath,
                OffsetDateTime.now(KOREA_ZONE_ID)
        );
    }

    private void applyAuthorizationHeader(HttpRequest.Builder requestBuilder, WorkGitPrjVo gitProject) {
        if (gitProject.getAcsToknEnc() == null || gitProject.getAcsToknEnc().isBlank()) {
            return;
        }

        String accessToken = workGitAccessTokenEncryptor.decrypt(gitProject.getAcsToknEnc());

        requestBuilder.header("PRIVATE-TOKEN", accessToken);
    }

    private RepositoryCoordinates parseRepositoryCoordinates(String repositoryUrl) {
        String normalizedRepositoryUrl = repositoryUrl == null ? "" : repositoryUrl.trim();
        Matcher sshMatcher = SSH_REPOSITORY_PATTERN.matcher(normalizedRepositoryUrl);

        if (sshMatcher.matches()) {
            String host = sshMatcher.group(1);
            String projectPath = trimGitSuffix(sshMatcher.group(2));
            return toRepositoryCoordinates("https", host, projectPath);
        }

        URI uri = URI.create(normalizedRepositoryUrl);
        String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
        String host = Objects.requireNonNullElse(uri.getHost(), "");
        String projectPath = trimGitSuffix(uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", ""));
        return toRepositoryCoordinates(scheme, host, projectPath);
    }

    private RepositoryCoordinates toRepositoryCoordinates(String scheme, String host, String projectPath) {
        String[] segments = projectPath.split("/");

        if (segments.length < 2) {
            throw new IllegalArgumentException("저장소 URL에서 owner/repository를 추출할 수 없습니다.");
        }

        return new RepositoryCoordinates(
                scheme,
                host,
                segments[0],
                segments[segments.length - 1],
                projectPath
        );
    }

    private String trimGitSuffix(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.replaceAll("/+$", "");
    }

    private boolean isSameAuthor(String authorName, String expectedAuthorName) {
        return normalize(authorName).equals(normalize(expectedAuthorName));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(value);
    }

    private record RepositoryCoordinates(
            String scheme,
            String host,
            String owner,
            String repository,
            String projectPath
    ) {
    }
}
