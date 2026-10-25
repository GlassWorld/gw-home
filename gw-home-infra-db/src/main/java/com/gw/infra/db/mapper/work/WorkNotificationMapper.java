package com.gw.infra.db.mapper.work;

import com.gw.share.vo.work.*;
import com.gw.share.jvo.work.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkNotificationMapper {
    Long selectOwnerLock(@Param("owner") Long owner);
    void insertDefaultSetting(@Param("owner") Long owner, @Param("email") String email, @Param("actor") String actor);
    WrkNtfStgVo selectSetting(@Param("owner") Long owner);
    int updateSetting(@Param("s") WrkNtfStgVo setting);
    WorkReminderTargetJvo selectTarget(@Param("owner") Long owner, @Param("todoUuid") String todoUuid);
    WorkReminderTargetJvo selectReminderTarget(@Param("owner") Long owner, @Param("reminderId") Long reminderId);
    WrkTodoRmdVo selectReminder(@Param("owner") Long owner, @Param("todoId") Long todoId);
    void insertReminder(@Param("r") WrkTodoRmdVo reminder);
    int updateCancelReminder(@Param("owner") Long owner, @Param("todoId") Long todoId, @Param("actor") String actor);
    int updateReminderVersions(@Param("owner") Long owner, @Param("disable") boolean disable, @Param("actor") String actor);
    int updateInvalidReminders(@Param("owner") Long owner, @Param("actor") String actor);
    int updateChangedSchedules(@Param("owner") Long owner, @Param("actor") String actor);
    int updateInvalidDeliveries(@Param("owner") Long owner);
    int insertReminderDeliveries(@Param("owner") Long owner);
    List<Long> selectDueSettings();
    int insertDigest(@Param("s") WrkNtfStgVo setting, @Param("key") String key, @Param("dueAt") OffsetDateTime dueAt);
    List<WorkTodoListJvo> selectDigestTodos(@Param("req") WorkTodoListSearchVo request);
    List<WrkNtfDlvrVo> selectClaimDeliveries();
    WrkNtfDlvrVo selectDelivery(@Param("id") Long id);
    int updateStartSending(@Param("id") Long id, @Param("token") String token);
    int updateDeliveryResult(@Param("id") Long id, @Param("token") String token,
            @Param("status") String status, @Param("code") String code, @Param("nextTry") OffsetDateTime nextTry);
    int updateClaimCancelled(@Param("id") Long id, @Param("token") String token, @Param("code") String code);
    int updateExpiredClaims();
    int updateUnknownSends();
    int updateExpiredDeliveries();
    boolean selectActiveAccount(@Param("owner") Long owner);
    List<WrkNtfDlvrVo> selectHistory(@Param("owner") Long owner, @Param("size") int size, @Param("offset") long offset);
    long selectHistoryCount(@Param("owner") Long owner);
}
