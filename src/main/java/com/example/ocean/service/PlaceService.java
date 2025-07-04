package com.example.ocean.service;

import com.example.ocean.domain.Place;
import com.example.ocean.dto.request.InsertPlace;
import com.example.ocean.mapper.PlaceMapper;
import com.example.ocean.mapper.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@RequiredArgsConstructor
@Service
public class PlaceService {

    private final PlaceMapper placeMapper;

    public int insertPlace(InsertPlace p){

        String placeCd = "p_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Place insertPlace = new Place(placeCd, p.getEventCd(), p.getPlaceNm(), p.getPlaceId(), p.getAddress(), p.getLat(), p.getLng(),null, null);

        return placeMapper.insertPlace(insertPlace);
    }

    public List<Place> findByWorkspaceCd(String workspaceCd) {
        return placeMapper.findByWorkspaceCd(workspaceCd);
    }

}