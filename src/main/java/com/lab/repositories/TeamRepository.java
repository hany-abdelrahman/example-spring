package com.lab.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.lab.models.Team;

@RestResource(path="/teams", rel="team")
public interface TeamRepository extends CrudRepository<Team, Long> {

}
