package com.example.ocean.repository;

import com.example.ocean.domain.Place;
import com.example.ocean.dto.response.PlaceInfoResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlaceRepository {

    int insertPlace(Place place);

    List<Place> findAll();

    List<Place> findByWorkspaceCd(@Param("workspaceCd") String workspaceCd);

    List<PlaceInfoResponse> findPlaceInfoByWorkspaceCd(@Param("workspaceCd") String workspaceCd);

    // 이벤트 코드로 장소 정보 조회
    Place findPlaceByEventCd(@Param("eventCd") String eventCd);

    // 장소 정보 업데이트
    int updatePlace(Place place);

    // 이벤트 코드로 장소 정보가 존재하는지 확인 (0 또는 1 반환)
    int checkPlaceExistsByEventCd(@Param("eventCd") String eventCd);

    // 이벤트 코드로 장소 정보 삭제
    int deletePlaceByEventCd(@Param("eventCd") String eventCd);

}