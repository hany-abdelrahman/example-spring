package com.lab.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.lab.models.View;

@RestResource(path="/view", rel="view")
public interface ViewRepository extends CrudRepository<View, Long> {
    Long countByVideoId(long videoId);
}
