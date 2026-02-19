package com.uniqdata.backend.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByProjectId(Long projectId);

    List<Participant> findByProjectIdAndActive(Long projectId, boolean active);

    Optional<Participant> findByProjectIdAndParticipantAddress(Long projectId, String participantAddress);

    long countByProjectId(Long projectId);

    long countByProjectIdAndActive(Long projectId, boolean active);

    long countByActive(boolean active);
}
