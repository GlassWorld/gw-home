package com.gw.share.jvo.work;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter @Setter @NoArgsConstructor @SuperBuilder
public class WorkCompletionBucketJvo {
    private LocalDate bucketStart;
    private long completedCount;
}
