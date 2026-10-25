package com.gw.infra.db.mapper.file;

import com.gw.share.vo.file.FileVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

    void insertFile(FileVo file);

    FileVo selectFileByUuid(@Param("uuid") String uuid);

    List<FileVo> selectFilesByUuids(@Param("uuids") List<String> uuids);

    FileVo selectFileByIdx(@Param("idx") Long idx);

    int deleteFile(@Param("uuid") String uuid);
}
