package com.gw.share.vo.work;

import java.time.LocalDate;
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
public class DailyReportListSearchVo {

    private Long mbrAcctIdx;

    private String mbrAcctUuid;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String kwd;

    private Integer page;

    private Integer size;

    private Integer offset;
}
