package com.gw.share.vo.work;

import com.gw.share.vo.common.BaseVo;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkUnitVo extends BaseVo {

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 업무명
    private String ttl;

    // 설명
    private String dscr;

    // 카테고리
    private String ctgr;

    // 상태
    private String sts;

    // 사용 여부
    private String useYn;

    // 사용 횟수
    private Integer useCnt;

    // 마지막 사용 일시
    private OffsetDateTime lastUsedAt;

    // 연결된 Git 프로젝트 목록
    private List<WorkGitPrjVo> gitProjects;
}
