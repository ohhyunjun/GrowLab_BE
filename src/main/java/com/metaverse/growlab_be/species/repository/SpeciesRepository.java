package com.metaverse.growlab_be.species.repository;

import com.metaverse.growlab_be.species.domain.Species;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<Species, Long> {
}
