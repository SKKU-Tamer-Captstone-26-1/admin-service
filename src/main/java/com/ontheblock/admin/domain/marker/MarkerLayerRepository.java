package com.ontheblock.admin.domain.marker;

import com.ontheblock.admin.domain.marker.entity.MarkerLayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarkerLayerRepository extends JpaRepository<MarkerLayerEntity, String> {

    List<MarkerLayerEntity> findAllByOrderByDisplayOrderAsc();
}
